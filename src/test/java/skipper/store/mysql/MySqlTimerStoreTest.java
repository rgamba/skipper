package skipper.store.mysql;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import lombok.val;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import skipper.common.Anything;
import skipper.models.Timer;
import skipper.store.SqlTestHelper;
import skipper.timers.TimerHandler;

public class MySqlTimerStoreTest extends SqlTestHelper {
  private MySqlTimerStore store;
  private final Clock clock =
      Clock.fixed(Instant.ofEpochMilli(1662908612003L), ZoneId.systemDefault());

  @Before
  public void setUp() {
    super.setUp();
    store = new MySqlTimerStore(trxMgr, clock);
  }

  @Test
  public void testUpsert() {
    val timer =
        Timer.builder()
            .timerId("timer123")
            .timeout(Instant.now())
            .retries(0)
            .version(0)
            .handlerClazz(TimerHandler.class)
            .payload(Anything.of("test123"))
            .build();
    store.createOrUpdate(timer);
    Timer storedTimer = store.get(timer.getTimerId());
    assertEquals(timer, storedTimer);
    // Now try upserting
    val newTimer = timer.toBuilder().timeout(null).retries(1).build();
    store.createOrUpdate(newTimer);
    // Now check that the persisted value indeed changed
    Timer newStoredTimer = store.get(newTimer.getTimerId());
    // The version should've been updated before persisting
    assertEquals(newTimer.toBuilder().version(1).build(), newStoredTimer);
    // Try getting an invalid timer
    val error = assertThrows(IllegalArgumentException.class, () -> store.get("invalidId"));
    assertTrue(error.getMessage().contains("unable to find timer with id invalidId"));
    // Now delete the timer... first try to delete a stale timer
    assertFalse(store.delete(storedTimer));
    store.get(storedTimer.getTimerId()); // This should be good because the timer was not deleted
    // Now try to actually delete a current timer
    store.delete(newStoredTimer);
    assertThrows(IllegalArgumentException.class, () -> store.get(newStoredTimer.getTimerId()));
  }

  @Test
  public void testFetchTimers() {
    val t1 =
        Timer.builder()
            .handlerClazz(TimerHandler.class)
            .timerId("t1")
            .timeout(clock.instant().plus(Duration.ofSeconds(10)))
            .payload(new Anything(String.class, "payload1"))
            .build();
    val t1Mod = t1.toBuilder().payload(new Anything(String.class, "payload_mod")).build();
    val t2 =
        Timer.builder()
            .handlerClazz(TimerHandler.class)
            .timerId("t2")
            .timeout(clock.instant().minus(Duration.ofSeconds(10)))
            .payload(new Anything(String.class, "payload2"))
            .build();
    // Test create
    store.createOrUpdate(t1);
    store.createOrUpdate(t2);

    // Test fetch
    val result = store.getExpiredTimers();
    assertEquals(1, result.size());
    assertEquals(t2, result.get(0));
    // Fetching again should produce no results, since we should've taken a lease on t2
    assertEquals(0, store.getExpiredTimers().size());
    // Verify that the lease for t2 is 30 secs in the future
    val newT2 = store.get(t2.getTimerId());
    val expectedT2 = t2.toBuilder().timeout(clock.instant().plus(Duration.ofSeconds(30))).build();
    Assert.assertEquals(expectedT2, newT2);
  }
}
