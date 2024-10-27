package com.vmlens.stresstest.tests;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings({"unused", "java:S2187"})
@JCStressTest
@Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Neither increment is seen.")
@Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "boring")
@State
public class MemOrderCompletableFuture {

    final CompletableFuture<Void> c = new CompletableFuture<>();
    private int a = 0;

    @Actor
    public void actor1(I_Result r) {
        a = 1;
        c.whenComplete((ret, e)-> {
            r.r1 = this.a;
        });
    }

    @Actor
    public void actor2() {
        c.completeExceptionally(new RuntimeException());
    }
}
