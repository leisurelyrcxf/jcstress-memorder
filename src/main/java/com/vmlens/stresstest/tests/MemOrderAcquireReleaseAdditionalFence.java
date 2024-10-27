package com.vmlens.stresstest.tests;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.L_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "java:S2187"})
@JCStressTest
@Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Only one increment is seen.")
@Outcome(id = "2", expect = Expect.ACCEPTABLE, desc = "Both increments are seen.")
@Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Neither increment is seen.")
@State
public class MemOrderAcquireReleaseAdditionalFence {

    private final AtomicInteger counter = new AtomicInteger();
    private final Consumer<Object> incrFunc = ret -> counter.incrementAndGet();

    static final VarHandle RESULT;
    static final VarHandle LISTENER;

    static {
        try {
            RESULT = MethodHandles.lookup().findVarHandle(MemOrderAcquireReleaseAdditionalFence.class, "result", Object.class);
            LISTENER = MethodHandles.lookup().findVarHandle(MemOrderAcquireReleaseAdditionalFence.class, "listener", Consumer.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private volatile Object result = null;
    private volatile Consumer<Object> listener = null;

    @Actor
    public void actor1() {
        final Consumer<Object> localListener = incrFunc;
        LISTENER.setRelease(this, localListener);

        VarHandle.fullFence();

        final Object localResult = RESULT.getAcquire(this);
        if (localResult != null) {
            localListener.accept(localResult);
        }
    }

    @Actor
    public void actor2() {
        final Object localResult = this;
        RESULT.setRelease(this, localResult);

        VarHandle.storeStoreFence();

        @SuppressWarnings("unchecked")
        final Consumer<Object> localListener = (Consumer<Object>) LISTENER.getAcquire(this);
        if (localListener != null) {
            localListener.accept(localResult);
        }
    }

    @Arbiter
    public void arbiter(L_Result r) {
        r.r1 = counter.get();
    }
}
