package com.vmlens.stressTest.tests;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.L_Result;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "java:S2187"})
@JCStressTest
@Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Both increments are seen.")
@Outcome(id = "2", expect = Expect.ACCEPTABLE, desc = "Only the result write is seen.")
@Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Neither increment is seen.")
@State
public class MemOrderSeqCst {

    private final AtomicInteger counter = new AtomicInteger();
    private final Consumer<Object> incrFunc = ret -> counter.incrementAndGet();

    private volatile Object result = null;
    private volatile Consumer<Object> listener = null;

    @Actor
    public void actor1() {
        listener = incrFunc;

        if (result != null) {
            listener.accept(result);
        }
    }

    @Actor
    public void actor2() {
        result = this;

        if (listener != null) {
            listener.accept(result);
        }
    }

    @Arbiter
    public void arbiter(L_Result r) {
        r.r1 = counter.get();
    }
}
