package com.example.concurrency.ex04_locks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.TimeUnit;

/**
 * LESSON 4 - Explicit locks.
 *
 * synchronized is simple. java.util.concurrent.locks gives you more control:
 *  - tryLock(): do not wait forever
 *  - ReadWriteLock: many readers OR one writer
 */
public class LocksDemo {

    // ---------- ReentrantLock ----------
    static class BankAccount {
        private final Lock lock = new ReentrantLock();
        private int balance = 100;

        void withdraw(int amount) {
            lock.lock();           // ALWAYS unlock in finally, otherwise an exception leaves it locked forever
            try {
                if (balance >= amount) {
                    balance -= amount;
                }
            } finally {
                lock.unlock();
            }
        }

        /** Gives up if the lock is not free after 100 ms instead of blocking forever. */
        boolean tryWithdraw(int amount) throws InterruptedException {
            if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    balance -= amount;
                    return true;
                } finally {
                    lock.unlock();
                }
            }
            return false;
        }

        int balance() { return balance; }
    }

    // ---------- ReadWriteLock ----------
    static class Cache {
        private final ReadWriteLock rw = new ReentrantReadWriteLock();
        private String value = "v1";

        String read() {
            rw.readLock().lock();      // many threads can hold the read lock together
            try {
                return value;
            } finally {
                rw.readLock().unlock();
            }
        }

        void write(String newValue) {
            rw.writeLock().lock();     // exclusive: waits for all readers to leave
            try {
                value = newValue;
            } finally {
                rw.writeLock().unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        BankAccount account = new BankAccount();
        Thread a = new Thread(() -> { for (int i = 0; i < 5; i++) account.withdraw(10); });
        Thread b = new Thread(() -> { for (int i = 0; i < 5; i++) account.withdraw(10); });
        a.start(); b.start(); a.join(); b.join();
        System.out.println("Balance after 10 withdrawals of 10: " + account.balance() + " (expected 0)");

        // tryLock demo: another thread holds the lock, so we give up after 100 ms
        account.lock.lock();
        Thread t = new Thread(() -> {
            try {
                System.out.println("tryWithdraw while lock is busy -> " + account.tryWithdraw(5));
            } catch (InterruptedException ignored) { }
        });
        t.start(); t.join();
        account.lock.unlock();

        Cache cache = new Cache();
        cache.write("v2");
        System.out.println("Cache value: " + cache.read());
    }
}
