package com.vmlens.stresstest.tests.datastructure.concurrent.producerconsumer;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author sadtheslayer
 */
public class MarketDataFeed {
    private final Queue<Tick> q;

    public Queue<Tick> getQ() {
        return q;
    }
    Thread thread;

    private double min = Double.MAX_VALUE;

    public double getMin() {
        return min;
    }
    private double max = 0.0;

    public  double getMax() {
        return max;
    }
    private double current = -1.0;

    public double getCurrent() {
        return current;
    }

    private volatile boolean parked = false;
    private final CountDownLatch stopped = new CountDownLatch(1);

    @SuppressWarnings("AlibabaAvoidManuallyCreateThread")
    public MarketDataFeed(Queue<Tick> q) {
        this.q = q;
        this.thread = new Thread(this::consume, "consumer");
    }

    public void start() {
        this.thread.start();
    }

    void consume() {
outer:
        while (true) {
            Tick tick = q.poll();
            if (tick == null) {
                do {
                    parked = true;
                    try {
                        tick = q.poll();
                        if (tick != null) {
                            break;
                        }
                        // q.poll() == null -> engine[q.offer()] -> engine[feed.isParked() == true] -> engine[feed.unpark]
                        LockSupport.park();
                        continue outer;
                    } finally {
                        parked = false;
                    }
                } while (false);
            }
            if (tick == MarketDataEngine.EOF) {
                break;
            }
            update(tick);
        }
        stopped.countDown();
    }

    void update(Tick tick) {
        final double price = tick.getPrice();
        if (price > max) {
            max = price;
        }
        if (price < min) {
            min = price;
        }
        current = price;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public void unpark() {
        LockSupport.unpark(thread);
    }

    public boolean isParked() {
        return parked;
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
