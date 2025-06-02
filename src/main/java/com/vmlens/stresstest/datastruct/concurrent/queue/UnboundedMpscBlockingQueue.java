package com.vmlens.stresstest.datastruct.concurrent.queue;

import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.MpscUnboundedArrayQueue;

import javax.annotation.Nullable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 多生产者单消费者（MPSC）阻塞队列实现
 *
 * @param <E> 元素类型
 * @author xiaofan
 */
@Slf4j
public class UnboundedMpscBlockingQueue<E> {
    private final MpscUnboundedArrayQueue<E> queue;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public UnboundedMpscBlockingQueue() {
        this.queue = new MpscUnboundedArrayQueue<>(1024);
    }

    /**
     * 将元素放入队列，如果队列是空的，则通知等待的消费者
     *
     * @param e 要放入队列的元素
     * @throws NullPointerException 如果元素为 null
     */
    @SuppressWarnings("java:S2446" /* only one thread needs to be notified */)
    public void put(E e) {
        if (e == null) {
            throw new NullPointerException("Null elements are not allowed");
        }

        queue.offer(e);  // 先入队

        lock.lock();
        try {
            notEmpty.signal();  // 再发出信号
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从队列中取出元素，如果队列为空，则阻塞等待
     *
     * @return 队列中的元素
     * @throws InterruptedException 如果线程在等待过程中被中断
     */
    public E take() throws InterruptedException {
        E e = dequeue();
        if (e != null) {
            return e;
        }

        lock.lockInterruptibly();
        try {
            while ((e = dequeue()) == null) {
                notEmpty.await();
            }
            return e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 尝试从队列中取出元素
     *
     * @return 队列中的元素，或 null 如果队列为空
     */
    private @Nullable E dequeue() {
        return queue.poll();
    }
}
