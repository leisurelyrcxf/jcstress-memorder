package com.vmlens.stresstest.tests.datastructure.concurrent.producerconsumer;

import org.openjdk.jcstress.annotations.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.openjdk.jcstress.annotations.Expect.*;

public class SpscBlockingQueue_No_Dangling {

    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Gracefully finished")
    @Outcome(id = "STALE", expect = ACCEPTABLE_INTERESTING, desc = "Test is stuck")
    @State
    public static class Progress1 {
        private final MarketDataFeed marketDataFeed;
        private final MarketDataEngine marketDataEngine;

        public Progress1() {
            Queue<Tick> q = new ConcurrentLinkedQueue<>();
            marketDataFeed = new MarketDataFeed(q);
            marketDataEngine = new MarketDataEngine(marketDataFeed);
        }

        @Actor
        public void consumer() {
            marketDataFeed.thread = Thread.currentThread();
            marketDataEngine.start();
            marketDataFeed.consume();
        }

        @Signal
        public void stop() {
            try {
                Thread.sleep(0, 100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            marketDataEngine.stop();
//            try {
//                marketDataFeed.waitStopped(2);
//                marketDataEngine.waitStopped(2);
//            } catch (Exception e) {
//                throw new IllegalStateException(e);
//            }
//
//            if (Math.abs(marketDataFeed.getMax() - marketDataEngine.getPrice()) > 1e-6) {
//                throw new IllegalStateException();
//            }
//            if (Math.abs(marketDataFeed.getMin() - 100.0) > 1e-6) {
//                throw new IllegalStateException();
//            }
//            if (Math.abs(marketDataFeed.getCurrent() - marketDataEngine.getPrice()) > 1e-6) {
//                throw new IllegalStateException();
//            }
        }
    }

}
