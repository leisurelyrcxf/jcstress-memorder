package com.vmlens.stressTest.tests.datastructure.concurrent.queue;

import com.vmlens.stressTest.datastruct.MpscBlockingQueue;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Mode;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.Signal;
import org.openjdk.jcstress.annotations.State;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

public class Progress_MpscBlockingQueue {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t BasicJMM_04_Progress[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

        One naively can expect that writes to variables are eventually visible. However, under Java Memory Model,
        this does not apply to plain reads and writes. The usual example is the busy loop in plain field.
        The optimizing compiler is allowed to check the field once, and if it is "false", reduce the rest of
        the loop into "while(true)", infinite version.

        Indeed, running this on just about any platform yields:

              RESULT  SAMPLES     FREQ       EXPECT  DESCRIPTION
               STALE        4   50.00%  Interesting  Test is stuck
          TERMINATED        4   50.00%   Acceptable  Gracefully finished
      */

    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Gracefully finished")
    @Outcome(id = "STALE", expect = ACCEPTABLE_INTERESTING, desc = "Test is stuck")
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
