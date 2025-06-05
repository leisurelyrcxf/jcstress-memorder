package com.vmlens.stresstest.tests.datastructure.concurrent.producerconsumer;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

public class SpscBlockingQueue_No_Dangling {

    /** @noinspection unused*/
    @JCStressTest
    @Outcome(id = {"0"}, expect = ACCEPTABLE, desc = "Gracefully finished")
    @Outcome(expect = FORBIDDEN, desc = "Test is stuck")
    @State
    public static class Progress1 {
        private final MarketDataFeed marketDataFeed;
        private final MarketDataEngine marketDataEngine;
        private volatile boolean ready;

        public Progress1() {
            Queue<Tick> q = new ConcurrentLinkedQueue<>();
            marketDataFeed = new MarketDataFeed(q);
            marketDataEngine = new MarketDataEngine(marketDataFeed);
            ready = false;
        }

        @Actor
        public void producer(I_Result r) {
            while (!ready) {}
            marketDataEngine.produce();
        }

        @Actor
        public void consumer(I_Result r) {
            marketDataFeed.thread = Thread.currentThread();
            ready = true;
            marketDataFeed.consume();
        }

        @Actor
        public void stop(I_Result r) {
            try {
                Thread.sleep(0, 10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            marketDataEngine.stop();
            try {
                marketDataFeed.waitStopped(10);
                marketDataEngine.waitStopped(10);
            } catch (Exception e) {
                r.r1 = -1;
                return;
            }

            if (Math.abs(marketDataFeed.getMax() - marketDataEngine.getPrice()) > 1e-6) {
                r.r1 = -1;
                return;
            }
            if (Math.abs(marketDataFeed.getMin() - 100.0) > 1e-6) {
                r.r1 = -1;
                return;
            }
            if (Math.abs(marketDataFeed.getCurrent() - marketDataEngine.getPrice()) > 1e-6) {
                r.r1 = -1;
                return;
            }
            r.r1 = 0;
        }
    }

}
