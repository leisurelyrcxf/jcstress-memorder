package com.vmlens.stresstest.tests.datastructure.concurrent.producerconsumer;

import java.lang.invoke.VarHandle;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author sadtheslayer
 */
public class MarketDataEngine {
    public static final Tick EOF = new Tick("", -1);
    private final Queue<Tick> q;
    private final Thread thread;
    private final MarketDataFeed feed;
    private final CountDownLatch stopped = new CountDownLatch(1);
    private volatile boolean shouldStop;
    private double price;

    public double getPrice() {
        return price;
    }

    @SuppressWarnings("AlibabaAvoidManuallyCreateThread")
    public MarketDataEngine(MarketDataFeed feed) {
        this.q = feed.getQ();
        this.thread = new Thread(this::produce, "producer");
        this.feed = feed;
    }

    public void start() {
        this.thread.start();
    }

    void produce() {
        do {
            offerTick(createTick());
        } while (!shouldStop);
        offerTick(EOF);
        stopped.countDown();
    }

    private void offerTick(Tick tick) {
        q.offer(tick);
        if (!(q instanceof ConcurrentLinkedQueue<Tick>)) {
            VarHandle.fullFence();
        }
        if (feed.isParked()) {
            feed.unpark();
        } else {
            // Engine[isParked() == false] -> Feed[parked = true] -> Feed[q.poll() != null] -> Feed[won't park]
        }
    }

    private Tick createTick() {
        price += 100.0;
        return new Tick("btc-usdt", price);
    }

    public void stop() {
        shouldStop = true;
    }

    public void waitStopped(long timeoutInSeconds) {
        try {
            if (!stopped.await(timeoutInSeconds, TimeUnit.SECONDS)) {
                throw new RuntimeException("failed to stop");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
