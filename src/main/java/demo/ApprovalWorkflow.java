package demo;

import com.maestroworkflow.OperationProxyFactory;
import com.maestroworkflow.api.MaestroWorkflow;
import com.maestroworkflow.api.OperationConfig;
import com.maestroworkflow.api.annotations.WorkflowMethod;
import com.maestroworkflow.models.FixedRetryStrategy;

import java.time.Duration;

public class ApprovalWorkflow implements MaestroWorkflow {
    private final Operations operations = OperationProxyFactory.create(Operations.class,
            OperationConfig.builder()
                    .retryStrategy(FixedRetryStrategy.builder()
                            .retryDelay(Duration.ofSeconds(2))
                            .maxRetries(3)
                            .build())
                    .build());
    @WorkflowMethod
    public String startApproval(String user) {
        operations.debit(user, 1);
        return "approval granted for " + user;
    }
}
