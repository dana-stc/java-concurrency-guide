package com.example.concurrency.ex05_deadlock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * LESSON 5 - Deadlock.
 *
 * Thread A holds lock 1 and waits for lock 2.
 * Thread B holds lock 2 and waits for lock 1.
 * Both wait forever.
 *
 * FIX: every thread must take locks in the SAME ORDER.
 */
public class DeadlockDemo {

    static final Object LOCK_1 = new Object();
    static final Object LOCK_2 = new Object();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Part 1: a real deadlock ===");
        Thread a = new Thread(() -> takeBoth(LOCK_1, LOCK_2), "thread-A");
        Thread b = new Thread(() -> takeBoth(LOCK_2, LOCK_1), "thread-B"); // opposite order!
        a.setDaemon(true);
        b.setDaemon(true);
        a.start();
        b.start();

        Thread.sleep(1000);

        // The JVM can detect deadlocks for you
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] ids = bean.findDeadlockedThreads();
        System.out.println(ids == null ? "No deadlock found" : "DEADLOCK detected between " + ids.length + " threads:");
        if (ids != null) {
            for (var info : bean.getThreadInfo(ids)) {
                System.out.println("  " + info.getThreadName() + " is waiting for a lock held by " + info.getLockOwnerName());
            }
        }

        // (LOCK_1 and LOCK_2 are stuck forever, so Part 2 uses two fresh locks)
        System.out.println("\n=== Part 2: fixed with the same lock order ===");
        Object lock3 = new Object();
        Object lock4 = new Object();
        Thread c = new Thread(() -> takeBoth(lock3, lock4), "thread-C");
        Thread d = new Thread(() -> takeBoth(lock3, lock4), "thread-D"); // same order
        c.start();
        d.start();
        c.join(2000);
        d.join(2000);
        System.out.println(c.isAlive() || d.isAlive() ? "still stuck" : "Both finished - no deadlock.");
    }

    static void takeBoth(Object first, Object second) {
        synchronized (first) {
            pause(100); // gives the other thread time to grab its first lock
            synchronized (second) {
                System.out.println(Thread.currentThread().getName() + " got both locks");
            }
        }
    }

    static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
