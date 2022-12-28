package maestro.module;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import java.time.Clock;
import maestro.store.*;
import maestro.store.mysql.MySqlOperationStore;
import maestro.store.mysql.MySqlTimerStore;
import maestro.store.mysql.MySqlWorkflowInstanceStore;

public class MaestroModule extends AbstractModule {
  private final String jdbcUrl;
  private final String dbUser;
  private final String dbPass;

  public MaestroModule(String jdbcUrl, String dbUser, String dbPass) {
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
    bindConstant().annotatedWith(SqlTransactionManager.JdbcUrl.class).to(jdbcUrl);
    bindConstant().annotatedWith(SqlTransactionManager.DbUser.class).to(dbUser);
    bindConstant().annotatedWith(SqlTransactionManager.DbPassword.class).to(dbPass);
  }
}
