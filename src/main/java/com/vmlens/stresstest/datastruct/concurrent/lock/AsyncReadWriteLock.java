package com.vmlens.stresstest.datastruct.concurrent.lock;


import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * A read write lock that allows lock() and unlock() in different threads.
 *
 * @author chenxiaofan
 */
public class AsyncReadWriteLock {

    private static final Duration DEFAULT_READER_PARK_DURATION = Duration.ofMillis(1);
    private static final Duration DEFAULT_WRITER_PARK_DURATION = Duration.ofNanos(1000);
    private static final int TIMEOUT = -1;
    private static final int RETRY = -2;

    @FunctionalInterface
    public interface Clock {
        long getNanos();
    }

    private static final int WRITE_FLAG = 1 << 30; // write lock flag
    private static final int MAX_READERS = WRITE_FLAG - 1; // max allowed readers
    private static final Clock ALWAYS_ZERO_CLOCK = () -> 0L;
    private final long readerParkNs;
    private final long writerParkNs;
    private final AtomicInteger state = new AtomicInteger(0);
    private final AsyncLock readLock = new ReadAsyncLock();
    private final AsyncLock writeLock = new AsyncWriteLock();
    private volatile Thread waitReaderZeroThread = null;
    private final ConcurrentHashMap<Long, Thread> waitWriterUnlockThreads = new ConcurrentHashMap<>();

    public AsyncReadWriteLock() {
        this(DEFAULT_READER_PARK_DURATION, DEFAULT_WRITER_PARK_DURATION);
    }

    public AsyncReadWriteLock(Duration readerParkDuration, Duration writerParkDuration) {
        this.readerParkNs = readerParkDuration.toNanos();
        this.writerParkNs = writerParkDuration.toNanos();
    }

    /**
     * get read lock obj
     */
    public AsyncLock getReadLock() {
        return readLock;
    }

    /**
     * get write lock obj
     */
    public AsyncLock getWriteLock() {
        return writeLock;
    }

    /*
     * If write lock is held, wait for it to be released.
     */
    private int blockUntilWriteUnlocked(long deadlineNs, Clock clock) {
        int s = state.get();
        if (s < 0) {
            throw new IllegalStateException("read lock count negative");
        }

        if ((s & WRITE_FLAG) == 0) {
            return s;
        }

        final Thread currentThread = Thread.currentThread();
        waitWriterUnlockThreads.put(currentThread.getId(), currentThread);
        try {
            s = state.get();
            if (s < 0) {
                throw new IllegalStateException("read lock count negative");
            }

            if (((s & WRITE_FLAG) == 0)) {
                return s;
            }

            long remainedNs = deadlineNs - clock.getNanos();
            if (remainedNs <= 0) {
                return TIMEOUT;
            }
            LockSupport.parkNanos(readerParkNs);
            remainedNs = deadlineNs - clock.getNanos();
            if (remainedNs <= 0) {
                return TIMEOUT;
            }
            return RETRY;
        } finally {
            waitWriterUnlockThreads.remove(currentThread.getId());
        }
    }

    /**
     * Reader asyncLock
     */
    private class ReadAsyncLock implements AsyncLock {
        @Override
        public void lock() {
            tryLock(Long.MAX_VALUE, ALWAYS_ZERO_CLOCK);
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit timeUnit) {
            return tryLock(
                    System.nanoTime() + timeUnit.toNanos(timeout),
                    System::nanoTime
            );
        }

        private boolean tryLock(long deadlineNs, Clock clock) {
            int s;
            do {
                s = blockUntilWriteUnlocked(deadlineNs, clock);
                switch (s) {
                    case TIMEOUT:
                        return false;
                    case RETRY:
                        continue;
                    case MAX_READERS:
                        throw new IllegalStateException("read lock count overflow");
                    default:
                        break;
                }

                // try to increment the reader count
                if (state.compareAndSet(s, s + 1)) {
                    return true;
                }
            } while (true);
        }

        @Override
        public void unlock() {
            int ret = state.decrementAndGet();
            if (ret < 0) {
                throw new IllegalStateException("read lock count negative");
            }
            if (ret == WRITE_FLAG) {
                final Thread t = waitReaderZeroThread;
                if (t != null) {
                    LockSupport.unpark(t);
                }
            }
        }
    }

    /**
     * Writer asyncLock
     */
    private class AsyncWriteLock implements AsyncLock {
        @Override
        public void lock() {
            tryLock(Long.MAX_VALUE, () -> 0L);
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit timeUnit) {
            return tryLock(
                    System.nanoTime() + timeUnit.toNanos(timeout),
                    System::nanoTime
            );
        }

        @SuppressWarnings("java:S135" /* break or continue at most once */)
        private boolean tryLock(long deadlineNs, Clock clock) {
            int s;
            do {
                s = blockUntilWriteUnlocked(deadlineNs, clock);
                switch (s) {
                    case TIMEOUT:
                        return false;
                    case RETRY:
                        continue;
                    case MAX_READERS:
                        throw new IllegalStateException("read lock count overflow");
                    default:
                        break;
                }

                if (state.compareAndSet(s, s + WRITE_FLAG)) {
                    break;
                }
            } while (true);

            waitReaderZeroThread = Thread.currentThread();
            // Wait for all readers to release their locks.
            try {
                do {
                    if (state.get() == WRITE_FLAG) {
                        return true;
                    }

                    long remainedNs = deadlineNs - clock.getNanos();
                    if (remainedNs <= 0) {
                        state.addAndGet(-WRITE_FLAG);
                        return false;
                    }
                    LockSupport.parkNanos(writerParkNs);
                    remainedNs = deadlineNs - clock.getNanos();
                    if (remainedNs <= 0) {
                        state.addAndGet(-WRITE_FLAG);
                        return false;
                    }
                } while (true);
            } finally {
                waitReaderZeroThread = null;
            }
        }

        @Override
        public void unlock() {
            if (!state.compareAndSet(WRITE_FLAG, 0)) {
                throw new IllegalStateException("Current thread does not hold the write lock");
            }
            waitWriterUnlockThreads.forEach((ignoredTid, t) -> LockSupport.unpark(t));
            waitWriterUnlockThreads.clear();
        }
    }

}

