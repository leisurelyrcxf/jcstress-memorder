package com.vmlens.stressTest.tests;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@State
@Outcome(id = {"-1", "42"}, expect = ACCEPTABLE,             desc = "Boring")
@Outcome(id = "0",          expect = FORBIDDEN, desc = "Whoa")
public  class Final {
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

