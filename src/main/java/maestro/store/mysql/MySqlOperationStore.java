package maestro.store.mysql;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import maestro.common.Anything;
import maestro.models.*;
import maestro.serde.SerdeUtils;
import maestro.store.DateTimeUtil;
import maestro.store.OperationStore;
import maestro.store.SqlTransactionManager;
import maestro.store.StorageError;

public class MySqlOperationStore implements OperationStore {
  private final SqlTransactionManager transactionManager;
  private final Gson gson = SerdeUtils.getGson();

  @Inject
  public MySqlOperationStore(@NonNull SqlTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  @Override
  public boolean createOperationRequest(@NonNull OperationRequest operationRequest) {
    val sql =
        ""
            + "INSERT INTO operation_requests (id, workflow_instance_id, operation_type, iteration, creation_time, arguments, retry_strategy, timeout_secs, failed_attempts) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(sql)) {
            int i = 0;
            ps.setString(++i, operationRequest.getOperationRequestId());
            ps.setString(++i, operationRequest.getWorkflowInstanceId());
            ps.setString(++i, gson.toJson(operationRequest.getOperationType()));
            ps.setInt(++i, operationRequest.getIteration());
            ps.setTimestamp(
                ++i, DateTimeUtil.instantToTimestamp(operationRequest.getCreationTime()));
            ps.setString(++i, gson.toJson(operationRequest.getArguments()));
            ps.setString(++i, gson.toJson(operationRequest.getRetryStrategy()));
            ps.setLong(++i, operationRequest.getTimeout().getSeconds());
            ps.setInt(++i, operationRequest.getFailedAttempts());
            return ps.executeUpdate();
          } catch (SQLException e) {
            if (isDuplicateKeyError(e)) {
              throw new StorageError(
                  "duplicate operation request ID", e, StorageError.Type.DUPLICATE_ENTRY);
            }
            throw new StorageError(
                "unexpected mysql error when trying to create operation request", e);
          }
        });
    return true;
  }

  @Override
  public void incrementOperationRequestFailedAttempts(
      @NonNull String operationRequestId, int currentRetries) {
    val sql =
        ""
            + "UPDATE operation_requests SET failed_attempts = failed_attempts + 1 "
            + "WHERE id = ? AND failed_attempts = ?";
    transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(sql)) {
            int i = 0;
            ps.setString(++i, operationRequestId);
            ps.setInt(++i, currentRetries);
            ps.executeUpdate();
          } catch (SQLException e) {
            throw new StorageError("unable to increment failed attempts on operation request", e);
          }
          return true;
        });
  }

  @Override
  public boolean createOperationResponse(@NonNull OperationResponse resp) {
    val sql =
        ""
            + "INSERT INTO operation_responses (id, workflow_instance_id, operation_type, iteration, creation_time_millis, is_success, is_transient, operation_request_id, result, error, execution_duration_millis, child_workflow_instance_id) "
            + "SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? FROM dual "
            + "WHERE NOT EXISTS ("
            + "    SELECT * FROM operation_responses "
            + "    WHERE workflow_instance_id = ? "
            + "        AND operation_type = ? "
            + "        AND iteration = ? "
            + "        AND is_transient = false "
            + ")";
    return transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(sql)) {
            int i = 0;
            // Insert values
            ps.setString(++i, resp.getId());
            ps.setString(++i, resp.getWorkflowInstanceId());
            ps.setString(++i, gson.toJson(resp.getOperationType()));
            ps.setInt(++i, resp.getIteration());
            ps.setLong(++i, resp.getCreationTime().toEpochMilli());
            ps.setBoolean(++i, resp.isSuccess());
            ps.setBoolean(++i, resp.isTransient());
            ps.setString(++i, resp.getOperationRequestId());
            val resultJson = resp.getResult() == null ? null : gson.toJson(resp.getResult());
            ps.setString(++i, resultJson);
            val errorJson = resp.getError() == null ? null : gson.toJson(resp.getError());
            ps.setString(++i, errorJson);
            ps.setLong(
                ++i,
                resp.getExecutionDuration() == null ? 0 : resp.getExecutionDuration().toMillis());
            ps.setString(++i, resp.getChildWorkflowInstanceId());
            // Where clause values
            ps.setString(++i, resp.getWorkflowInstanceId());
            ps.setString(++i, gson.toJson(resp.getOperationType()));
            ps.setInt(++i, resp.getIteration());
            return ps.executeUpdate() == 1;
          } catch (SQLException e) {
            if (isDuplicateKeyError(e)) {
              return false;
            }
            throw new StorageError(
                "unexpected mysql error when trying to create operation response", e);
          }
        });
  }

  private boolean isDuplicateKeyError(SQLException e) {
    return e.getMessage().toLowerCase().contains("duplicate entry");
  }

  @Override
  public List<OperationResponse> getOperationResponses(
      @NonNull String workflowInstanceId, boolean includeTransientResponses) {
    StringBuilder sql = new StringBuilder();
    sql.append(
        ""
            + "SELECT id, workflow_instance_id, operation_type, iteration, creation_time_millis, is_success, is_transient, operation_request_id, result, error, execution_duration_millis, child_workflow_instance_id "
            + "FROM operation_responses "
            + "WHERE workflow_instance_id = ?");
    if (!includeTransientResponses) {
      sql.append(" AND is_transient = false");
    }
    sql.append(" ORDER BY creation_time_millis ASC");
    String finalSql = sql.toString();
    return transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(finalSql)) {
            ps.setString(1, workflowInstanceId);
            val result = ps.executeQuery();
            List<OperationResponse> responses = new ArrayList<>();
            while (result.next()) {
              responses.add(recordToOperationResponse(result));
            }
            return responses;
          } catch (SQLException e) {
            throw new StorageError(
                "unexpected mysql error while trying to get operation responses", e);
          }
        });
  }

  @SneakyThrows
  private OperationResponse recordToOperationResponse(ResultSet result) {
    val builder = OperationResponse.builder();
    builder.id(result.getString("id"));
    builder.workflowInstanceId(result.getString("workflow_instance_id"));
    builder.operationType(gson.fromJson(result.getString("operation_type"), OperationType.class));
    builder.iteration(result.getInt("iteration"));
    builder.creationTime(Instant.ofEpochMilli(result.getLong("creation_time_millis")));
    builder.isSuccess(result.getBoolean("is_success"));
    builder.isTransient(result.getBoolean("is_transient"));
    builder.operationRequestId(result.getString("operation_request_id"));
    val resultJson = result.getString("result");
    val errorJson = result.getString("error");
    builder.result(resultJson != null ? gson.fromJson(resultJson, Anything.class) : null);
    builder.error(errorJson != null ? gson.fromJson(errorJson, Anything.class) : null);
    long executionDurationMillis = result.getLong("execution_duration_millis");
    builder.executionDuration(
        executionDurationMillis == 0 ? null : Duration.ofMillis(executionDurationMillis));
    builder.childWorkflowInstanceId(result.getString("child_workflow_instance_id"));
    return builder.build();
  }

  @Override
  public OperationRequest getOperationRequest(@NonNull String operationRequestId) {
    val sql =
        ""
            + "SELECT id, workflow_instance_id, operation_type, iteration, creation_time, arguments, retry_strategy, timeout_secs, failed_attempts "
            + "FROM operation_requests "
            + "WHERE id = ? ";
    return transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(sql)) {
            ps.setString(1, operationRequestId);
            val result = ps.executeQuery();
            if (!result.first()) {
              throw new IllegalArgumentException(
                  String.format(
                      "unable to find operation request with id '%s'", operationRequestId));
            }
            return recordToOperationRequest(result);
          } catch (SQLException e) {
            throw new StorageError(
                "unexpected mysql error when trying to get operation request", e);
          }
        });
  }

  @SneakyThrows
  private OperationRequest recordToOperationRequest(ResultSet result) {
    val builder = OperationRequest.builder();
    builder.operationRequestId(result.getString("id"));
    builder.workflowInstanceId(result.getString("workflow_instance_id"));
    builder.operationType(gson.fromJson(result.getString("operation_type"), OperationType.class));
    builder.iteration(result.getInt("iteration"));
    builder.creationTime(result.getTimestamp("creation_time").toInstant());
    builder.arguments(
        gson.fromJson(
            result.getString("arguments"),
            TypeToken.getParameterized(List.class, Anything.class).getType()));
    builder.retryStrategy(gson.fromJson(result.getString("retry_strategy"), RetryStrategy.class));
    builder.timeout(Duration.ofSeconds(result.getLong("timeout_secs")));
    builder.failedAttempts(result.getInt("failed_attempts"));
    return builder.build();
  }

  @Override
  public List<OperationRequest> getOperationRequests(@NonNull String workflowInstanceId) {
    val sql =
        ""
            + "SELECT id, workflow_instance_id, operation_type, iteration, creation_time, arguments, retry_strategy, timeout_secs, failed_attempts "
            + "FROM operation_requests "
            + "WHERE workflow_instance_id = ? "
            + "ORDER BY creation_time ASC";
    return transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(sql)) {
            ps.setString(1, workflowInstanceId);
            val result = ps.executeQuery();
            List<OperationRequest> requests = new ArrayList<>();
            while (result.next()) {
              requests.add(recordToOperationRequest(result));
            }
            return requests;
          } catch (SQLException e) {
            throw new StorageError(
                "unexpected mysql error when trying to get operation request", e);
          }
        });
  }

  @Override
  public void convertAllErrorResponsesToTransient(String workflowInstanceId) {
    val sql =
        ""
            + "UPDATE operation_responses SET is_transient = true WHERE id IN ("
            + "    SELECT * FROM (SELECT id FROM operation_responses WHERE workflow_instance_id = ? AND is_success = false"
            + "    ORDER BY creation_time_millis DESC LIMIT 1) tmp"
            + ")";
    transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(sql)) {
            ps.setString(1, workflowInstanceId);
            return ps.executeUpdate();
          } catch (SQLException e) {
            throw new StorageError(
                "unexpected mysql error when trying to convert transient responses to true", e);
          }
        });
  }
}
