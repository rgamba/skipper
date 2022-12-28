package maestro;

import com.google.common.base.Stopwatch;
import com.google.inject.Injector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.val;
import maestro.api.*;
import maestro.common.Anything;
import maestro.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationExecutor {
  private static Logger logger = LoggerFactory.getLogger(OperationExecutor.class);

  public static RetryStrategy DEFAULT_RETRY_STRATEGY =
      FixedRetryStrategy.builder().retryDelay(Duration.ofSeconds(5)).maxRetries(5).build();
  public static OperationConfig DEFAULT_OPERATION_CONFIG =
      OperationConfig.builder().retryStrategy(DEFAULT_RETRY_STRATEGY).build();

  public OperationExecutionResponse execute(
      @NonNull OperationRequest request, @NonNull Injector injector) {
    val responseBuilder = OperationExecutionResponse.builder();
    if (request.getOperationType().isWaitTimeout()) {
      return responseBuilder
          .status(OperationExecutionResponse.Status.NON_RETRIABLE_ERROR)
          .error(Anything.of(new WaitTimeout()))
          .build();
    }
    val operation = injector.getInstance(request.getOperationType().getClazz());
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
      // TODO add more logic to determine if error is retriable or not
      responseBuilder.status(OperationExecutionResponse.Status.RETRIABLE_ERROR);
    } finally {
      timer.stop();
      responseBuilder.executionDuration(timer.elapsed());
    }
    val response = responseBuilder.build();
    logger.info("operation execution response = {}", response);
    return response;
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
      if (argValue.getClazz() != params[i].getType()) {
        throw new IllegalStateException(
            String.format(
                "operation request provided the argument %s with type %s when the expected type is %s",
                params[i].getName(), argValue.getClazz(), params[i].getType()));
      }
      response[i] = argValue.getClazz().cast(argValue.getValue());
    }
    return response;
  }
}
