package com.vmlens.stresstest.tests;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IIIII_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;


public class NonFinalCausalConsistency {
    static class Holder {
        int f1;
        int f2;
        int f3;
        int f4;

        Holder(int v) {
            if (v == 0) {
                f1 = -1;
                f2 = -1;
                f3 = -1;
                f4 = -1;
                return;
            }
            f1 = v;
            f2 = v + 1;
            f3 = v + 2;
            f4 = f3 + 1;
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
    public static class NonFinalCausalConsistencySetReleaseNoGetAcquire {
        static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(NonFinalCausalConsistencySetReleaseNoGetAcquire.class, "h", Holder.class);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }


        static final Holder INITIAL = new Holder(0);

        Holder h = INITIAL;

        @Actor
        void thread1() {
            VH.setRelease(this, new Holder(1));
        }

        @Actor
        void thread2(IIIII_Result r) {
            Holder lh = h;
            r.r1 = (lh == INITIAL ? 0 : 1);
            // guarantee to see completed object even though we don't have getAcquire
            r.r2 = lh.f1;
            r.r3 = lh.f2;
            r.r4 = lh.f3;
            r.r5 = lh.f4;
        }
    }

    @JCStressTest
    @State
    @Outcome(id = "0, -1, -1, -1, -1", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(id = "1, 1, 2, 3, 4", expect = ACCEPTABLE, desc = "Boring")
    @Outcome(expect = FORBIDDEN, desc = "Whoa")
    public static class NonFinalCausalConsistencyStoreStoreFenceNoLoadLoadFence {

        static final Holder INITIAL = new Holder(0);

        Holder h = INITIAL;

        @Actor
        void thread1() {
            Holder ref = new Holder(1);
            VarHandle.storeStoreFence();
            h = ref;
        }

        @Actor
        void thread2(IIIII_Result r) {
            Holder lh = h;
            r.r1 = (lh == INITIAL ? 0 : 1);
            // guarantee to see completed object even though we don't have getAcquire
            r.r2 = lh.f1;
            r.r3 = lh.f2;
            r.r4 = lh.f3;
            r.r5 = lh.f4;
        }
    }

}
