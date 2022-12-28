package skipper;

import com.google.common.base.Stopwatch;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skipper.api.*;
import skipper.common.Anything;
import skipper.models.*;
import skipper.runtime.StopWorkflowExecution;

public class OperationExecutor {
  private static Logger logger = LoggerFactory.getLogger(OperationExecutor.class);

  public static RetryStrategy DEFAULT_RETRY_STRATEGY =
      FixedRetryStrategy.builder().retryDelay(Duration.ofSeconds(5)).maxRetries(5).build();
  public static OperationConfig DEFAULT_OPERATION_CONFIG =
      OperationConfig.builder().retryStrategy(DEFAULT_RETRY_STRATEGY).build();

  public OperationExecutionResponse execute(
      @NonNull OperationRequest request, @NonNull DependencyRegistry registry) {
    val responseBuilder = OperationExecutionResponse.builder();
    if (request.getOperationType().isWaitTimeout()) {
      return responseBuilder
          .status(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR)
          .error(Anything.of(new WaitTimeout()))
          .build();
    }
    val operation = registry.getOperation(request.getOperationType().getClazz());
    Method method = getMethod(request.getOperationType());
    Stopwatch timer = Stopwatch.createStarted();
    try {
      logger.info("executing operation request. request={}", request);
      val result = method.invoke(operation, getMethodParams(method, request.getArguments()));
      if (result != null) {
        responseBuilder.result(new Anything(result.getClass(), result));
      }
      responseBuilder.status(OperationExecutionResponse.Status.COMPLETED);
    } catch (IllegalAccessException e) {
      responseBuilder.error(new Anything(OperationError.class, new OperationError(e)));
      responseBuilder.status(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR);
    } catch (InvocationTargetException e) {
      if (e.getCause() != null && e.getCause() instanceof StopWorkflowExecution) {
        throw (StopWorkflowExecution) e.getCause();
      }
      // TODO error is not propagated correctly here!
      Class<? extends Throwable> clazz =
          e.getCause() != null ? e.getCause().getClass() : e.getClass();
      Throwable error = e.getCause() != null ? e.getCause() : e;
      responseBuilder.error(new Anything(clazz, error));
      val status =
          isRetriableError(error, method)
              ? OperationExecutionResponse.Status.RETRIABLE_ERROR
              : OperationExecutionResponse.Status.NON_RETRIABLE_ERROR;
      responseBuilder.status(status);
    } catch (IllegalArgumentException e) {
      // This might happen if we tried to inject the params to the operation and failed due to bad
      // input params
      // for instance when calling getMethodParams
      responseBuilder.status(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR);
      responseBuilder.error(Anything.of(e));
    } finally {
      timer.stop();
      responseBuilder.executionDuration(timer.elapsed());
    }
    val response = responseBuilder.build();
    logger.info("operation execution response = {}", response);
    return response;
  }

  private boolean isRetriableError(Throwable error, Method method) {
    List<Class<? extends Throwable>> commonNonRetriableErrors = new ArrayList<>();
    commonNonRetriableErrors.add(NullPointerException.class);
    commonNonRetriableErrors.add(IllegalArgumentException.class);
    if (commonNonRetriableErrors.stream().anyMatch(common -> common.isInstance(error))) {
      return false;
    }
    // If not a common non-retryable, check if it was a declared exception, in which case
    // we will throw a non retryable error.
    return Arrays.stream(method.getExceptionTypes())
        .noneMatch(declaredEx -> declaredEx.isInstance(error));
  }

  private Method getMethod(OperationType operationType) {
    return Stream.of(operationType.getClazz().getDeclaredMethods())
        .filter(m -> m.getName().equals(operationType.getMethod()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "the operation class %s does not have a method %s",
                        operationType.getClazz(), operationType.getMethod())));
  }

  public Object[] getMethodParams(@NonNull Method method, @NonNull List<Anything> methodArgs) {
    if (methodArgs.size() != method.getParameters().length) {
      throw new IllegalArgumentException(
          String.format(
              "expected %d method arguments but got %d",
              method.getParameters().length, methodArgs.size()));
    }
    val params = method.getParameters();
    val response = new Object[methodArgs.size()];
    for (int i = 0; i < methodArgs.size(); i++) {
      val argValue = methodArgs.get(i);
      if (argValue == null) {
        response[i] = null;
      } else {
        val paramType = boxPrimitive(params[i].getType());
        if (!paramType.isAssignableFrom(argValue.getClazz())) {
          throw new IllegalArgumentException(
              String.format(
                  "operation request provided the argument %s with type %s when the expected type is %s",
                  params[i].getName(), argValue.getClazz(), params[i].getType()));
        }
        response[i] = argValue.getClazz().cast(argValue.getValue());
      }
    }
    return response;
  }

  public Class<?> boxPrimitive(Class<?> clazz) {
    if (Anything.PRIMITIVES_TO_BOXED.containsKey(clazz.getName())) {
      return Anything.PRIMITIVES_TO_BOXED.get(clazz.getName());
    }
    return clazz;
  }
}
