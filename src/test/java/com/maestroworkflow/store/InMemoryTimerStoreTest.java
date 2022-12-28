package com.maestroworkflow.store;

import static org.junit.Assert.*;

import com.maestroworkflow.models.Anything;
import com.maestroworkflow.models.Timer;
import com.maestroworkflow.timers.TimerHandler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import lombok.val;
import org.junit.Test;

public class InMemoryTimerStoreTest {
  @Test
  public void testTimers() throws Exception {
    val clock = Clock.fixed(Instant.ofEpochMilli(1662908612003L), ZoneId.systemDefault());
    val store = new InMemoryTimerStore(clock);
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
    assertEquals(t1, store.get(t1.getTimerId()));
    // Test update
    store.createOrUpdate(t1Mod);
    assertEquals(t1Mod.toBuilder().version(1).build(), store.get(t1.getTimerId()));
    // Test proper update
    store.update(t1.getTimerId(), Duration.ofSeconds(10));
    val expectedNew =
        t1Mod.toBuilder().version(1).timeout(clock.instant().plus(Duration.ofSeconds(10))).build();
    assertEquals(expectedNew, store.get(t1.getTimerId()));
    // Test fetch
    val result = store.getExpiredTimers();
    assertEquals(1, result.size());
    assertEquals(t2, result.get(0));
    // Fetching again should produce no results, since we should've taken a lease on t2
    assertEquals(0, store.getExpiredTimers().size());
    // Verify that the lease for t2 is 30 secs in the future
    val newT2 = store.get(t2.getTimerId());
    val expectedT2 = t2.toBuilder().timeout(clock.instant().plus(Duration.ofSeconds(30))).build();
    assertEquals(expectedT2, newT2);
    // Delete t2
    val updatedT2 = store.createOrUpdate(t2);
    assertEquals(t2.getVersion() + 1, updatedT2.getVersion());
    // Trying to delete a stale timer should fail
    assertFalse(store.delete(t2));
    store.get(t2.getTimerId()); // If this doesn't throw it means the timer wasn't deleted
    // Now delete it for real
    assertTrue(store.delete(updatedT2));
    assertThrows(IllegalArgumentException.class, () -> store.get(t2.getTimerId()));
  }

  @Test
  public void testCoroutine() {
    Thread thread = Thread.currentThread();
    Thread.yield();
  }
}
