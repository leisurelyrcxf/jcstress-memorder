package com.vmlens.stresstest.datastruct.concurrent.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author chenxiaofan
 */
public class RowLockManagerConcurrentHashMapBased<K> {
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
                m.computeIfPresent(key, (ignoredKey, old) -> old.refCount.get() == 0 ? null : old);
            }
        }

        private RowLock ref() {
            refCount.incrementAndGet();
            return this;
        }
    }

    private final Map<K, RowLock> m = new ConcurrentHashMap<>();

    public Lock getLock(K key) {
        return m.compute(key, (k, old) -> old == null ? new RowLock(key) : old.ref());
    }
}
