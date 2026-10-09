package com.example.concurrency.ex03_visibility;

/**
 * LESSON 3 - Visibility and volatile.
 *
 * Each CPU core has its own cache. A change made by one thread is NOT guaranteed to be seen
 * by another thread, unless you use something that creates a "happens-before" relationship:
 * volatile, synchronized, locks, atomics, thread start/join...
 *
 * Below, a worker loops until a "running" flag becomes false. Will it stop?
 */
public class VisibilityDemo {

    static boolean plainFlag = true;           // NO visibility guarantee
    static volatile boolean volatileFlag = true; // writes are immediately visible to all threads

    public static void main(String[] args) throws InterruptedException {
        // --- Case 1: plain boolean ---
        Thread plainWorker = new Thread(() -> {
            long loops = 0;
            while (plainFlag) {
                loops++; // the JIT compiler is allowed to read plainFlag only once and loop forever
            }
            System.out.println("plain worker stopped after " + loops + " loops");
        });
        plainWorker.setDaemon(true); // daemon so the JVM can exit even if this thread never stops
        plainWorker.start();

        Thread.sleep(500);
        plainFlag = false;
        plainWorker.join(1000);
        System.out.println("plain flag:    worker " + (plainWorker.isAlive()
                ? "is STILL RUNNING (it never saw the change!) - this is the visibility problem"
                : "stopped (you were lucky - this is NOT guaranteed)"));

        // --- Case 2: volatile boolean ---
        Thread volatileWorker = new Thread(() -> {
            long loops = 0;
            while (volatileFlag) {
                loops++;
            }
            System.out.println("volatile worker stopped after " + loops + " loops");
        });
        volatileWorker.start();

        Thread.sleep(500);
        volatileFlag = false;
        volatileWorker.join(1000);
        System.out.println("volatile flag: worker " + (volatileWorker.isAlive() ? "still running" : "stopped - always works"));

        // Remember: volatile gives VISIBILITY, not ATOMICITY. volatile int x; x++ is still a race condition.
    }
}
