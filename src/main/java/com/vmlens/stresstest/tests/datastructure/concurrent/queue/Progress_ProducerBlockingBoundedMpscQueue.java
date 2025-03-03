package com.vmlens.stresstest.tests.datastructure.concurrent.queue;

import com.vmlens.stresstest.datastruct.concurrent.queue.blocking.ProducerBlockingBoundedMpscQueue;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.III_Result;

import java.util.concurrent.TimeUnit;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * @author chenxiaofan
 */
public class Progress_ProducerBlockingBoundedMpscQueue {
    @JCStressTest
    @Outcome(id = {"0, 0, 0"}, expect = ACCEPTABLE, desc = "Gracefully finished")
    @Outcome(expect = FORBIDDEN, desc = "Test is stuck")
    @State
    public static class Progress1 {
        private final ProducerBlockingBoundedMpscQueue<Integer> queue = new ProducerBlockingBoundedMpscQueue<>(100);

        public Progress1() {
            for (int i = 0; i < queue.capacity(); i++) {
                if (!queue.offer(i)) {
                    throw new IllegalStateException();
                }
            }
        }

        @Actor
        public void actor1(III_Result r) {
            long start = System.nanoTime();
            try {
                queue.put(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            r.r1 = (int) TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
        }

        @Actor
        public void actor2(III_Result r) {
            long start = System.nanoTime();
            try {
                queue.put(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            r.r2 = (int) TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
        }

        @Actor
        public void actor3(III_Result r) {
            long start = System.nanoTime();
            try {
                queue.put(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            r.r3 = (int) TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
        }

        @Actor
        public void consumer1() {
            for (int i = 0; i < 3; i++) {
                Integer e = queue.poll();
                if (e == null) {
                    throw new IllegalStateException();
                }
            }
        }
    }
}
