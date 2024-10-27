package com.vmlens.stressTest.tests.datastructure.concurrent.queue;

import com.vmlens.stresstest.datastruct.MpscBlockingQueue;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Mode;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.Signal;
import org.openjdk.jcstress.annotations.State;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

public class Progress_MpscBlockingQueue {

    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Gracefully finished")
    @Outcome(id = "STALE", expect = FORBIDDEN, desc = "Test is stuck")
    @State
    public static class Progress1 {
        MpscBlockingQueue<Integer> queue = new MpscBlockingQueue<>();

        @Actor
        public void actor1() {
            try {
                queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Signal
        public void signal() {
            queue.put(1);
        }
    }

}
