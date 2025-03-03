package com.vmlens.stresstest.tests.datastructure.concurrent.queue;

import com.vmlens.stresstest.datastruct.concurrent.queue.BoundedMpscBlockingQueue;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.concurrent.TimeUnit;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

public class Progress_MpscBlockingQueue {

    @JCStressTest
    @Outcome(id = {"0,0,0"}, expect = ACCEPTABLE, desc = "Gracefully finished")
    @Outcome(expect = FORBIDDEN, desc = "Test is stuck")
    @State
    public static class Progress1 {
        BoundedMpscBlockingQueue<Integer> queue = new BoundedMpscBlockingQueue<>();

        @Actor
        public void actor1(I_Result r) {
            long start = System.nanoTime();
            try {
                queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            r.r1 = (int) TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
        }

        @Actor // use signal is incorret
        public void signal() {
            queue.put(1);
        }
    }

}
