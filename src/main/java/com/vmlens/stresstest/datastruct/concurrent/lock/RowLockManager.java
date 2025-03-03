package com.vmlens.stresstest.datastruct.concurrent.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author chenxiaofan
 */
public class RowLockManager<K> {
    public interface Lock {
        void lock();

        void unlock();
    }

    public class RowLock implements Lock {
        final K key;
        final AtomicInteger refCount;
        final ReentrantLock lock;

        public RowLock(K key) {
            this.key = key;
            refCount = new AtomicInteger(1);
            lock = new ReentrantLock();
        }

        @Override
        public void lock() {
            lock.lock();
        }

        @Override
        public void unlock() {
            lock.unlock();
            if (refCount.decrementAndGet() == 0) {
                synchronized (RowLockManager.this) {
                    if (refCount.get() == 0) {
                        m.remove(key, this);
                    }
                }
            }
        }
    }

    private final Map<K, RowLock> m = new HashMap<>();

    public Lock getLock(K key) {
        synchronized (this) {
            return m.compute(key, (k, old) -> {
                if (old == null) {
                    return new RowLock(key);
                }
                old.refCount.incrementAndGet();
                return old;
            });
        }
    }
}
