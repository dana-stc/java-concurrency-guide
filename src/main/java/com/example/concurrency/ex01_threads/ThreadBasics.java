package com.example.concurrency.ex01_threads;

/**
 * LESSON 1 - Creating threads.
 *
 * A thread is an independent path of execution inside your program.
 * The JVM starts with one thread ("main"). You can start more, and they run at the same time.
 */
public class ThreadBasics {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Running in: " + Thread.currentThread().getName());

        // Way 1: give a Runnable (a task) to a Thread. This is the recommended way:
        // the TASK (what to do) is separate from the THREAD (who does it).
        Runnable task = () -> {
            for (int i = 1; i <= 3; i++) {
                System.out.println(Thread.currentThread().getName() + " -> step " + i);
                sleep(100);
            }
        };

        Thread t1 = new Thread(task, "worker-1");
        Thread t2 = new Thread(task, "worker-2");

        // start() creates a NEW thread that runs the task.
        // (Calling run() directly would just run it on the main thread - a classic mistake!)
        t1.start();
        t2.start();

        // join() = "wait here until that thread is finished".
        t1.join();
        t2.join();

        System.out.println("Both workers finished. Back in: " + Thread.currentThread().getName());

        // Interrupting: the polite way to ask a thread to stop.
        Thread sleeper = new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                System.out.println("sleeper: I was interrupted, stopping cleanly.");
            }
        }, "sleeper");
        sleeper.start();
        sleep(200);
        sleeper.interrupt(); // does not kill the thread, it only asks it to stop
        sleeper.join();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
