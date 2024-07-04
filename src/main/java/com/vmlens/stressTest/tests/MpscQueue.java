package com.vmlens.stressTest.tests;/*
 * Copyright (c) 2016, 2021, Red Hat, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

public class MpscQueue {

    public static class HasOngoingSendLoop {

        final AtomicBoolean safe;

        boolean unsafe;

        public HasOngoingSendLoop() {
            this(false);
        }

        private HasOngoingSendLoop(boolean b) {
            safe = new AtomicBoolean(b);
            unsafe = b;
        }

        public boolean compareAndSetSafe(boolean expectedValue, boolean newValue) {
            return safe.compareAndSet(expectedValue, newValue);
        }

        /**
         * This method is not thread safe, can only be used from single thread.
         *
         * @param expectedValue expected value
         * @param newValue new value
         * @return true if the value was updated
         */
        public boolean compareAndSetUnsafe(boolean expectedValue, boolean newValue) {
            if (unsafe != expectedValue) {
                return false;
            }
            unsafe = newValue;
            return true;
        }

        public void setSafe(boolean b) {
            safe.set(b);
        }

        public void setUnsafe(boolean b) {
            unsafe = b;
        }

    }


    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_02_MultiCopyAtomic[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

     */

    @JCStressTest
    @State
    @Outcome(id = "1", expect = ACCEPTABLE, desc = "Boring: queue empty")
    @Outcome(expect = FORBIDDEN, desc = "Impossible: dangling element in the queue")
    public static class MpscQueueFirst {

        private static final int WORKER_COUNT = 4;

        public final MpscUnboundedAtomicArrayQueue<Integer> queue = new MpscUnboundedAtomicArrayQueue<>(256);

        private final MpscUnboundedAtomicArrayQueue<Runnable> runnableQueue = new MpscUnboundedAtomicArrayQueue<>(256);

        private void executeInEventLoopThread(Runnable runnable) {
            runnableQueue.offer(runnable);
        }

        private final HasOngoingSendLoop hasOngoingConsumer = new HasOngoingSendLoop();

        private final AtomicInteger workers = new AtomicInteger(WORKER_COUNT);

        private void test(int j) {
            try {
                for (int i = 0; i < 1000; i++) {
                    if (!offer(j * 1000 + i)) {
                        return;
                    }
                }
            } finally {
                workers.decrementAndGet();
            }
        }

        private boolean offer(int ele) {
            queue.offer(ele);
            return scheduleIfNeeded();
        }

        private boolean scheduleIfNeeded() {
            if (hasOngoingConsumer.compareAndSetSafe(false, true)) {
                executeInEventLoopThread(this::scheduleInEventLoopIfNeeded);
                return true;
            } else {
                // Not possible to have a dangling task in the queue.
                // 1. offer(ele) synchronize-before compareAndSetUnsafe(false, true)
                // 2. compareAndSetUnsafe(false, true) synchronize-before hasOngoingConsumer.setSafe(false) in first loopSend0()
                // 3. hasOngoingConsumer.setSafe(false) synchronize-before second loopSend0(), which will drain the queue.
                return false;
            }
        }

        private void scheduleInEventLoopIfNeeded() {
            if (hasOngoingConsumer.compareAndSetUnsafe(false, true)) {
                loopSend();
            }
        }

        private void loopSend() {
            loopSend0(1, 1, true);
        }

        private void loopSend0(final int maxBatchSize, int remainingSpinCount, final boolean firstCall) {
            do {
                final int count = pollBatch(maxBatchSize);
                if (count == 0 || (firstCall && count < maxBatchSize)) {
                    // queue was empty
                    break;
                }
            } while (--remainingSpinCount > 0);

            if (remainingSpinCount <= 0) {
                executeInEventLoopThread(this::loopSend);
                return;
            }

            // QPSPattern is low and we have drained all tasks.
            if (firstCall) {
                // Don't setUnsafe here because loopSend0() may result in a delayed loopSend() call.
                hasOngoingConsumer.setSafe(false);
                // Guarantee thread-safety: no dangling tasks in the queue.
                loopSend0(maxBatchSize, remainingSpinCount, false);
            } else {
                // In low qps pattern, the send job will be triggered later when a new task is added,
                hasOngoingConsumer.setUnsafe(false);
            }
        }

        private int pollBatch(final int maxBatchSize) {
            int count = 0;
            for (; count < maxBatchSize; count++) {
                final Integer ele = queue.poll(); // relaxed poll is faster and we wil retry later anyway.
                if (ele == null) {
                    break;
                }
            }
            return count;
        }

        // Single thread event loop
        @Actor
        public void eventLoopThread() {
            while (true) {
                Runnable runnable = runnableQueue.poll();
                if (runnable != null) {
                    runnable.run();
                } else if (workers.get() <= 0) {
                    while ((runnable = runnableQueue.poll()) != null) {
                        runnable.run();
                    }
                    break;
                }
            }
        }

        @Actor
        public void actor1() {
            test(0);
        }

        @Actor
        public void actor2() {
            test(1);
        }

        @Actor
        public void actor3() {
            test(2);
        }

        @Actor
        public void actor4() {
            test(WORKER_COUNT - 1);
        }

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = queue.isEmpty() ? 1 : 0;
        }

    }

}
