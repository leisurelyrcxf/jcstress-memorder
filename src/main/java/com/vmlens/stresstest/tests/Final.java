package com.vmlens.stressTest.tests;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IIIII_Result;
import org.openjdk.jcstress.infra.results.IIII_Result;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

public class Final {
    @JCStressTest
    @State
    @Outcome(id = {"-1", "42"}, expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "0", expect = FORBIDDEN, desc = "Whoa")
    public static class FinalSimple {
        static class Holder {
            final int x;

            Holder() {
                x = 42;
            }
        }

        Holder h;

        @Actor
        void thread1() {
            h = new Holder();
        }

        @Actor
        void thread2(I_Result r) {
            Holder lh = h;
            if (lh != null) {
                r.r1 = lh.x;
            } else {
                r.r1 = -1;
            }
        }
    }

    /**
     * @author chenxiaofan
     */
    @JCStressTest
    @State
    @Outcome(id = "-1, -1, -1, -1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "1, 2, 3, 4", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(expect = FORBIDDEN, desc = "Whoa")
    public static class FinalPlusNonFinal {
        static class Holder {
            int f1;
            final int y;
            int f2;
            int f3;
            int f4;

            Holder() {
                y = 2;
                f1 = 1;
                f2 = 2;
                f3 = 3;
                f4 = 4;
            }
        }

        Holder h;

        @Actor
        void thread1() {
            h = new Holder();
        }

        @Actor
        void thread2(IIII_Result r) {
            Holder lh = h;
            if (lh != null) {
                r.r1 = lh.f1;
//            r.r2 = lh.y; not event read the final field
                r.r2 = lh.f2;
                r.r3 = lh.f3;
                r.r4 = lh.f4;
            } else {
                r.r1 = -1;
                r.r2 = -1;
                r.r3 = -1;
                r.r4 = -1;
            }
        }
    }

    /**
     * @author chenxiaofan
     */
    @JCStressTest
    @State
    @Outcome(id = "-1, -1, -1, -1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "1, 2, 3, 4", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(expect = FORBIDDEN, desc = "Whoa")
    public static class FinalPlusNonFinalInitiallyNonnull {
        static class Holder {
            int f1;
            final int y;
            int f2;
            int f3;
            int f4;

            Holder(int v) {
                if (v == 0) {
                    y = 2;
                    f1 = -1;
                    f2 = -1;
                    f3 = -1;
                    f4 = -1;
                    return;
                }
                y = 2;
                f1 = 1;
                f2 = 2;
                f3 = 3;
                f4 = 4;
            }
        }

        static final Holder INITIAL = new Holder(0);

        Holder h = INITIAL;

        @Actor
        void thread1() {
            h = new Holder(1);
        }

        @Actor
        void thread2(IIII_Result r) {
            Holder lh = h;
            if (lh != INITIAL) {
                r.r1 = lh.f1;
//            r.r2 = lh.y; not event read the final field
                r.r2 = lh.f2;
                r.r3 = lh.f3;
                r.r4 = lh.f4;
            } else {
                r.r1 = lh.f1;
//            r.r2 = lh.y; not event read the final field
                r.r2 = lh.f2;
                r.r3 = lh.f3;
                r.r4 = lh.f4;
            }
        }
    }


    /**
     * @author chenxiaofan
     */
    @JCStressTest
    @State
    @Outcome(id = "0, -1, -1, -1, -1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "1, 1, 2, 3, 4", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(expect = FORBIDDEN, desc = "Whoa")
    public static class FinalPlusNonFinalInitiallyNonnullNoCausalConsistency {
        static class Holder {
            int f1;
            final int y;
            int f2;
            int f3;
            int f4;

            Holder(int v) {
                if (v == 0) {
                    y = 2;
                    f1 = -1;
                    f2 = -1;
                    f3 = -1;
                    f4 = -1;
                    return;
                }
                y = 2;
                f1 = v;
                f2 = v + 1;
                f3 = v + 2;
                f4 = f3 + 1;
            }
        }

        static final Holder INITIAL = new Holder(0);

        Holder h = INITIAL;

        @Actor
        void thread1() {
            h = new Holder(1);
        }

        @Actor
        void thread2(IIIII_Result r) {
            Holder lh = h;
            r.r1 = (lh == INITIAL ? 0 : 1);
            r.r2 = lh.f1;
            r.r3 = lh.f2;
            r.r4 = lh.f3;
            r.r5 = lh.f4;
        }
    }

    @JCStressTest
    @State
    @Outcome(id = "0, 1, -1, -1, -1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "0, -1, -1, -1, -1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "1, 1, 2, 3, 4", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(expect = FORBIDDEN, desc = "Whoa")
    public static class FinalPlusNonFinalInitiallyNonnullNoCausalConsistencyNonLocalRef {
        static class Holder {
            int f1;
            final int y;
            int f2;
            int f3;
            int f4;

            Holder(int v) {
                if (v == 0) {
                    y = 2;
                    f1 = -1;
                    f2 = -1;
                    f3 = -1;
                    f4 = -1;
                    return;
                }
                y = 2;
                f1 = v;
                f2 = v + 1;
                f3 = v + 2;
                f4 = f3 + 1;
            }
        }

        static final FinalPlusNonFinalInitiallyNonnullNoCausalConsistencyNonLocalRef.Holder INITIAL = new FinalPlusNonFinalInitiallyNonnullNoCausalConsistencyNonLocalRef.Holder(0);

        FinalPlusNonFinalInitiallyNonnullNoCausalConsistencyNonLocalRef.Holder h = INITIAL;

        @Actor
        void thread1() {
            h = new FinalPlusNonFinalInitiallyNonnullNoCausalConsistencyNonLocalRef.Holder(1);
        }

        @Actor
        void thread2(IIIII_Result r) {
            FinalPlusNonFinalInitiallyNonnullNoCausalConsistencyNonLocalRef.Holder lh = h;
            r.r1 = (lh == INITIAL ? 0 : 1);
            r.r2 = h.f1;
            r.r3 = lh.f2;
            r.r4 = lh.f3;
            r.r5 = lh.f4;
        }
    }

    @JCStressTest
    @State
    @Outcome(id = "1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "-1", expect = FORBIDDEN, desc = "Seen uninitialized HashMap object")
    @Outcome(expect = FORBIDDEN, desc = "Whoa")
    public static class FinalHashMap {
        static final Map<Integer, Integer> INITIAL = Map.of(2, 2);
        Map<Integer, Integer> m = INITIAL;

        @Actor
        void thread1() {
            m = new HashMap<>(m);
        }

        @Actor
        void thread2(I_Result r) {
            r.r1 = m.containsKey(2) ? 1 : -1;
        }
    }

    @JCStressTest
    @State
    @Outcome(id = "1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "-1", expect = ACCEPTABLE_INTERESTING, desc = "Seen uninitialized HashSet object")
    @Outcome(expect = FORBIDDEN, desc = "Whoa")
    public static class FinalHashSet {
        Set<Integer> s = Set.of(2);

        @Actor
        void thread1() {
            s = new HashSet<>(s);
        }

        @Actor
        void thread2(I_Result r) {
            r.r1 = s.contains(2) ? 1 : -1;
        }
    }

    @JCStressTest
    @State
    @Outcome(id = "1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "-1", expect = FORBIDDEN, desc = "Not possible with unmodifiableSet because it has final field")
    @Outcome(expect = FORBIDDEN, desc = "Whoa")
    public static class FinalUnmodifiableSet {

        Set<Integer> s = Set.of(2);

        @Actor
        void thread1() {
            s = Collections.unmodifiableSet(new HashSet<>(s));
        }

        @Actor
        void thread2(I_Result r) {
            r.r1 = s.contains(2) ? 1 : -1;
        }
    }

    @JCStressTest
    @State
    @Outcome(id = "1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "-1", expect = FORBIDDEN, desc = "Impossible with storeStoreFence")
    public static class FinalHashSetStoreStoreFence {
        static final Set<Integer> M = Set.of(2);

        Set<Integer> s = new HashSet<>(M);

        @Actor
        void thread1() {
            Set<Integer> ls = new HashSet<>(M);
            VarHandle.storeStoreFence(); // for jdk9 and later.
            // unsafe.storeFence(); for jdk-8
            s = ls;
        }

        @Actor
        void thread2(I_Result r) {
            r.r1 = s.contains(2) ? 1 : -1; // no need loadload fence due to causal consistency
        }
    }

    @JCStressTest
    @State
    @Outcome(id = "1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "-1", expect = FORBIDDEN, desc = "Impossible with setRelease")
    public static class FinalHashSetRelease {
        static final VarHandle VS;

        static {
            try {
                VS = MethodHandles.lookup().findVarHandle(FinalHashSetRelease.class, "s", Set.class);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }


        Set<Integer> s = Set.of(2);

        @Actor
        void thread1() {
            VS.setRelease(this, new HashSet<>(s));
        }

        @Actor
        void thread2(I_Result r) {
            r.r1 = s.contains(2) ? 1 : -1;
        }
    }

}

