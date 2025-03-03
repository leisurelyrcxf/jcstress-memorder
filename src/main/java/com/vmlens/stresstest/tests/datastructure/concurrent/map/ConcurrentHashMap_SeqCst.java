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
package com.vmlens.stresstest.tests.datastructure.concurrent.map;

import com.vmlens.stresstest.datastruct.concurrent.map.MyConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IIII_Result;
import org.openjdk.jcstress.infra.results.II_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@SuppressWarnings("ALL")
public class ConcurrentHashMap_SeqCst {

    private static final String KEY1 = "a";
    private static final String KEY2 = "ccc";

    @JCStressTest
    @Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = ACCEPTABLE, desc = "Trivial under sequential consistency")
    @Outcome(id = "0, 0", expect = FORBIDDEN, desc = "Violates sequential consistency")
    @State
    public static class ConcurrentHashMapDekker_put {
        MyConcurrentHashMap<String, Integer> map = new MyConcurrentHashMap<>();

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
    @Outcome(id = {"1, 2, 2, 1", "2, 1, 1, 2"}, expect = FORBIDDEN, desc = "Violates coherence.")
    @Outcome(expect = ACCEPTABLE, desc = "Every other result is ignored.")
    @State
    public static class ConcurrentHashMap_NonMCA_Coherence {
        final ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        @Actor
        public void actor1() {
            map.put(KEY1, 1);
        }

        @Actor
        public void actor2() {
            map.put(KEY1, 2);
        }

        @Actor
        public void actor3(IIII_Result r) {
            r.r1 = map.getOrDefault(KEY1, 0);
            r.r2 = map.getOrDefault(KEY1, 0);
        }

        @Actor
        public void actor4(IIII_Result r) {
            r.r3 = map.getOrDefault(KEY1, 0);
            r.r4 = map.getOrDefault(KEY1, 0);
        }
    }

    @JCStressTest
    @Outcome(id = "1, 0, 1, 0", expect = FORBIDDEN, desc = "Whoa")
    @Outcome(expect = ACCEPTABLE, desc = "Boring")
    @State
    public static class ConcurrentHashMap_MultiCopy {
        final ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        @Actor
        public void actor1() {
            map.put(KEY1, 1);
        }

        @Actor
        public void actor2() {
            map.put(KEY2, 1);
        }

        @Actor
        public void actor3(IIII_Result r) {
            r.r1 = map.getOrDefault(KEY1, 0);
            r.r2 = map.getOrDefault(KEY2, 0);
        }

        @Actor
        public void actor4(IIII_Result r) {
            r.r3 = map.getOrDefault(KEY2, 0);
            r.r4 = map.getOrDefault(KEY1, 0);
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
    public static class SynchronizedOpaqueDekker {
        static final VarHandle VH_ARRAY = MethodHandles.arrayElementVarHandle(int[].class);
        static final int FIRST = 0;
        static final int SECOND = 1;
        final int[] array = new int[2];
        final AtomicInteger counter = new AtomicInteger();

        @Actor
        public void actor1(II_Result r) {
            VH_ARRAY.setOpaque(array, FIRST, 1);
            counter.incrementAndGet(); // full fence
            r.r1 = (int) VH_ARRAY.getOpaque(array, SECOND);
        }

        @Actor
        public void actor2(II_Result r) {
            VH_ARRAY.setOpaque(array, SECOND, 1);
            counter.incrementAndGet(); // full fence
            r.r2 = (int) VH_ARRAY.getOpaque(array, FIRST);
        }
    }

    @JCStressTest
    @Outcome(id = {"-1, 1", "1, -1", "1, 1"}, expect = ACCEPTABLE, desc = "Trivial under sequential consistency")
    @Outcome(id = {"-1, -1"}, expect = FORBIDDEN, desc = "Violates sequential consistency")
    @Outcome(expect = FORBIDDEN, desc = "Partial constructed object")
    @State
    public static class SynchronizedOpaqueDekker_PartialConstruction {
        @NoArgsConstructor
        @AllArgsConstructor
        static class Node {
            int val;
        }

        static final VarHandle VH_ARRAY = MethodHandles.arrayElementVarHandle(Node[].class);
        static final int FIRST = 0;
        static final int SECOND = 1;
        final Node[] array = new Node[2];
        final AtomicInteger counter = new AtomicInteger();

        public SynchronizedOpaqueDekker_PartialConstruction() {
            array[FIRST] = new Node(-1);
            array[SECOND] = new Node(-1);
        }

        @Actor
        public void actor1(II_Result r) {
            VH_ARRAY.setOpaque(array, FIRST, new Node(1));
            counter.incrementAndGet(); // equivalent to fullFence
            r.r1 = ((Node) VH_ARRAY.getOpaque(array, SECOND)).val;
        }

        @Actor
        public void actor2(II_Result r) {
            VH_ARRAY.setOpaque(array, SECOND, new Node(1));
            counter.incrementAndGet(); // equivalent to fullFence
            r.r2 = ((Node) VH_ARRAY.getOpaque(array, FIRST)).val;
        }
    }

    @JCStressTest
    @Outcome(id = {"-1, 1", "1, -1", "1, 1"}, expect = ACCEPTABLE, desc = "Trivial under sequential consistency")
    @Outcome(id = {"-1, -1"}, expect = FORBIDDEN, desc = "Violates sequential consistency")
    @Outcome(expect = FORBIDDEN, desc = "Partial constructed object")
    @State
    public static class SynchronizedAcqRelDekker_PartialConstruction_Impossible_AcqRel {
        @NoArgsConstructor
        @AllArgsConstructor
        static class Node {
            int val;
        }

        static final VarHandle VH_ARRAY = MethodHandles.arrayElementVarHandle(Node[].class);
        static final int FIRST = 0;
        static final int SECOND = 1;
        final Node[] array = new Node[2];
        final AtomicInteger counter1 = new AtomicInteger();
        final AtomicInteger counter2 = new AtomicInteger();

        public SynchronizedAcqRelDekker_PartialConstruction_Impossible_AcqRel() {
            array[FIRST] = new Node(-1);
            array[SECOND] = new Node(-1);
        }

        @Actor
        public void actor1(II_Result r) {
            VH_ARRAY.setRelease(array, FIRST, new Node(1));
            counter1.incrementAndGet(); // equivalent to fullFence
            r.r1 = ((Node) VH_ARRAY.getAcquire(array, SECOND)).val;
        }

        @Actor
        public void actor2(II_Result r) {
            VH_ARRAY.setRelease(array, SECOND, new Node(1));
            counter2.incrementAndGet(); // equivalent to fullFence
            r.r2 = ((Node) VH_ARRAY.getAcquire(array, FIRST)).val;
        }
    }

}
