package com.example.concurrency.ex08_synchronizers;

import java.util.Map;
import java.util.concurrent.*;

/**
 * LESSON 8 - Coordination tools and concurrent collections.
 *
 *  CountDownLatch      "wait until N things have happened" (one-time)
 *  Semaphore           "at most N threads at the same time"
 *  ConcurrentHashMap   a thread-safe map (do not use HashMap from several threads!)
 */
public class SynchronizersDemo {

    public static void main(String[] args) throws Exception {
        countDownLatch();
        semaphore();
        concurrentHashMap();
    }

    static void countDownLatch() throws Exception {
        System.out.println("--- CountDownLatch ---");
        CountDownLatch ready = new CountDownLatch(3);
        for (int i = 1; i <= 3; i++) {
            int id = i;
            new Thread(() -> {
                sleep(100L * id);
                System.out.println("service-" + id + " started");
                ready.countDown(); // one less to wait for
            }).start();
        }
        ready.await(); // main waits until the count reaches 0
        System.out.println("All services started, application is ready.\n");
    }

    static void semaphore() throws Exception {
        System.out.println("--- Semaphore (max 2 at once) ---");
        Semaphore permits = new Semaphore(2);
        try (ExecutorService pool = Executors.newFixedThreadPool(5)) {
            for (int i = 1; i <= 5; i++) {
                int id = i;
                pool.submit(() -> {
                    try {
                        permits.acquire(); // waits if 2 threads are already inside
                        try {
                            System.out.println("client-" + id + " is using the resource ("
                                    + (2 - permits.availablePermits()) + "/2 in use)");
                            sleep(300);
                        } finally {
                            permits.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
        System.out.println();
    }

    static void concurrentHashMap() throws Exception {
        System.out.println("--- ConcurrentHashMap ---");
        Map<String, Integer> wordCount = new ConcurrentHashMap<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            for (int t = 0; t < 4; t++) {
                pool.submit(() -> {
                    for (int i = 0; i < 1000; i++) {
                        // merge() is atomic. "get then put" would be a race condition.
                        wordCount.merge("java", 1, Integer::sum);
                    }
                });
            }
        }
        System.out.println("java -> " + wordCount.get("java") + " (expected 4000)");
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
