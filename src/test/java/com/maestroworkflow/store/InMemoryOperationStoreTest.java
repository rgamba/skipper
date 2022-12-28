package com.maestroworkflow.store;

import static org.junit.Assert.*;

import com.maestroworkflow.models.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.Test;

public class InMemoryOperationStoreTest {
  @Test
  public void testOperationRequests() {
    final String wfId1 = "wftest123";
    final String wfId2 = "wftest1234";
    final OperationType operationType = new OperationType(String.class, "testmethod");
    InMemoryOperationStore store = new InMemoryOperationStore();
    OperationRequest req1 =
        OperationRequest.builder()
            .operationRequestId("req1")
            .workflowInstanceId(wfId1)
            .operationType(operationType)
            .creationTime(Instant.now())
            .arguments(new ArrayList<>())
            .retryStrategy(
                FixedRetryStrategy.builder().retryDelay(Duration.ZERO).maxRetries(1).build())
            .timeout(Duration.ofSeconds(10))
            .build();
    OperationRequest req2 =
        req1.toBuilder().workflowInstanceId(wfId2).operationRequestId("req2").build();

    assertTrue(store.createOperationRequest(req1));
    assertFalse(store.createOperationRequest(req1)); // Trying to recreate the same operation should fail
    assertEquals(store.getOperationRequest(req1.getOperationRequestId()), req1);
  }

  @Test
  public void testOperationResponses() {
    final String wfId1 = "wftest123";
    final String wfId2 = "wftest1234";
    final OperationType operationType = new OperationType(String.class, "testmethod");
    InMemoryOperationStore store = new InMemoryOperationStore();
    OperationResponse res1 =
        OperationResponse.builder()
            .id("res1")
            .workflowInstanceId(wfId1)
            .operationType(operationType)
            .creationTime(Instant.now())
            .isSuccess(false)
            .isTransient(true)
            .error(Anything.of("test"))
            .operationRequestId("op1")
            .build();
    OperationResponse res2 =
        res1.toBuilder().workflowInstanceId(wfId2).operationRequestId("res2").build();
    OperationResponse res1Success =
        res1.toBuilder().isSuccess(true).isTransient(false).id("res3").build();

    assertTrue(store.createOperationResponse(res1));
    assertTrue(store.createOperationResponse(res2));
    assertTrue(store.createOperationResponse(res1Success));
    assertFalse(store.createOperationResponse(res1));

    assertEquals(2, store.getOperationResponses(wfId1, true).size());
    assertEquals(1, store.getOperationResponses(wfId1, false).size());
    assertEquals(1, store.getOperationResponses(wfId2, true).size());
  }
}
