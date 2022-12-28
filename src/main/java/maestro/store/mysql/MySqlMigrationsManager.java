package maestro.store.mysql;

import org.flywaydb.core.Flyway;

public class MySqlMigrationsManager {
  public static void migrate() {
    Flyway flyway =
        Flyway.configure()
            .dataSource("jdbc:mysql://localhost:3306/maestro?serverTimezone=UTC", "root", "root")
            .load();
    flyway.migrate();
  }
}
