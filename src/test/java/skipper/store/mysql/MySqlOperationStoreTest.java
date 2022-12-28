package skipper.store.mysql;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import skipper.common.Anything;
import skipper.models.*;
import skipper.store.SqlTestHelper;
import skipper.store.StorageError;

public class MySqlOperationStoreTest extends SqlTestHelper {
  private MySqlOperationStore store;

  @Before
  public void setUp() {
    super.setUp();
    store = new MySqlOperationStore(this.trxMgr);
  }

  @Test
  public void testCreateAndGetOperationRequest() {
    final String wfId1 = "wftest123";
    final String wfId2 = "wftest1234";
    final OperationType operationType = new OperationType(String.class, "testmethod");
    OperationRequest req1 =
        OperationRequest.builder()
            .operationRequestId("req1")
            .workflowInstanceId(wfId1)
            .operationType(operationType)
            .creationTime(Instant.now().truncatedTo(ChronoUnit.SECONDS))
            .arguments(new ArrayList<>())
            .retryStrategy(
                FixedRetryStrategy.builder().retryDelay(Duration.ZERO).maxRetries(1).build())
            .timeout(Duration.ofSeconds(10))
            .build();
    OperationRequest req2 =
        req1.toBuilder().workflowInstanceId(wfId2).operationRequestId("req2").build();

    assertTrue(store.createOperationRequest(req1));
    assertTrue(store.createOperationRequest(req2));
    val error =
        assertThrows(
            StorageError.class,
            () ->
                store.createOperationRequest(
                    req1)); // Trying to recreate the same operation should fail
    assertEquals(StorageError.Type.DUPLICATE_ENTRY, error.getType());
    assertEquals(store.getOperationRequest(req1.getOperationRequestId()), req1);
    // Get all requests
    val requests1 = store.getOperationRequests(wfId1);
    assertEquals(1, requests1.size());
    assertEquals(requests1.get(0), req1);

    val requests2 = store.getOperationRequests(wfId2);
    assertEquals(1, requests2.size());
    assertEquals(requests2.get(0), req2);
    store.incrementOperationRequestFailedAttempts(
        req1.getOperationRequestId(), req1.getFailedAttempts());
    val updatedReq = store.getOperationRequest(req1.getOperationRequestId());
    assertEquals(1, updatedReq.getFailedAttempts());
  }

  @Test
  public void testOperationResponses() {
    final String wfId1 = "wftest123";
    final String wfId2 = "wftest1234";
    final OperationType operationType = new OperationType(String.class, "testmethod");
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
        res1.toBuilder().id("res2").workflowInstanceId(wfId2).operationRequestId("op2").build();
    OperationResponse res1Success =
        res1.toBuilder().isSuccess(true).isTransient(false).id("res3").build();

    assertTrue(store.createOperationResponse(res1));
    assertFalse(
        store.createOperationResponse(
            res1)); // Immediately trying to create the same response should fail
    assertTrue(store.createOperationResponse(res2));
    assertTrue(store.createOperationResponse(res1Success));
    // Trying to create a new response after a non-transient response has been recorded for the same
    // execution should fail.
    assertFalse(store.createOperationResponse(res1.toBuilder().id("newId").build()));

    assertEquals(2, store.getOperationResponses(wfId1, true).size());
    assertEquals(1, store.getOperationResponses(wfId1, false).size());
    assertEquals(1, store.getOperationResponses(wfId2, true).size());
  }
}
