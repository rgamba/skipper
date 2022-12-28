package demo.services;

import com.google.inject.Singleton;
import demo.Utils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

@Singleton
public class Ledger {
  @Getter private final Map<String, Integer> balances = new HashMap<>();
  private final Map<String, Transaction> transactions = new LinkedHashMap<>();
  ReentrantLock lock = new ReentrantLock();

  public Ledger() {
    balances.put("system", 100000);
  }

  public String deposit(
      @NonNull String userId,
      @NonNull Integer amount,
      @NonNull String concept,
      @NonNull String idempotencyToken) {
    Utils.randomSleep();
    // Utils.randomFail();
    try {
      lock.lock();
      if (transactions.containsKey(idempotencyToken)) {
        return idempotencyToken;
      }
      if (!balances.containsKey(userId)) {
        balances.put(userId, 0);
      }
      balances.put(userId, balances.get(userId) + amount);
      transactions.put(idempotencyToken, new Transaction(userId, "deposit", amount));
      return idempotencyToken;
    } finally {
      lock.unlock();
    }
  }

  public String withdraw(
      @NonNull String userId,
      @NonNull Integer amount,
      @NonNull String concept,
      @NonNull String idempotencyToken) {
    Utils.randomSleep();
    // Utils.randomFail();
    try {
      lock.lock();
      if (!balances.containsKey(userId)) {
        balances.put(userId, 0);
      }
      int balance = balances.get(userId);
      if (balance < amount) {
        throw new LedgerError("not enough balance");
      }
      balances.put(userId, balance - amount);
      transactions.put(idempotencyToken, new Transaction(userId, "withdraw", amount));
      return idempotencyToken;
    } finally {
      lock.unlock();
    }
  }

  public Transaction getTransaction(@NonNull String id) {
    return transactions.get(id);
  }

  @Value
  public static class Transaction {
    @NonNull String userId;
    @NonNull String operation;
    int amount;
  }
}
