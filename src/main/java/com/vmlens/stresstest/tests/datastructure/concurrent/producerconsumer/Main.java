package com.vmlens.stresstest.tests.datastructure.concurrent.producerconsumer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {

    public static class TestCase {
        final MarketDataEngine marketDataEngine;
        final MarketDataFeed marketDataFeed;

        public TestCase() {
            Queue<Tick> q = new ConcurrentLinkedQueue<>();
            marketDataFeed = new MarketDataFeed(q);
            marketDataEngine = new MarketDataEngine(marketDataFeed);
        }

        public void run() {
            marketDataEngine.start();
            marketDataFeed.start();
            try {
                Thread.sleep(0, 10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            marketDataEngine.stop();
            marketDataFeed.waitStopped(10);
            marketDataEngine.waitStopped(10);

            if (Math.abs(marketDataFeed.getMax() - marketDataEngine.getPrice()) > 1e-6) {
                throw new IllegalStateException(String.format("exp: %.2f, got: %.2f", marketDataEngine.getPrice(), marketDataFeed.getMax()));
            }
            if (Math.abs(marketDataFeed.getMin() - 100.0) > 1e-6) {
                throw new IllegalStateException(String.format("min not okay, got: %.2f", marketDataFeed.getMin()));
            }
            if (Math.abs(marketDataFeed.getCurrent() - marketDataEngine.getPrice()) > 1e-6) {
                throw new IllegalStateException();
            }
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10000; i++) {
            final TestCase testCase = new TestCase();
            testCase.run();
        }
    }
}
