package com.maestroworkflow.store;

import com.google.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.dbcp2.BasicDataSource;

@Singleton
public class SqlTransactionManager {
  private final BasicDataSource ds;
  private final ThreadLocal<Connection> currentTransaction;

  public SqlTransactionManager(
      @NonNull String url, @NonNull String user, @NonNull String password) {
    ds = new BasicDataSource();
    ds.setUrl(url);
    ds.setUsername(user);
    ds.setPassword(password);
    ds.setMaxIdle(10);
    ds.setMinIdle(5);
    this.currentTransaction = new ThreadLocal<>();
  }

  public Connection getConnection() throws SQLException {
    return ds.getConnection();
  }

  @SneakyThrows
  public <T> T execute(Function<Connection, T> lambda) {
    boolean inTransaction = currentTransaction.get() != null;
    try {
      if (!inTransaction) {
        begin();
      }
      T result = lambda.apply(currentTransaction.get());
      if (!inTransaction) {
        currentTransaction.get().commit();
      }
      return result;
    } finally {
      if (!inTransaction) {
        currentTransaction.get().close();
        currentTransaction.remove();
      }
    }
  }

  @SneakyThrows
  public void begin() {
    if (currentTransaction.get() == null) {
      currentTransaction.set(getConnection());
      currentTransaction.get().setAutoCommit(false);
    }
  }

  @SneakyThrows
  public void commit() {
    if (currentTransaction.get() != null) {
      currentTransaction.get().commit();
      currentTransaction.get().close();
      currentTransaction.remove();
    }
  }

  @SneakyThrows
  public void rollback() {
    if (currentTransaction.get() != null) {
      currentTransaction.get().rollback();
      currentTransaction.get().close();
      currentTransaction.remove();
    }
  }
}
