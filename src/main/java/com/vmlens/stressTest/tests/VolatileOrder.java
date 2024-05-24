package com.vmlens.stressTest.tests;

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

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.III_Result;
import org.openjdk.jcstress.infra.results.II_Result;

import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;
import static org.openjdk.jcstress.util.UnsafeHolder.UNSAFE;

@SuppressWarnings("unused")
public class VolatileOrder {

    /*
        How to run this test:
            $ java -jar jcstress-samples/target/jcstress.jar -t AdvancedJMM_15_VolatilesAreNotFences[.SubTestName]
     */

    /*
       ----------------------------------------------------------------------------------------------------------

        Similarly to AdvancedJMM_14_SynchronizedAreNotFences example, the volatile accesses cannot be reliably
        used for their auxiliary memory effects. In this example, if we do not observe the write of the "b", then
        we can see the old "x", even though volatile accesses _might_ be implemented with barriers.

        This reproduces on AArch64:
            RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
           0, 0, 0  401,242,129   51.33%   Acceptable  Boring
           0, 0, 1   12,608,887    1.61%   Acceptable  Irrelevant
           0, 1, 1    6,231,104    0.80%   Acceptable  Irrelevant
           1, 0, 0       91,935    0.01%  Interesting  Whoa
           1, 0, 1    4,941,677    0.63%   Acceptable  Irrelevant
           1, 1, 1  356,621,484   45.62%   Acceptable  Boring
     */

    @JCStressTest
    @State
    @Outcome(id = {"1, 0"}, expect = FORBIDDEN,             desc = "Irrelevant")
    @Outcome(               expect = ACCEPTABLE, desc = "Boring")
    public static class Volatiles {
        int x;
        volatile int b;

        @Actor
        void thread1() {
            x = 1;
            b = 1; // fake "release"
        }

        @Actor
        void thread2(II_Result r) {
            r.r1 = b; // fake "acquire"
            r.r2 = x;
        }
    }

    @JCStressTest
    @State
    @Outcome(id = {"1, 0"}, expect = FORBIDDEN,             desc = "Irrelevant")
    @Outcome(               expect = ACCEPTABLE, desc = "Boring")
    public static class Fence {
        int x;
        int b;

        @Actor
        void thread1() {
            x = 1;
            VarHandle.storeStoreFence();
            b = 1; // fake "release"
        }

        @Actor
        void thread2(II_Result r) {
            r.r1 = b; // fake "acquire"
            VarHandle.loadLoadFence();
            r.r2 = x;
        }
    }

}
