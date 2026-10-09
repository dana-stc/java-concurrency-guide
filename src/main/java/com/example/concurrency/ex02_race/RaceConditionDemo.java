package com.example.concurrency.ex02_race;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * LESSON 2 - Race conditions and how to fix them.
 *
 * Four threads each add 1 to a counter 100,000 times. The right answer is 400,000.
 * count++ looks like one step but it is THREE: read, add one, write back.
 * Two threads can read the same value and both write back the same result -> lost updates.
 */
public class RaceConditionDemo {

    static final int THREADS = 4;
    static final int INCREMENTS = 100_000;

    // 1) BROKEN: plain int, no protection
    static class UnsafeCounter {
        int count;
        void increment() { count++; }
        int get() { return count; }
    }

    // 2) FIX with synchronized: only one thread at a time can run these methods on the same object
    public static class SynchronizedCounter {
        private int count;
        public synchronized void increment() { count++; }
        public synchronized int get() { return count; }
    }

    // 3) FIX with an atomic class: the read-add-write happens as ONE indivisible step (lock-free)
    public static class AtomicCounter {
        private final AtomicInteger count = new AtomicInteger();
        public void increment() { count.incrementAndGet(); }
        public int get() { return count.get(); }
    }

    // 4) FIX with LongAdder: even faster when MANY threads only count (spreads work over several cells)
    static class AdderCounter {
        private final LongAdder count = new LongAdder();
        void increment() { count.increment(); }
        long get() { return count.sum(); }
    }

    public static void main(String[] args) throws InterruptedException {
        UnsafeCounter unsafe = new UnsafeCounter();
        run("unsafe (broken)", unsafe::increment);
        System.out.println("   result = " + unsafe.get() + "   (expected " + THREADS * INCREMENTS + ")\n");

        SynchronizedCounter sync = new SynchronizedCounter();
        run("synchronized", sync::increment);
        System.out.println("   result = " + sync.get() + "\n");

        AtomicCounter atomic = new AtomicCounter();
        run("AtomicInteger", atomic::increment);
        System.out.println("   result = " + atomic.get() + "\n");

        AdderCounter adder = new AdderCounter();
        run("LongAdder", adder::increment);
        System.out.println("   result = " + adder.get());
    }

    static void run(String name, Runnable increment) throws InterruptedException {
        System.out.println("Counter: " + name);
        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    increment.run();
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }
}
