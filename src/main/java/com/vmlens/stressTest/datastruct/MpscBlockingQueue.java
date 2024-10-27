package com.okx.redis.shift.datastructure;

import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.MpscUnboundedArrayQueue;

/**
 * @author chenxiaofan
 */
@Slf4j
public class MpscBlockingQueue<E> {
    private final Queue<E> queue;
    private final AtomicInteger size;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmptyAndEnabledConsumingOrExceedCap = lock.newCondition();
    private final Consumer<E> consumer;
    private final Thread consumerThread;
    private final int maximumBuffered;
    private volatile boolean enabledConsuming;

    public MpscBlockingQueue(Consumer<E> consumer, int maximumBuffered) {
        this.queue = new MpscUnboundedArrayQueue<>(1024);
        this.size = new AtomicInteger();
        this.consumer = consumer;
        this.consumerThread = new Thread(this::consumeLoop);
        this.consumerThread.setDaemon(true);
        this.consumerThread.setName("MpscQueue-Consumer");
        this.consumerThread.start();
        this.maximumBuffered = maximumBuffered;
    }

    public void enableConsuming() {
        lock.lock();
        try {
            enabledConsuming = true;
            notEmptyAndEnabledConsumingOrExceedCap.signal();
        } finally {
            lock.unlock();
        }
    }

    public void stopConsuming() {
        consumerThread.interrupt();
        log.info("[MpscQueue] Stopping consuming");
    }

    @SuppressWarnings("java:S135" /* at most one break/continue */)
    private void consumeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            final E e;
            try {
                e = poll();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }

            if (e != null) {
                if (exceedCapacity()) {
                    log.warn("[MpscQueue] Exceed maximum buffered size, discarding element: {}", e);
                    continue;
                }
                consumer.accept(e);
            }
        }

        log.warn("[MpscQueue] Consumer thread interrupted, stopping consuming");
    }

    @SuppressWarnings("java:S2446" /* only one thread needs to be notified */)
    public void offer(E e) {
        size.incrementAndGet();
        queue.offer(e);

        lock.lock();
        try {
            notEmptyAndEnabledConsumingOrExceedCap.signal();
        } finally {
            lock.unlock();
        }
    }

    public E poll() throws InterruptedException {
        // Reduce overhead of locking in case the queue is busy.
        if (exceedCapacity()) {
            return dequeue();
        }

        E e;
        if (enabledConsuming) {
            e = dequeue();
            if (e != null) {
                return e;
            }
        }

        lock.lockInterruptibly();
        try {
            boolean timeouted = false;
            while (true) {
                if (exceedCapacity()) {
                    return dequeue();
                }

                if (enabledConsuming) {
                    e = dequeue();
                    if (e != null) {
                        return e;
                    }
                    if (timeouted) {
                        log.info("[MpscQueue] no elements in last second");
                    }
                } else if (timeouted) {
                    log.info("[MpscQueue] consumer not enabled");
                }
                timeouted = !notEmptyAndEnabledConsumingOrExceedCap.await(100, TimeUnit.MILLISECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    private @Nullable E dequeue() {
        E e = queue.poll();
        if (e != null) {
            size.decrementAndGet();
        }
        return e;
    }

    public boolean exceedCapacity() {
        return size.get() > maximumBuffered;
    }
}

