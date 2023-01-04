package io.github.rgamba.skipper.store.inmemory;

import com.google.inject.Singleton;
import io.github.rgamba.skipper.models.OperationRequest;
import io.github.rgamba.skipper.models.OperationResponse;
import io.github.rgamba.skipper.store.OperationStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
public class InMemoryOperationStore implements OperationStore {

  private final List<OperationRequest> requests;
  private final List<OperationResponse> responses;
  private final ReentrantLock reqsLock = new ReentrantLock();
  private final ReentrantLock respsLock = new ReentrantLock();

  public InMemoryOperationStore() {
    this.requests = new ArrayList<>();
    this.responses = new ArrayList<>();
  }

  @Override
  public boolean createOperationRequest(@NonNull OperationRequest operationRequest) {
    reqsLock.lock();
    try {
      if (this.requests.stream()
          .anyMatch(
              req ->
                  req.getOperationRequestId().equals(operationRequest.getOperationRequestId()))) {
        return false;
      }
      this.requests.add(operationRequest);
      return true;
    } finally {
      reqsLock.unlock();
    }
  }

  @Override
  public void incrementOperationRequestFailedAttempts(
      @NonNull String operationRequestId, int currentRetries) {
    throw new NotImplementedException();
  }

  @Override
  public boolean createOperationResponse(@NonNull OperationResponse operationResponse) {
    respsLock.lock();
    try {
      if (this.responses.stream()
          .anyMatch(
              res ->
                  res.getWorkflowInstanceId().equals(operationResponse.getWorkflowInstanceId())
                      && res.getOperationType().equals(operationResponse.getOperationType())
                      && res.getIteration() == operationResponse.getIteration()
                      && !res.isTransient())) {
        return false;
      }
      this.responses.add(operationResponse);
      return true;
    } finally {
      respsLock.unlock();
    }
  }

  @Override
  public List<OperationResponse> getOperationResponses(
      @NonNull String workflowInstanceId, boolean includeTransientResponses) {
    respsLock.lock();
    try {
      Stream<OperationResponse> stream =
          this.responses.stream()
              .filter(req -> req.getWorkflowInstanceId().equals(workflowInstanceId));
      if (!includeTransientResponses) {
        stream = stream.filter(r -> !r.isTransient());
      }
      return stream.collect(Collectors.toList());
    } finally {
      respsLock.unlock();
    }
  }

  @Override
  public OperationRequest getOperationRequest(@NonNull String operationRequestId) {
    reqsLock.lock();
    try {
      Optional<OperationRequest> result =
          this.requests.stream()
              .filter(req -> req.getOperationRequestId().equals(operationRequestId))
              .findFirst();
      if (!result.isPresent()) {
        throw new IllegalArgumentException("invalid operation request id provided");
      }
      return result.get();
    } finally {
      reqsLock.unlock();
    }
  }

  @Override
  public List<OperationRequest> getOperationRequests(@NonNull String workflowInstanceId) {
    reqsLock.lock();
    try {
      return this.requests.stream()
          .filter(r -> r.getWorkflowInstanceId().equals(workflowInstanceId))
          .collect(Collectors.toList());
    } finally {
      reqsLock.unlock();
    }
  }

  @Override
  public void convertAllErrorResponsesToTransient(String workflowInstanceId) {
    respsLock.lock();
    try {
      this.responses.replaceAll(
          resp -> {
            if (resp.getWorkflowInstanceId().equals(workflowInstanceId)
                && !resp.isSuccess()
                && !resp.isTransient()) {
              return resp.toBuilder().isTransient(true).build();
            }
            return resp;
          });
    } finally {
      respsLock.unlock();
    }
  }

  @Override
  public void createOperationRequestAndResponse(
      @NonNull OperationRequest operationRequest, @NonNull OperationResponse operationResponse) {
    throw new UnsupportedOperationException();
  }
}
