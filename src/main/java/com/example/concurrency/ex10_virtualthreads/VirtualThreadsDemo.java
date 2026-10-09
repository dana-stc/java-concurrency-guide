package com.example.concurrency.ex10_virtualthreads;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * LESSON 10 - Virtual threads (Java 21+).
 *
 * A normal ("platform") thread = one OS thread. Heavy: ~1 MB of memory, only thousands possible.
 * A virtual thread is managed by the JVM, very cheap, and you can have MILLIONS.
 * When a virtual thread blocks (sleep, network, DB), the JVM parks it and lets the
 * underlying OS thread run something else.
 *
 * Use them for code that mostly WAITS (I/O). They do not make CPU-heavy code faster.
 * Do NOT pool them - just create one per task.
 */
public class VirtualThreadsDemo {

    public static void main(String[] args) throws Exception {
        // Simplest way
        Thread vt = Thread.ofVirtual().name("my-virtual-thread").start(() ->
                System.out.println("Hello from " + Thread.currentThread() + " virtual=" + Thread.currentThread().isVirtual()));
        vt.join();

        // 10,000 tasks that each wait 1 second. With virtual threads this takes ~1 second in total.
        long start = System.currentTimeMillis();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 10_000).forEach(i ->
                    executor.submit(() -> {
                        Thread.sleep(Duration.ofSeconds(1));
                        return i;
                    }));
        } // close() waits for all tasks
        System.out.println("10,000 virtual threads finished in " + (System.currentTimeMillis() - start) + " ms");

        // Same job with a fixed pool of 200 platform threads: 10,000 / 200 * 1s = ~50 seconds, so we only do 1,000 tasks
        start = System.currentTimeMillis();
        try (var executor = Executors.newFixedThreadPool(200)) {
            IntStream.range(0, 1_000).forEach(i ->
                    executor.submit(() -> {
                        Thread.sleep(Duration.ofSeconds(1));
                        return i;
                    }));
        }
        System.out.println("1,000 tasks on 200 platform threads took " + (System.currentTimeMillis() - start) + " ms (5 rounds of 1s)");
    }
}
