package com.example.concurrency.ex06_executors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * LESSON 6 - Thread pools (ExecutorService) and Future.
 *
 * Creating a thread per task is expensive. A thread POOL keeps a fixed number of threads alive
 * and feeds them tasks from a queue.
 *   Runnable -> returns nothing
 *   Callable -> returns a result (or throws)
 *   Future   -> a "ticket" for a result that will be ready later
 */
public class ExecutorsDemo {

    public static void main(String[] args) throws Exception {
        // try-with-resources works since Java 19: close() waits for tasks, then shuts the pool down
        try (ExecutorService pool = Executors.newFixedThreadPool(3)) {

            // 1) submit a Callable, get a Future
            Future<Integer> future = pool.submit(() -> {
                Thread.sleep(500);
                return 6 * 7;
            });
            System.out.println("Doing other work while the task runs...");
            System.out.println("Result: " + future.get()); // get() blocks until the result is ready

            // 2) run many tasks and collect all results
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                int id = i;
                tasks.add(() -> {
                    Thread.sleep(200);
                    return "task-" + id + " done by " + Thread.currentThread().getName();
                });
            }
            for (Future<String> f : pool.invokeAll(tasks)) {
                System.out.println(f.get());
            }
            // Only 3 threads exist, so 5 tasks share them.

            // 3) exceptions come back wrapped in ExecutionException
            Future<Object> failing = pool.submit(() -> { throw new IllegalStateException("boom"); });
            try {
                failing.get();
            } catch (ExecutionException e) {
                System.out.println("Task failed with: " + e.getCause());
            }

            // 4) timeouts
            Future<String> slow = pool.submit(() -> { Thread.sleep(5000); return "late"; });
            try {
                slow.get(300, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                System.out.println("Too slow, cancelling the task");
                slow.cancel(true); // interrupts the worker
            }
        }
        System.out.println("Pool closed.");

        // 5) scheduled tasks
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> ticker = scheduler.scheduleAtFixedRate(
                () -> System.out.println("tick"), 0, 200, TimeUnit.MILLISECONDS);
        Thread.sleep(700);
        ticker.cancel(false);
        scheduler.shutdown();
    }
}
