package com.example.concurrency.ex07_completablefuture;

import java.util.concurrent.CompletableFuture;

/**
 * LESSON 7 - CompletableFuture: chain async steps without blocking.
 *
 * Future.get() blocks. CompletableFuture lets you say "when this is done, then do that":
 *   supplyAsync   start async work that returns a value
 *   thenApply     transform the result (like map)
 *   thenCompose   chain another async step (like flatMap)
 *   thenCombine   join two independent async results
 *   exceptionally handle errors
 */
public class CompletableFutureDemo {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        // Two independent slow calls run IN PARALLEL
        CompletableFuture<String> user = CompletableFuture.supplyAsync(() -> slow("user: Ana", 500));
        CompletableFuture<String> orders = CompletableFuture.supplyAsync(() -> slow("3 orders", 500));

        CompletableFuture<String> page = user
                .thenCombine(orders, (u, o) -> u + " has " + o)  // wait for both
                .thenApply(String::toUpperCase);                 // transform

        System.out.println(page.join());
        System.out.println("Took ~" + (System.currentTimeMillis() - start) + " ms (not 1000, because they ran in parallel)");

        // thenCompose: the second call needs the result of the first
        String chained = CompletableFuture
                .supplyAsync(() -> slow("userId=42", 200))
                .thenCompose(id -> CompletableFuture.supplyAsync(() -> slow("profile of " + id, 200)))
                .join();
        System.out.println(chained);

        // Error handling with a default value
        String safe = CompletableFuture
                .supplyAsync(() -> {
                    if (true) throw new IllegalStateException("service down");
                    return "never";
                })
                .exceptionally(ex -> "fallback value (" + ex.getCause().getMessage() + ")")
                .join();
        System.out.println(safe);

        // Wait for many
        var all = CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> slow("a", 100)),
                CompletableFuture.runAsync(() -> slow("b", 100)));
        all.join();
        System.out.println("All done.");
    }

    static String slow(String value, long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return value;
    }
}
