package com.vmlens.stresstest.datastruct.concurrent.queue.blocking;


import org.jctools.queues.MpscChunkedArrayQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @param <E>
 * @author chenxiaofan
 */
public class ProducerBlockingBoundedMpscQueue<E> implements BlockingQueue<E> {

    private final MpscChunkedArrayQueue<E> delegate;
    private final Object isNotFull = new Object();
    private final AtomicInteger waitingIsNotFullThreadCount = new AtomicInteger(0);

    public ProducerBlockingBoundedMpscQueue(int cap) {
        this.delegate = new MpscChunkedArrayQueue<>(cap);
    }

    public int capacity() {
        return delegate.capacity();
    }

    @Override
    public void put(E e) throws InterruptedException {
        while (!delegate.offer(e)) {
            synchronized (isNotFull) {
                waitingIsNotFullThreadCount.incrementAndGet();
                // double check after increase of waitingIsNotFullThreadCount
//                if (delegate.offer(e)) {
//                    waitingIsNotFullThreadCount.decrementAndGet();
//                    return;
//                }
                try {
                    isNotFull.wait(2222);
                } finally {
                    waitingIsNotFullThreadCount.decrementAndGet();
                }
            }
        }
    }

    @Override
    @SuppressWarnings("java:S2446")
    public E poll() {
        E e = delegate.poll();
        if (waitingIsNotFullThreadCount.get() > 0) {
            synchronized (isNotFull) {
                if (e == null) {
                    e = delegate.poll();
                    if (e == null) {
                        return null;
                    }
                }
                if (waitingIsNotFullThreadCount.get() > 0) {
                    // Don't use notifyAll to avoid the thundering herd problem.
                    isNotFull.notifyAll();
                }
            }
        }
        return e;
    }

    @Override
    public E peek() {
        return delegate.peek();
    }

    @Override
    public boolean offer(E e) {
        return delegate.offer(e);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E take() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int remainingCapacity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E element() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> ignored) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

}
