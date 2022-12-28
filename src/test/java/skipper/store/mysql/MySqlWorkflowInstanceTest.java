package skipper.store.mysql;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import lombok.val;
import lombok.var;
import org.junit.Before;
import org.junit.Test;
import skipper.api.CallbackHandler;
import skipper.api.SkipperWorkflow;
import skipper.common.Anything;
import skipper.models.WorkflowInstance;
import skipper.models.WorkflowType;
import skipper.store.SqlTestHelper;
import skipper.store.StorageError;

public class MySqlWorkflowInstanceTest extends SqlTestHelper {
  private MySqlWorkflowInstanceStore store;
  private final WorkflowInstance instance =
      WorkflowInstance.builder()
          .id(UUID.randomUUID().toString())
          .workflowType(new WorkflowType(SkipperWorkflow.class))
          .status(WorkflowInstance.Status.ACTIVE)
          .state(
              new HashMap<String, Anything>() {
                {
                  put("foo", Anything.of("bar"));
                }
              })
          .correlationId(UUID.randomUUID().toString())
          .version(1)
          .initialArgs(
              new ArrayList<Anything>() {
                {
                  add(Anything.of("test"));
                  add(Anything.of(123));
                }
              })
          .result(Anything.of("test"))
          .creationTime(Instant.now())
          .build();

  @Before
  public void setUp() {
    super.setUp();
    store = new MySqlWorkflowInstanceStore(trxMgr);
  }

  @Test
  public void testCreateAndGet() throws Exception {
    // create normal
    var newInstance = instance;
    store.create(newInstance);
    var storedInstance = store.get(newInstance.getId());
    assertEquals(newInstance, storedInstance);
    // trying to create the same one again should fail
    val newInstanceCopy = newInstance;
    assertThrows(StorageError.class, () -> store.create(newInstanceCopy));
    // now with null result
    newInstance =
        instance
            .toBuilder()
            .result(null)
            .correlationId(UUID.randomUUID().toString())
            .id(UUID.randomUUID().toString())
            .build();
    store.create(newInstance);
    storedInstance = store.get(newInstance.getId());
    assertEquals(newInstance, storedInstance);
    // now with null state
    newInstance =
        instance
            .toBuilder()
            .state(new HashMap<>())
            .correlationId(UUID.randomUUID().toString())
            .id(UUID.randomUUID().toString())
            .build();
    store.create(newInstance);
    storedInstance = store.get(newInstance.getId());
    assertEquals(newInstance, storedInstance);
    // now with callback handler class
    newInstance =
        instance
            .toBuilder()
            .callbackHandlerClazz(CallbackHandler.class)
            .correlationId(UUID.randomUUID().toString())
            .id(UUID.randomUUID().toString())
            .build();
    store.create(newInstance);
    storedInstance = store.get(newInstance.getId());
    assertEquals(newInstance, storedInstance);
    // now with not null status reason
    newInstance =
        instance
            .toBuilder()
            .statusReason("this is a test!")
            .correlationId(UUID.randomUUID().toString())
            .id(UUID.randomUUID().toString())
            .build();
    store.create(newInstance);
    storedInstance = store.get(newInstance.getId());
    assertEquals(newInstance, storedInstance);
  }

  @Test
  public void testCreateWhenInstanceAlreadyExists() {
    var newInstance = instance;
    store.create(newInstance);
    // Now try creating it again
    StorageError err = assertThrows(StorageError.class, () -> store.create(newInstance));
    assertEquals(err.getType(), StorageError.Type.DUPLICATE_ENTRY);
  }

  @Test
  public void testUpdate() {
    val newInstance =
        instance
            .toBuilder()
            .correlationId(UUID.randomUUID().toString())
            .id(UUID.randomUUID().toString())
            .build();
    store.create(newInstance);
    HashMap<String, Anything> newState = new HashMap<>();
    newState.put("test", Anything.of("test val"));
    var mutation =
        WorkflowInstance.Mutation.builder()
            .state(newState)
            .statusReason("new status reason")
            .result(Anything.of("the new result!"))
            .status(WorkflowInstance.Status.COMPLETED)
            .build();
    store.update(newInstance.getId(), mutation, newInstance.getVersion());
    val updated = store.get(newInstance.getId());
    assertEquals(2, updated.getVersion());
    assertEquals("new status reason", updated.getStatusReason());
    assertEquals(newState, updated.getState());
    assertEquals(WorkflowInstance.Status.COMPLETED, updated.getStatus());
    assertEquals(Anything.of("the new result!"), updated.getResult());
    // Now trying to update a stale instance should fail
    StorageError error =
        assertThrows(
            StorageError.class,
            () -> store.update(newInstance.getId(), mutation, newInstance.getVersion()));
    assertTrue(error.getMessage().contains("optimistic lock error"));
  }
}
