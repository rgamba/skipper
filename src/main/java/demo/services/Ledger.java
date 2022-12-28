package demo.services;

import com.google.inject.Singleton;
import demo.Utils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

@Singleton
public class Ledger {
  @Getter private final Map<String, Integer> balances = new HashMap<>();
  private final Map<String, Transaction> transactions = new LinkedHashMap<>();

  public Ledger() {
    balances.put("system", 10000);
  }

  public String deposit(
      @NonNull String userId,
      @NonNull Integer amount,
      @NonNull String concept,
      @NonNull String idempotencyToken) {
    Utils.randomSleep();
    Utils.randomFail();
    if (transactions.containsKey(idempotencyToken)) {
      return idempotencyToken;
    }
    if (!balances.containsKey(userId)) {
      balances.put(userId, 0);
    }
    balances.put(userId, balances.get(userId) + amount);
    transactions.put(idempotencyToken, new Transaction(userId, "deposit", amount));
    return idempotencyToken;
  }

  public String withdraw(
      @NonNull String userId,
      @NonNull Integer amount,
      @NonNull String concept,
      @NonNull String idempotencyToken) {
    Utils.randomSleep();
    Utils.randomFail();
    if (!balances.containsKey(userId)) {
      balances.put(userId, 0);
    }
    if (balances.get(userId) < amount) {
      throw new LedgerError("not enough balance");
    }
    balances.put(userId, balances.get(userId) - amount);
    transactions.put(idempotencyToken, new Transaction(userId, "withdraw", amount));
    return idempotencyToken;
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
