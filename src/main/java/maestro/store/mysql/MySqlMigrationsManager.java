package maestro.store.mysql;

import com.google.inject.Inject;
import lombok.NonNull;
import maestro.store.SqlTransactionManager;
import org.flywaydb.core.Flyway;

public class MySqlMigrationsManager {

  private final String url;
  private final String user;
  private final String password;

  @Inject
  public MySqlMigrationsManager(
      @NonNull @SqlTransactionManager.JdbcUrl String url,
      @NonNull @SqlTransactionManager.DbUser String user,
      @NonNull @SqlTransactionManager.DbPassword String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public void migrate() {
    Flyway flyway = Flyway.configure().dataSource(this.url, this.user, this.password).load();
    flyway.migrate();
  }
}
