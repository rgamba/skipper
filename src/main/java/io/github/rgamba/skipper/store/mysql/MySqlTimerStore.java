package io.github.rgamba.skipper.store.mysql;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.github.rgamba.skipper.Metrics;
import io.github.rgamba.skipper.common.Anything;
import io.github.rgamba.skipper.models.Timer;
import io.github.rgamba.skipper.serde.SerdeUtils;
import io.github.rgamba.skipper.store.SqlTransactionManager;
import io.github.rgamba.skipper.store.StorageError;
import io.github.rgamba.skipper.store.TimerStore;
import io.github.rgamba.skipper.timers.TimerHandler;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

public class MySqlTimerStore implements TimerStore {
  private final SqlTransactionManager transactionManager;
  private static final Gson gson = SerdeUtils.getGson();
  private final Clock clock;
  public static final Duration leaseDuration = Duration.ofSeconds(15);

  @Inject
  public MySqlTimerStore(
      @NonNull SqlTransactionManager transactionManager, @NonNull @Named("UTC") Clock clock) {
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  @Override
  public Timer createOrUpdate(@NonNull Timer timer) {
    val sql =
        ""
            + "INSERT INTO timers (id, timeout_ts_millis, handler_clazz, payload, retries, version) "
            + "    VALUES (?, ?, ?, ?, ?, 0) "
            + "ON DUPLICATE KEY"
            + "    UPDATE timeout_ts_millis = ?, handler_clazz = ?, payload = ?, retries = ?, version = version + 1";
    val builder = timer.toBuilder();
    int version =
        transactionManager.execute(
            conn -> {
              try {
                val ps = conn.prepareStatement(sql);
                int i = 0;
                long timeoutMillis =
                    timer.getTimeout() == null ? 0 : timer.getTimeout().toEpochMilli();
                // Values for INSERT
                ps.setString(++i, timer.getTimerId());
                ps.setLong(++i, timeoutMillis);
                ps.setString(++i, timer.getHandlerClazz().getName());
                ps.setString(++i, gson.toJson(timer.getPayload()));
                ps.setInt(++i, timer.getRetries());
                // Values for UPDATE
                ps.setLong(++i, timeoutMillis);
                ps.setString(++i, timer.getHandlerClazz().getName());
                ps.setString(++i, gson.toJson(timer.getPayload()));
                ps.setInt(++i, timer.getRetries());
                if (ps.executeUpdate() == 1) {
                  return timer.getVersion() + 1;
                }
                return timer.getVersion();
              } catch (SQLException e) {
                Metrics.errorCounter("timer", "unknown").inc();
                throw new StorageError("unable to upsert timer: " + e.getMessage(), e);
              }
            });
    return builder.version(version).build();
  }

  @Override
  public Timer get(@NonNull String timerId) {
    val sql =
        ""
            + "SELECT id, timeout_ts_millis, handler_clazz, payload, retries, version FROM timers "
            + "WHERE id = ?";
    return this.transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(sql)) {
            ps.setString(1, timerId);
            val result = ps.executeQuery();
            if (!result.next()) {
              throw new IllegalArgumentException("unable to find timer with id " + timerId);
            }
            return recordToInstance(result);
          } catch (SQLException e) {
            throw new StorageError("unexpected mysql error", e);
          }
        });
  }

  @SneakyThrows
  private Timer recordToInstance(ResultSet result) {
    val builder = Timer.builder();
    builder.timerId(result.getString("id"));
    builder.version(result.getInt("version"));
    builder.handlerClazz(
        Class.forName(result.getString("handler_clazz")).asSubclass(TimerHandler.class));
    long timeoutMillis = result.getLong("timeout_ts_millis");
    if (timeoutMillis > 0) {
      builder.timeout(Instant.ofEpochMilli(timeoutMillis));
    }
    builder.payload(gson.fromJson(result.getString("payload"), Anything.class));
    builder.retries(result.getInt("retries"));
    return builder.build();
  }

  @Override
  public void update(@NonNull String timerId, Duration timeout) {
    val sql = "UPDATE timers SET timeout_ts_millis = ? WHERE id = ?";
    this.transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(sql)) {
            ps.setLong(1, clock.instant().plus(timeout).toEpochMilli());
            ps.setString(2, timerId);
            return ps.executeUpdate();
          } catch (SQLException e) {
            throw new StorageError("unable to update timer: " + e.getMessage(), e);
          }
        });
  }

  @Override
  public void update(
      @NonNull String timerId, @NonNull Duration timeout, @NonNull Anything payload) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean delete(@NonNull Timer timer) {
    val sql = "DELETE FROM timers WHERE id = ? AND version = ?";
    return this.transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(sql)) {
            ps.setString(1, timer.getTimerId());
            ps.setInt(2, timer.getVersion());
            return ps.executeUpdate() > 0;
          } catch (SQLException e) {
            throw new StorageError("unexpected mysql error", e);
          }
        });
  }

  @Override
  public List<Timer> getExpiredTimers() {
    val expiredIds = getExpiredIds();
    if (expiredIds.isEmpty()) {
      return new ArrayList<>();
    }
    val sql =
        String.format(
            ""
                + "SELECT id, timeout_ts_millis, handler_clazz, payload, retries, version FROM timers "
                + "WHERE id IN ('%s') "
                + "FOR UPDATE SKIP LOCKED",
            String.join("','", expiredIds));
    val updateLeaseSql = "" + "UPDATE timers SET timeout_ts_millis = ? WHERE id IN ('%s')";
    try (val latencyTimer = Metrics.getStoreLatencyTimer("timers", "get_expired").time()) {
      return this.transactionManager.execute(
          conn -> {
            // First fetch the results
            List<Timer> timers = new ArrayList<>();
            val now = clock.instant();
            try (val ps = conn.prepareStatement(sql)) {
              val result = ps.executeQuery();
              while (result.next()) {
                timers.add(recordToInstance(result));
              }
            } catch (SQLException e) {
              Metrics.errorCounter("timers", "getExpiredTimers").inc();
              throw new StorageError(
                  "unexpected mysql error while trying to select timers" + e.getMessage(), e);
            }
            // Now update the timers lease
            String ids = timers.stream().map(Timer::getTimerId).collect(Collectors.joining("','"));
            try (val ps = conn.prepareStatement(String.format(updateLeaseSql, ids))) {
              ps.setLong(1, now.plus(leaseDuration).toEpochMilli());
              ps.executeUpdate();
            } catch (SQLException e) {
              throw new StorageError(
                  "unexpected mysql error while trying to update the timers lease" + e.getMessage(),
                  e);
            }
            return timers;
          });
    }
  }

  private List<String> getExpiredIds() {
    val sql =
        "SELECT id FROM timers WHERE timeout_ts_millis <= ? ORDER BY timeout_ts_millis ASC, id ASC LIMIT 300";
    return this.transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(sql)) {
            List<String> timers = new ArrayList<>();
            ps.setLong(1, clock.instant().toEpochMilli());
            val result = ps.executeQuery();
            while (result.next()) {
              timers.add(result.getString("id"));
            }
            return timers;
          } catch (SQLException e) {
            throw new StorageError(
                "unexpected mysql error while trying to get expired timers" + e.getMessage(), e);
          }
        });
  }

  @Override
  public long countExpiredTimers() {
    val sql = "SELECT count(*) FROM timers WHERE timeout_ts_millis <= ? ";
    return this.transactionManager.execute(
        conn -> {
          try (val ps = conn.prepareStatement(sql)) {
            ps.setLong(1, clock.instant().toEpochMilli());
            val record = ps.executeQuery();
            record.next();
            return record.getLong(1);
          } catch (SQLException e) {
            throw new StorageError("unable to count timers: " + e.getMessage(), e);
          }
        });
  }
}
