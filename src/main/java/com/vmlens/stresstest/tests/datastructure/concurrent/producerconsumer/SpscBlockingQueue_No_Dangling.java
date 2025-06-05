package com.vmlens.stresstest.tests.datastructure.concurrent.producerconsumer;

import org.jctools.queues.MpscUnboundedArrayQueue;
import org.jctools.queues.SpscUnboundedArrayQueue;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.Queue;

import static org.openjdk.jcstress.annotations.Expect.*;

public class SpscBlockingQueue_No_Dangling {

    @JCStressTest
    @Outcome(id = "0", expect = ACCEPTABLE, desc = "Gracefully finished")
    @Outcome(          expect = ACCEPTABLE_INTERESTING, desc = "Test is stuck")
    @State
    public static class Progress1 {
        private final MarketDataFeed marketDataFeed;
        private final MarketDataEngine marketDataEngine;

        public Progress1() {
            Queue<Tick> q = new SpscUnboundedArrayQueue<>(2);
            marketDataFeed = new MarketDataFeed(q);
            marketDataEngine = new MarketDataEngine(marketDataFeed);
        }

        @Actor
        public void consumer(I_Result r) {
            marketDataFeed.thread = Thread.currentThread();
            marketDataEngine.start();
            marketDataFeed.consume();
        }

        @Actor
        public void stop(I_Result r) {
            try {
                Thread.sleep(0, 100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            marketDataEngine.stop();
            try {
                marketDataFeed.waitStopped(2);
                marketDataEngine.waitStopped(2);
            } catch (Exception e) {
                marketDataFeed.thread.interrupt();
                r.r1 = -1;
                return;
            }

            if (Math.abs(marketDataFeed.getMax() - marketDataEngine.getPrice()) > 1e-6) {
                r.r1 = -2;
                return;
            }
            if (Math.abs(marketDataFeed.getMin() - 100.0) > 1e-6) {
                r.r1 = -3;
                return;
            }
            if (Math.abs(marketDataFeed.getCurrent() - marketDataEngine.getPrice()) > 1e-6) {
                r.r1 = -4;
                return;
            }
            r.r1 = 0;
        }
    }

}
