package skipper.store;

import org.junit.After;
import org.junit.Before;

public abstract class SqlTestHelper {
  protected SqlTransactionManager trxMgr;

  @Before
  public void setUp() {
    trxMgr =
        new SqlTransactionManager(
            "jdbc:mysql://localhost:3306/maestro?serverTimezone=UTC", "root", "root");
    trxMgr.begin();
  }

  @After
  public void tearDown() {
    trxMgr.rollback();
  }
}
