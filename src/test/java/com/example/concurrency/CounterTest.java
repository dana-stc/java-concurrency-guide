package com.example.concurrency;

import com.example.concurrency.ex02_race.RaceConditionDemo.AtomicCounter;
import com.example.concurrency.ex02_race.RaceConditionDemo.SynchronizedCounter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CounterTest {

    private static void hammer(Runnable increment) {
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (int t = 0; t < 8; t++) {
                pool.submit(() -> {
                    for (int i = 0; i < 50_000; i++) increment.run();
                });
            }
        }
    }

    @Test
    void synchronizedCounterNeverLosesUpdates() {
        SynchronizedCounter c = new SynchronizedCounter();
        hammer(c::increment);
        assertEquals(400_000, c.get());
    }

    @Test
    void atomicCounterNeverLosesUpdates() {
        AtomicCounter c = new AtomicCounter();
        hammer(c::increment);
        assertEquals(400_000, c.get());
    }
}
