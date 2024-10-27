package com.vmlens.stresstest.tests;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@State
@Outcome(id = "42", expect = ACCEPTABLE, desc = "Boring")
@Outcome(id = "-2", expect = ACCEPTABLE, desc = "Not initialized yet")
@Outcome(expect = FORBIDDEN, desc = "Every other result is forbidden.")
public class DefaultValue {

    static class Holder {

        int x;

        Holder() {
            x = 42;
        }

    }

    static class HolderHolder {

        final Holder h = new Holder();

    }

    HolderHolder hh;

    @Actor
    void thread1() {
        hh = new HolderHolder();
    }

    @Actor
    void thread2(I_Result r) {
        if (hh == null) {
            r.r1 = -2;
        } else {
            Holder h = hh.h;
            r.r1 = h.x;
        }
    }

}

