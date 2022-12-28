package demo;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import demo.operations.Operations;
import demo.workflows.TransferWorkflow;
import lombok.val;
import maestro.api.OperationError;
import maestro.testUtils.WorkflowTest;
import org.junit.Before;
import org.junit.Test;

public class TransferWorkflowTest extends WorkflowTest {
  private TransferWorkflow transferWorkflow;
  private Operations mockOperations;

  @Before
  public void setUp() {
    super.setUp();
    mockOperations = mock(Operations.class);
    transferWorkflow = new TransferWorkflow();
    mockOperationField(transferWorkflow, "operations", mockOperations);
  }

  @Test
  public void testTransferHappyPath() throws Exception {
    // given
    when(mockOperations.withdraw(any(), any(), any())).thenReturn("");
    when(mockOperations.deposit(any(), any(), any())).thenReturn("");
    // when
    val result = transferWorkflow.transfer("a", "b", 10);
    // then
    assertEquals(1, 1);
    verify(mockOperations, times(1)).withdraw(eq("a"), eq(11), anyString());
    verify(mockOperations, times(2)).deposit(any(), any(), anyString());
  }

  @Test
  public void testTransferWhenDepositFails() throws Exception {
    // given
    when(mockOperations.withdraw(any(), any(), any())).thenReturn("123");
    when(mockOperations.deposit(any(), any(), any()))
        .thenThrow(new OperationError(new RuntimeException("something went wrong")));
    // when
    val result = transferWorkflow.transfer("a", "b", 10);
    // then
    assertEquals(1, 1);
    verify(mockOperations, times(1)).rollbackWithdraw(eq("123"), anyString());
  }
}
