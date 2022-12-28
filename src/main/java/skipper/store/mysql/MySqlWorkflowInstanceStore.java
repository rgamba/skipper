package skipper.store.mysql;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import skipper.api.CallbackHandler;
import skipper.common.Anything;
import skipper.models.WorkflowInstance;
import skipper.models.WorkflowType;
import skipper.serde.SerdeUtils;
import skipper.store.DateTimeUtil;
import skipper.store.SqlTransactionManager;
import skipper.store.StorageError;
import skipper.store.WorkflowInstanceStore;

public class MySqlWorkflowInstanceStore implements WorkflowInstanceStore {

  private static final Gson gson = SerdeUtils.getGson();

  private final SqlTransactionManager transactionManager;

  @Inject
  public MySqlWorkflowInstanceStore(@NonNull SqlTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  @Override
  @SneakyThrows
  public void create(@NonNull WorkflowInstance workflowInstance) {
    val sql =
        "INSERT INTO workflow_instances (id, correlation_id, workflow_type, initial_args, status, callback_handler_clazz, creation_time, state, result, status_reason, version)"
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    this.transactionManager.execute(
        conn -> {
          try {
            try (val ps = conn.prepareStatement(sql)) {
              int i = 0;
              ps.setString(++i, workflowInstance.getId());
              ps.setString(++i, workflowInstance.getCorrelationId());
              ps.setString(++i, gson.toJson(workflowInstance.getWorkflowType()));
              ps.setString(
                  ++i,
                  gson.toJson(
                      workflowInstance.getInitialArgs(),
                      TypeToken.getParameterized(List.class, Anything.class).getType()));
              ps.setString(++i, WorkflowInstance.Status.ACTIVE.toString());
              ps.setString(
                  ++i,
                  workflowInstance.getCallbackHandlerClazz() != null
                      ? workflowInstance.getCallbackHandlerClazz().getName()
                      : null);
              ps.setTimestamp(++i, Timestamp.from(workflowInstance.getCreationTime()));
              ps.setString(
                  ++i,
                  gson.toJson(
                      workflowInstance.getState(),
                      TypeToken.getParameterized(Map.class, String.class, Anything.class)
                          .getType()));
              ps.setString(
                  ++i,
                  workflowInstance.getResult() == null
                      ? null
                      : new Gson().toJson(workflowInstance.getResult()));
              ps.setString(++i, workflowInstance.getStatusReason());
              ps.setInt(++i, workflowInstance.getVersion());
              return ps.executeUpdate();
            }
          } catch (SQLException e) {
            if (e.getMessage().toLowerCase().contains("duplicate entry")) {
              throw new StorageError(
                  "unable to create workflow instance", e, StorageError.Type.DUPLICATE_ENTRY);
            }
            throw new StorageError("unexpected mysql error", e);
          }
        });
  }

  @Override
  @SneakyThrows
  public WorkflowInstance get(@NonNull String workflowInstanceId) {
    val sql =
        "SELECT id, correlation_id, workflow_type, initial_args, status, callback_handler_clazz, creation_time, state, result, status_reason, version FROM workflow_instances"
            + "  WHERE id = ?";
    return this.transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(sql)) {
            ps.setString(1, workflowInstanceId);
            val result = ps.executeQuery();
            if (!result.first()) {
              throw new IllegalArgumentException(
                  "unable to find workflow instance with id " + workflowInstanceId);
            }
            return recordToInstance(result);
          } catch (SQLException e) {
            throw new StorageError("unexpected mysql error", e);
          }
        });
  }

  @SneakyThrows
  private WorkflowInstance recordToInstance(ResultSet result) {
    val builder = WorkflowInstance.builder();

    try {
      builder.id(result.getString("id"));
      builder.correlationId(result.getString("correlation_id"));
      val workflowType = gson.fromJson(result.getString("workflow_type"), WorkflowType.class);
      builder.workflowType(workflowType);
      List<Anything> initialArgs =
          gson.fromJson(
              result.getString("initial_args"),
              TypeToken.getParameterized(List.class, Anything.class).getType());
      builder.initialArgs(initialArgs);
      builder.status(WorkflowInstance.Status.valueOf(result.getString("status")));
      String callbackHandlerClazz = result.getString("callback_handler_clazz");
      if (callbackHandlerClazz != null) {
        builder.callbackHandlerClazz(
            Class.forName(callbackHandlerClazz).asSubclass(CallbackHandler.class));
      }
      val creationTime =
          DateTimeUtil.truncateInstant(result.getTimestamp("creation_time").toInstant());
      builder.creationTime(creationTime);
      String stateJson = result.getString("state");
      if (stateJson != null) {
        Map<String, Anything> state =
            gson.fromJson(
                stateJson,
                TypeToken.getParameterized(Map.class, String.class, Anything.class).getType());
        builder.state(state);
      }
      String resultVal = result.getString("result");
      if (resultVal != null) {
        builder.result(gson.fromJson(resultVal, Anything.class));
      }
      builder.statusReason(result.getString("status_reason"));
      builder.version(result.getInt("version"));
    } catch (ClassCastException | ClassNotFoundException e) {
      throw new StorageError("unable to deserialize into a class type", e);
    }
    return builder.build();
  }

  @Override
  public List<WorkflowInstance> find() {
    val sql =
        ""
            + "SELECT id, correlation_id, workflow_type, initial_args, status, callback_handler_clazz, creation_time, state, result, status_reason, version "
            + "FROM workflow_instances "
            + "ORDER BY creation_time DESC";
    return this.transactionManager.execute(
        conn -> {
          List<WorkflowInstance> instances = new ArrayList<>();
          try (val ps = conn.prepareStatement(sql)) {
            val result = ps.executeQuery();
            while (result.next()) {
              instances.add(recordToInstance(result));
            }
            return instances;
          } catch (SQLException e) {
            throw new StorageError("unexpected mysql error", e);
          }
        });
  }

  @Override
  public void update(
      @NonNull String workflowInstanceId,
      WorkflowInstance.@NonNull Mutation mutation,
      int version) {
    Map<String, String> fieldsToUpdate = new LinkedHashMap<>();
    if (mutation.getResult() != null) {
      fieldsToUpdate.put("result", gson.toJson(mutation.getResult()));
    }
    if (mutation.getStatus() != null) {
      fieldsToUpdate.put("status", mutation.getStatus().name());
    }
    if (mutation.getState() != null) {
      fieldsToUpdate.put("state", gson.toJson(mutation.getState()));
    }
    if (mutation.getStatusReason() != null) {
      fieldsToUpdate.put("status_reason", mutation.getStatusReason());
    }
    String assignments =
        fieldsToUpdate.keySet().stream().map(key -> key + " = ?").collect(Collectors.joining(","));
    val sql =
        "UPDATE workflow_instances SET "
            + assignments
            + ", version = version + 1 "
            + "WHERE id = ? AND version = ?";
    int updatedRows =
        this.transactionManager.execute(
            conn -> {
              try {
                try (val ps = conn.prepareStatement(sql)) {
                  int i = 0;
                  for (val entry : fieldsToUpdate.entrySet()) {
                    ps.setString(++i, entry.getValue());
                  }
                  ps.setString(++i, workflowInstanceId);
                  ps.setInt(++i, version);
                  return ps.executeUpdate();
                }
              } catch (SQLException e) {
                throw new StorageError("unexpected mysql error", e);
              }
            });
    if (updatedRows < 1) {
      throw new StorageError(
          "unable to update workflow instance, probably a caused by an optimistic lock error!");
    }
  }
}
