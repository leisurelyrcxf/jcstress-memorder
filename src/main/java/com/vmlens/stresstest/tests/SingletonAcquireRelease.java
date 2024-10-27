package com.vmlens.stresstest.tests;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@State
@Outcome(id = "42, 42", expect = ACCEPTABLE,  desc = "Boring")
@Outcome(              expect = FORBIDDEN,   desc = "Whoa")
public class SingletonAcquireRelease {
    static class Holder {
        int x;
        Holder() {
            x = 42;
        }
    }

    Holder h;

    static final VarHandle VH_H;

    static {
        try {
            VH_H = MethodHandles.lookup().findVarHandle(SingletonAcquireRelease.class, "h", Holder.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

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
        Holder lh = (Holder) VH_H.getAcquire(this);
        if (lh != null) {
            return lh;
        }
        synchronized (this) {
            if (h != null) {
                return h;
            }
            Holder nlh = new Holder();
            VH_H.setRelease(this, nlh);
            return nlh;
        }
    }
}

