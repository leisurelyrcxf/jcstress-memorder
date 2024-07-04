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
import org.openjdk.jcstress.infra.results.II_Result;
import org.openjdk.jcstress.infra.results.I_Result;
import org.openjdk.jcstress.infra.results.L_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

public class MpscQueue {


    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_02_MultiCopyAtomic[.SubTestName]
     */

    /*
      ----------------------------------------------------------------------------------------------------------

     */

    @SuppressWarnings("StatementWithEmptyBody")
    @JCStressTest
    @State
    @Outcome(id = "1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(          expect = FORBIDDEN,  desc = "Impossible")
    public static class MpscQueueFirst {

        public final MpscUnboundedAtomicArrayQueue<Integer> queue = new MpscUnboundedAtomicArrayQueue<>(256);; // new MpscUnboundedAtomicArrayQueue<>(256);

        private volatile boolean hasOngoingConsumer = false;

        /**
         * Create var handle for hasOngoingConsumer
         */
        private static final VarHandle HAS_ONGOING_CONSUMER;
        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                HAS_ONGOING_CONSUMER = l.findVarHandle(MpscQueueFirst.class, "hasOngoingConsumer", boolean.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private final MpscUnboundedAtomicArrayQueue<Runnable> runnableQueue = new MpscUnboundedAtomicArrayQueue<>(256);
        private void schedule(Runnable runnable) {
            runnableQueue.offer(runnable);
        }

        private final AtomicInteger workers = new AtomicInteger(3);

        @Actor
        public void actor4() {
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
        private void test(int j) {
            try {
                for (int i = 0; i < 1000; i++) {
                    offer(j * 1000 + i);
                }
            } finally {
                workers.decrementAndGet();
            }
        }

        private void offer(int n) {
            queue.offer(n);

            if (!HAS_ONGOING_CONSUMER.compareAndSet(this, false, true)) {
                return;
            }

            schedule(() -> {
                while (queue.poll() != null)
                    ;
                hasOngoingConsumer = false;
                while (queue.poll() != null)
                    ;
            });
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

        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = queue.isEmpty() ? 1 : 0;
        }
    }

}
