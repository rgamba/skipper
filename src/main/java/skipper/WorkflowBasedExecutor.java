package skipper;

import java.time.Instant;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class WorkflowBasedExecutor {
  private final ConcurrentHashMap<String, ExecutorService> executorMap = new ConcurrentHashMap<>();
  private final PriorityQueue<QueueElement> lastActivityQueue = new PriorityQueue<>();

  private static class QueueElement {
    String key;
    Instant lastActivity;
  }
}
