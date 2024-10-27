/*
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
package com.vmlens.stressTest.tests.concurrenthashmap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ConcurrentHashMap;

import com.vmlens.stressTest.datastruct.concurrent.map.MyConcurrentHashMap;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@SuppressWarnings("ALL")
public class ConcurrentHashMap_SeqCst {

    private static final String KEY1 = "a";
    private static final String KEY2 = "b";


    /*
      ----------------------------------------------------------------------------------------------------------

        Adding volatile to both $x and $y bring them together into synchronization order, and thus require
        the results to be consistent with the case when reads/writes form a total order.

          RESULT        SAMPLES     FREQ      EXPECT  DESCRIPTION
            0, 0              0    0.00%   Forbidden  Violates sequential consistency
            0, 1  1,016,018,128   44.40%  Acceptable  Trivial under sequential consistency
            1, 0  1,068,127,239   46.68%  Acceptable  Trivial under sequential consistency
            1, 1    204,027,177    8.92%  Acceptable  Trivial under sequential consistency
     */

    @JCStressTest
    @Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = ACCEPTABLE, desc = "Trivial under sequential consistency")
    @Outcome(id = "0, 0", expect = FORBIDDEN, desc = "Violates sequential consistency")
    @State
    public static class ConcurrentHashMapDekker_put {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        @Actor
        public void actor1(II_Result r) {
            map.put(KEY1, 1);
            r.r1 = map.getOrDefault(KEY2, 0);
        }

        @Actor
        public void actor2(II_Result r) {
            map.put(KEY2, 1);
            r.r2 = map.getOrDefault(KEY1, 0);
        }
    }

    @JCStressTest
    @Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = ACCEPTABLE, desc = "Trivial under sequential consistency")
    @Outcome(id = "0, 0", expect = FORBIDDEN, desc = "Violates sequential consistency")
    @State
    public static class ConcurrentHashMapDekker_Compute {
        MyConcurrentHashMap<String, Integer> map = new MyConcurrentHashMap<>();

        @Actor
        public void actor1(II_Result r) {
            map.compute(KEY1, (k, v) -> v == null ? 1 : v);
            r.r1 = map.getOrDefault(KEY2, 0);
        }

        @Actor
        public void actor2(II_Result r) {
            map.compute(KEY2, (k, v) -> v == null ? 1 : v);
            r.r2 = map.getOrDefault(KEY1, 0);
        }
    }


    @JCStressTest
    @Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = ACCEPTABLE, desc = "Trivial under sequential consistency")
    @Outcome(id = "0, 0", expect = FORBIDDEN, desc = "Violates sequential consistency")
    @State
    public static class SynchronizedAcqRelDekker {
        static final VarHandle VH_X;
        static final VarHandle VH_Y;

        static {
            try {
                VH_X = MethodHandles.lookup().findVarHandle(SynchronizedAcqRelDekker.class, "x", int.class);
                VH_Y = MethodHandles.lookup().findVarHandle(SynchronizedAcqRelDekker.class, "y", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        int x;
        int y;

        final Object lockx = new Object();
        final Object locky = new Object();

        @Actor
        public void actor1(II_Result r) {
            synchronized (lockx) {
                VH_X.setRelease(this, 1);
            }
            r.r1 = (int) VH_Y.getAcquire(this);
        }

        @Actor
        public void actor2(II_Result r) {
            synchronized (locky) {
                VH_Y.setRelease(this, 1);
            }
            r.r2 = (int) VH_X.getAcquire(this);
        }
    }


}
