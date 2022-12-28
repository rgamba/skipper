package com.maestroworkflow.store;

import com.google.inject.Singleton;
import com.maestroworkflow.models.OperationRequest;
import com.maestroworkflow.models.OperationResponse;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            // Operation requests don't need to be unique as long as the timer elements preserve the uniqueness
//            if (this.requests.stream().anyMatch(req ->
//                    req.getWorkflowInstanceId().equals(operationRequest.getWorkflowInstanceId())
//                            && req.getOperationType().equals(operationRequest.getOperationType())
//                            && req.getIteration() == operationRequest.getIteration()
//                            && req.getFailedAttempts() == operationRequest.getFailedAttempts())) {
//                return false;
//            }
            this.requests.add(operationRequest);
            return true;
        } finally {
            reqsLock.unlock();
        }
    }

    @Override
    public boolean createOperationResponse(@NonNull OperationResponse operationResponse) {
        respsLock.lock();
        try {
            if (this.responses.stream().anyMatch(res ->
                    res.getWorkflowInstanceId().equals(operationResponse.getWorkflowInstanceId())
                            && res.getOperationType().equals(operationResponse.getOperationType())
                            && res.getIteration() == operationResponse.getIteration() && !res.isTransient())) {
                return false;
            }
            this.responses.add(operationResponse);
            return true;
        } finally {
            respsLock.unlock();
        }
    }

    @Override
    public List<OperationResponse> getOperationResponses(@NonNull String workflowInstanceId, boolean includeTransientResponses) {
        respsLock.lock();
        try {
            Stream<OperationResponse> stream = this.responses.stream()
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
            Optional<OperationRequest> result = this.requests.stream()
                    .filter(req -> req.getOperationRequestId().equals(operationRequestId)).findFirst();
            if (!result.isPresent()) {
                throw new IllegalArgumentException("invalid operation request id provided");
            }
            return result.get();
        } finally {
            reqsLock.unlock();
        }
    }

    @Override
    public void convertAllErrorResponsesToTransient(String workflowInstanceId) {
        respsLock.lock();
        try {
            this.responses.replaceAll(resp -> {
                if (resp.getWorkflowInstanceId().equals(workflowInstanceId) && !resp.isSuccess() && !resp.isTransient()) {
                    return resp.toBuilder().isTransient(true).build();
                }
                return resp;
            });
        } finally {
            respsLock.unlock();
        }
    }
}
