# Java Concurrency Guide (for beginners)

A hands-on introduction to **multithreading and concurrency in modern Java (21+)**.
Each lesson is one small, runnable class with comments. Read the explanation here, then run the code and watch what happens.

No frameworks, no database: just plain Java.

---

## Table of contents
1. [The big picture](#1-the-big-picture)
2. [How to run the examples](#2-how-to-run-the-examples)
3. [Lessons](#3-lessons)
4. [Cheat sheet: which tool for which problem?](#4-cheat-sheet-which-tool-for-which-problem)
5. [Common mistakes](#5-common-mistakes)
6. [Where to go next](#6-where-to-go-next)

---

## 1. The big picture

**Process vs thread.** A *process* is a running program (your Java app). A *thread* is a worker inside that process.
All threads of a process **share the same memory**. That is what makes threads powerful, and also what makes them dangerous.

**Concurrency vs parallelism.**
- *Concurrency*: dealing with many things at once (a cook switching between several pots).
- *Parallelism*: really doing many things at the same time (several cooks, several CPU cores).

**Why use threads?**
- Do work **in parallel** to finish faster (CPU-heavy work on many cores).
- **Don't wait idle**: while one thread waits for a database or network, others keep working.
- Keep an app **responsive** (a UI or a web server handling many users).

**Why is it hard?** Three families of problems cause almost all bugs:

| Problem | In one sentence | Lesson |
|---|---|---|
| **Race condition** | Two threads change the same data at the same time and results get lost. | 2 |
| **Visibility** | One thread changes a value but another thread keeps seeing the old one. | 3 |
| **Deadlock** | Threads wait for each other forever. | 5 |

Everything else in this guide is a tool to avoid those three.

---

## 2. How to run the examples

**Requirements:** Java 21 or newer, and Maven.

```bash
mvn compile
java -cp target/classes com.example.concurrency.ex01_threads.ThreadBasics
```

Replace the class name to run another lesson (see the table below). Run each one **several times**:
concurrent programs can behave differently on every run, and that is part of the lesson.

Run the tests:
```bash
mvn test
```

---

## 3. Lessons

| # | Class | Topic |
|---|---|---|
| 1 | `ex01_threads.ThreadBasics` | Creating, starting, joining and interrupting threads |
| 2 | `ex02_race.RaceConditionDemo` | Race conditions, `synchronized`, atomics |
| 3 | `ex03_visibility.VisibilityDemo` | Visibility and `volatile` |
| 4 | `ex04_locks.LocksDemo` | `ReentrantLock`, `tryLock`, `ReadWriteLock` |
| 5 | `ex05_deadlock.DeadlockDemo` | Deadlock, detecting and avoiding it |
| 6 | `ex06_executors.ExecutorsDemo` | Thread pools, `Callable`, `Future` |
| 7 | `ex07_completablefuture.CompletableFutureDemo` | Chaining async work |
| 8 | `ex08_synchronizers.SynchronizersDemo` | `CountDownLatch`, `Semaphore`, `ConcurrentHashMap` |
| 9 | `ex09_producerconsumer.ProducerConsumerDemo` | `BlockingQueue` |
| 10 | `ex10_virtualthreads.VirtualThreadsDemo` | Virtual threads (Java 21) |

All classes are in the package `com.example.concurrency`.

### Lesson 1 - Threads
```java
Thread t = new Thread(() -> System.out.println("hello from " + Thread.currentThread().getName()));
t.start();   // starts a NEW thread
t.join();    // wait until it finishes
```
- `start()` runs the code in a new thread. `run()` just calls the method on the current thread (common mistake).
- `join()` waits for a thread to end.
- `interrupt()` is a polite request to stop. The thread must check for it (blocking methods like `sleep` throw `InterruptedException`).
- Separate the **task** (`Runnable`: what to do) from the **thread** (who does it).

### Lesson 2 - Race conditions
`count++` looks like one step but is three: **read, add, write**. Two threads can read the same value and one update is lost.
The demo runs 4 threads x 100,000 increments. The expected result is 400,000, but the unsafe counter gives a smaller random number.

Fixes, from simplest to most specialized:
- **`synchronized`**: only one thread at a time can enter the block/method for the same object (a "lock").
- **`AtomicInteger` / `AtomicLong`**: do read-modify-write as one indivisible step, without a lock.
- **`LongAdder`**: faster than atomics when many threads only *count*.

> Rule of thumb: any data that is **shared** and **changes** needs protection. Data that is not shared, or never changes, is safe.

### Lesson 3 - Visibility and `volatile`
Each CPU core has its own cache and the compiler may reorder or optimize code. So a write from thread A may not be seen by thread B.
In the demo, a worker loops `while (flag)`. With a plain `boolean`, it may **never stop** even after another thread sets `flag = false`.

- `volatile` guarantees that writes are visible to other threads (and prevents some reordering).
- `volatile` does **not** make `x++` safe. It gives *visibility*, not *atomicity*.
- `synchronized`, locks, atomics, and `Thread.start()/join()` also guarantee visibility.

This guarantee is called **happens-before**: if action A happens-before action B, B sees everything A did.

### Lesson 4 - Locks
`java.util.concurrent.locks` gives more control than `synchronized`:
```java
lock.lock();
try {
    // critical section
} finally {
    lock.unlock();   // ALWAYS in finally
}
```
- `tryLock(timeout)` lets you give up instead of waiting forever.
- `ReadWriteLock`: many readers can read together, but a writer needs exclusive access. Great for data that is read a lot and changed rarely.

### Lesson 5 - Deadlock
```
Thread A: holds lock 1, wants lock 2
Thread B: holds lock 2, wants lock 1   -> both wait forever
```
The demo creates a real deadlock, then asks the JVM (`ThreadMXBean.findDeadlockedThreads`) to find it.
**How to avoid it:**
- Always take locks in the **same order**.
- Hold locks for as little time as possible.
- Use `tryLock` with a timeout.
- Prefer higher-level tools (queues, executors) over manual locking.

In a real application, take a **thread dump** (`jstack <pid>`) and it will print "Found one Java-level deadlock".

### Lesson 6 - Thread pools (`ExecutorService`)
Creating a thread is expensive. A pool keeps a few threads alive and gives them tasks from a queue.
```java
try (ExecutorService pool = Executors.newFixedThreadPool(3)) {
    Future<Integer> f = pool.submit(() -> 6 * 7);   // Callable returns a value
    System.out.println(f.get());                    // blocks until ready
}
```
- `Runnable` returns nothing; `Callable` returns a value or throws.
- `Future` = a ticket for a result that will exist later. `get()` waits; `get(timeout)` gives up; `cancel()` stops.
- Errors inside a task come back wrapped in `ExecutionException`.
- Since Java 19 `ExecutorService` is `AutoCloseable`: `try-with-resources` waits for tasks and shuts it down. **Always shut pools down**, or your JVM may never exit.
- `ScheduledExecutorService` runs tasks later or repeatedly.

**Pool size tip:** CPU-heavy work -> about the number of cores. Waiting (I/O) work -> more threads, or use virtual threads (lesson 10).

### Lesson 7 - `CompletableFuture`
`Future.get()` blocks. `CompletableFuture` lets you describe *what happens next*:

| Method | Meaning | Think of it as |
|---|---|---|
| `supplyAsync(...)` | start async work that returns a value | start |
| `thenApply(f)` | transform the result | `map` |
| `thenCompose(f)` | next step is itself async | `flatMap` |
| `thenCombine(other, f)` | wait for two independent results | join |
| `exceptionally(f)` | recover from an error | catch |
| `allOf(...)` | wait for many | wait all |

The demo calls two 500 ms services in parallel and gets the result in about 500 ms instead of 1000 ms.

### Lesson 8 - Synchronizers and concurrent collections
- **`CountDownLatch`**: "wait until N things have happened". One-time use.
- **`Semaphore`**: "at most N threads at once" (e.g. limit connections to a service).
- **`ConcurrentHashMap`**: thread-safe map. Use atomic methods like `merge`, `compute`, `putIfAbsent`.
  `if (!map.containsKey(k)) map.put(k, v)` is **not** atomic, even on a concurrent map.
- Other useful ones: `CopyOnWriteArrayList` (many reads, few writes), `ConcurrentLinkedQueue`.
- Never share a plain `HashMap` / `ArrayList` between threads that write to them.

### Lesson 9 - Producer / consumer
One part produces work, another consumes it. A `BlockingQueue` between them handles all waiting:
- `put()` waits when the queue is **full** (the producer is slowed down: *back-pressure*).
- `take()` waits when the queue is **empty** (the consumer sleeps).

This pattern decouples fast and slow parts of a system. The demo ends the consumer with a "poison pill" message.

### Lesson 10 - Virtual threads (Java 21)
| | Platform thread | Virtual thread |
|---|---|---|
| Backed by | one OS thread | managed by the JVM |
| Cost | heavy (~1 MB) | tiny (a few KB) |
| How many | thousands | millions |
| Created with | `new Thread(...)` | `Thread.ofVirtual().start(...)` / `Executors.newVirtualThreadPerTaskExecutor()` |

When a virtual thread blocks (sleep, network call, DB call), the JVM parks it and uses the OS thread for something else.
So you can write simple **"one thread per request" blocking code** and still scale.

In the demo, 10,000 tasks that each sleep 1 second finish in about **1 second** with virtual threads.
200 pooled platform threads need 5 seconds for just 1,000 of those tasks.

Rules:
- Use them for **waiting** (I/O) work. They do not speed up CPU-heavy work.
- **Don't pool them.** Create one per task; they are cheap.
- Avoid long `synchronized` blocks around blocking calls (can "pin" the carrier thread); prefer `ReentrantLock`.

---

## 4. Cheat sheet: which tool for which problem?

| I need to... | Use |
|---|---|
| Run something in the background | `ExecutorService` (or virtual threads) |
| Get a result later | `Future` / `CompletableFuture` |
| Combine several async calls | `CompletableFuture.thenCombine / allOf` |
| Protect a shared counter | `AtomicInteger` / `LongAdder` |
| Protect a block of code | `synchronized` or `ReentrantLock` |
| Many readers, few writers | `ReadWriteLock` |
| Share a flag between threads | `volatile` (or an atomic) |
| Share a map | `ConcurrentHashMap` |
| Pass work between threads | `BlockingQueue` |
| Wait for N events | `CountDownLatch` |
| Limit concurrent access | `Semaphore` |
| Handle huge numbers of blocking tasks | Virtual threads |

## 5. Common mistakes

1. Calling `run()` instead of `start()`.
2. Forgetting `unlock()` in a `finally` block.
3. Forgetting to shut down an `ExecutorService`.
4. Assuming `volatile` makes `count++` safe.
5. Swallowing `InterruptedException`. At least restore the flag: `Thread.currentThread().interrupt()`.
6. Using `HashMap`/`ArrayList` from several threads.
7. Check-then-act on a concurrent collection (`containsKey` then `put`).
8. Taking locks in different orders (deadlock).
9. Doing slow work (network, disk) while holding a lock.
10. Creating a new thread for every tiny task instead of using a pool or virtual threads.

**Golden rules:** prefer *no shared state* (immutable objects, pass copies), then high-level tools (executors, queues, concurrent collections), and only then manual locks.

## 6. Where to go next
- *Java Concurrency in Practice* by Brian Goetz (the classic book).
- `ForkJoinPool` and parallel streams for splitting CPU-heavy work.
- Structured concurrency and scoped values (new in recent Java versions, still evolving).
- Tools: `jstack`, `jcmd Thread.print`, VisualVM, and Java Flight Recorder to inspect threads.

## Project structure
```
src/main/java/com/example/concurrency
├── ex01_threads/            ThreadBasics
├── ex02_race/               RaceConditionDemo
├── ex03_visibility/         VisibilityDemo
├── ex04_locks/              LocksDemo
├── ex05_deadlock/           DeadlockDemo
├── ex06_executors/          ExecutorsDemo
├── ex07_completablefuture/  CompletableFutureDemo
├── ex08_synchronizers/      SynchronizersDemo
├── ex09_producerconsumer/   ProducerConsumerDemo
└── ex10_virtualthreads/     VirtualThreadsDemo
src/test/java/.../CounterTest.java   proves the safe counters never lose updates
```
