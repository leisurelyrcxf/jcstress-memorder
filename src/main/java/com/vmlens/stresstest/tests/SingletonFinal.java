package com.vmlens.stresstest.tests;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@State
@Outcome(id = "42, 42", expect = ACCEPTABLE,  desc = "Boring")
@Outcome(              expect = FORBIDDEN,   desc = "Whoa")
public class SingletonFinal {
    static class Holder {
        int x;
        final int y;
        Holder() {
            x = 42;
            y = 43;
        }
    }

    Holder h;

    @Actor
    void thread1(II_Result r) {
        Holder lh = getInstance();
        if (lh != null) {
            r.r1 = lh.x;
        } else {
            r.r1 = -1;
        }
    }

    @Actor
    void thread2(II_Result r) {
        Holder lh = getInstance();
        if (lh != null) {
            r.r2 = lh.x;
        } else {
            r.r2 = -1;
        }
    }

    Holder getInstance() {
        if (h == null) {
            synchronized (this) {
                if (h == null) {
                    h = new Holder();
                }
            }
        }
        return h;
    }
}

