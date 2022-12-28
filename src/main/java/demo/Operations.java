package demo;

import java.util.concurrent.ThreadLocalRandom;

public class Operations {
    public boolean credit(String account, Integer amount) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean debit(String account, Integer amount) {
        if (ThreadLocalRandom.current().nextBoolean()) {
            throw new RuntimeException("something went wrong!");
        }
        return true;
    }
}
