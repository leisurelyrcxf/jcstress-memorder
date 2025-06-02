package com.vmlens.stresstest.tests.datastructure.concurrent.queue;

import org.jctools.queues.MpscUnboundedArrayQueue;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

public class MpscBlockingQueue_No_Dangling {

    @JCStressTest
    @Outcome(id = {"0"}, expect = ACCEPTABLE, desc = "Gracefully finished")
    @Outcome(expect = FORBIDDEN, desc = "Test is stuck")
    @State
    public static class Progress1 {
        MpscUnboundedArrayQueue<Integer> queue = new MpscUnboundedArrayQueue<>(2);
        MpscUnboundedArrayQueue<Runnable> tasks = new MpscUnboundedArrayQueue<>(2);
        AtomicBoolean lock = new AtomicBoolean();

        volatile boolean stopProducer = false;
        AtomicInteger producerRunning = new AtomicInteger(2);

        @Actor
        public void producer1(I_Result r) {
            try {
                produce(0);
            } finally {
                producerRunning.decrementAndGet();
            }
        }

        @Actor
        public void producer2(I_Result r) {
            try {
                produce(1);
            } finally {
                producerRunning.decrementAndGet();
            }
        }

        private void produce(int i) {
            while (!stopProducer) {
                queue.offer(i);
                i += 2;
                if (lock.compareAndSet(false, true)) {
                    tasks.offer(() -> {
                        while (true) {
                            var ele = queue.poll();
                            if (ele == null) {
                                lock.set(false);
                                while (true) {
                                    ele = queue.poll();
                                    if (ele == null) {
                                        return;
                                    }
                                }
                            }
                        }
                    });
                }
            }
        }

        @Actor
        public void consumer(I_Result r) {
            outer:
            while (true) {
                var task = tasks.poll();
                if (task == null) {
                    if (producerRunning.get() == 0) {
                        while (true) {
                            task = tasks.poll();
                            if (task == null) {
                                break outer;
                            }
                            task.run();
                        }
                    }
                    Thread.yield();
                    continue;
                }
                task.run();
            }
            Integer e = null;
            for (int i = 0; i < 1000; i++) {
                var ee = queue.poll();
                if (ee != null) {
                    e = ee;
                }
            }
            r.r1 = e == null ? 0 : e;
        }

        @Actor
        public void stop(I_Result r) {
            try {
                Thread.sleep(100);
                stopProducer = true;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
