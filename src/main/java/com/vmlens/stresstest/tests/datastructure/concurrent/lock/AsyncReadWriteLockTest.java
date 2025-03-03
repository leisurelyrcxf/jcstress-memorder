package com.vmlens.stresstest.tests.datastructure.concurrent.lock;

import com.vmlens.stresstest.datastruct.concurrent.lock.AsyncLock;
import com.vmlens.stresstest.datastruct.concurrent.lock.AsyncReadWriteLock;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IIII_Result;
import org.openjdk.jcstress.infra.results.I_Result;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;


public class AsyncReadWriteLockTest {
    @JCStressTest
    @State
    @Outcome(id = "1, 1, 1, 1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(expect = FORBIDDEN, desc = "Cannot happen")
    public static class TestReaderBlockedByWriter {
        AsyncReadWriteLock lock = new AsyncReadWriteLock(Duration.ofSeconds(15), Duration.ofSeconds(15));
        AsyncLock readLock = lock.getReadLock();
        AsyncLock writeLock = lock.getWriteLock();

        public TestReaderBlockedByWriter() {
            writeLock.lock();
        }

        @Actor
        public void actorReader1(IIII_Result r) {
            r.r1 = readLock.tryLock(5, TimeUnit.SECONDS) ? 1 : 0;
        }

        @Actor
        public void actorReader2(IIII_Result r) {
            r.r2 = readLock.tryLock(5, TimeUnit.SECONDS) ? 1 : 0;
        }

        @Actor
        public void actorReader3(IIII_Result r) {
            r.r3 = readLock.tryLock(5, TimeUnit.SECONDS) ? 1 : 0;
        }

        @Actor
        public void actorReader4(IIII_Result r) {
            r.r4 = readLock.tryLock(5, TimeUnit.SECONDS) ? 1 : 0;
        }

        @Actor
        public void writer(IIII_Result r) {
            writeLock.unlock();
        }
    }

    @JCStressTest
    @State
    @Outcome(id = "1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(expect = FORBIDDEN, desc = "Cannot happen")
    public static class TestWriterBlockedByReader {
        AsyncReadWriteLock lock = new AsyncReadWriteLock(Duration.ofMinutes(1), Duration.ofMinutes(1));
        AsyncLock readLock = lock.getReadLock();
        AsyncLock writeLock = lock.getWriteLock();

        public TestWriterBlockedByReader() {
            readLock.lock();
            readLock.lock();
        }

        @Actor
        public void actorReader1(I_Result r) {
            readLock.unlock();
        }

        @Actor
        public void actorReader2(I_Result r) {
            readLock.unlock();
        }

        @Actor
        public void writer(I_Result r) {
            r.r1 = writeLock.tryLock(5, TimeUnit.SECONDS) ? 1 : 0;
        }
    }
}
