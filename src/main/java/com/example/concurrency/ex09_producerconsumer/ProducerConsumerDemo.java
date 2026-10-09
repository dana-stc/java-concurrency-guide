package com.example.concurrency.ex09_producerconsumer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * LESSON 9 - Producer / Consumer with a BlockingQueue.
 *
 * One thread produces work, another consumes it. A BlockingQueue sits in between:
 *  - put() waits if the queue is FULL   (producer slows down)
 *  - take() waits if the queue is EMPTY (consumer sleeps until there is work)
 * No wait()/notify() and no manual locking needed.
 */
public class ProducerConsumerDemo {

    private static final String POISON_PILL = "STOP"; // special message meaning "no more work"

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(3); // small on purpose

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 8; i++) {
                    String job = "job-" + i;
                    queue.put(job); // blocks when the queue already has 3 items
                    System.out.println("produced " + job + "   (queue size " + queue.size() + ")");
                }
                queue.put(POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    String job = queue.take(); // blocks when the queue is empty
                    if (job.equals(POISON_PILL)) {
                        System.out.println("consumer: no more jobs, bye.");
                        return;
                    }
                    System.out.println("   consumed " + job);
                    Thread.sleep(200); // consumer is slower than the producer
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }
}
