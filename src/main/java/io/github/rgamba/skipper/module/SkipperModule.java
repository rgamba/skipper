package io.github.rgamba.skipper.module;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import io.github.rgamba.skipper.DecisionExecutor;
import io.github.rgamba.skipper.SyncDecisionExecutor;
import io.github.rgamba.skipper.store.OperationStore;
import io.github.rgamba.skipper.store.SqlTransactionManager;
import io.github.rgamba.skipper.store.TimerStore;
import io.github.rgamba.skipper.store.WorkflowInstanceStore;
import io.github.rgamba.skipper.store.mysql.MySqlOperationStore;
import io.github.rgamba.skipper.store.mysql.MySqlTimerStore;
import io.github.rgamba.skipper.store.mysql.MySqlWorkflowInstanceStore;
import java.time.Clock;

public class SkipperModule extends AbstractModule {
  private final String jdbcUrl;
  private final String dbUser;
  private final String dbPass;

  public SkipperModule(String jdbcUrl, String dbUser, String dbPass) {
    this.jdbcUrl = jdbcUrl;
    this.dbUser = dbUser;
    this.dbPass = dbPass;
  }

  @Override
  protected void configure() {
    bind(Clock.class).annotatedWith(Names.named("UTC")).toInstance(Clock.systemUTC());
    bind(WorkflowInstanceStore.class).to(MySqlWorkflowInstanceStore.class);
    bind(TimerStore.class).to(MySqlTimerStore.class);
    bind(OperationStore.class).to(MySqlOperationStore.class);
    bind(DecisionExecutor.class).to(SyncDecisionExecutor.class);
    bindConstant().annotatedWith(SqlTransactionManager.JdbcUrl.class).to(jdbcUrl);
    bindConstant().annotatedWith(SqlTransactionManager.DbUser.class).to(dbUser);
    bindConstant().annotatedWith(SqlTransactionManager.DbPassword.class).to(dbPass);
  }
}
