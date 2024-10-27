package com.vmlens.stressTest.datastruct;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.MpscUnboundedArrayQueue;

/**
 * @author chenxiaofan
 */
@Slf4j
public class MpscBlockingQueue<E> {
    private final MpscUnboundedArrayQueue<E> queue;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public MpscBlockingQueue() {
        this.queue = new MpscUnboundedArrayQueue<>(1024);
    }

    @SuppressWarnings("java:S2446" /* only one thread needs to be notified */)
    public void put(E e) {
        queue.offer(e);

        lock.lock();
        try {
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public E take() throws InterruptedException {
        E e = queue.poll();
        if (e != null) {
            return e;
        }

        lock.lockInterruptibly();
        try {
            while (true) {
                e = queue.poll();
                if (e != null) {
                    return e;
                }
                notEmpty.await();
            }
        } finally {
            lock.unlock();
        }
    }
}

