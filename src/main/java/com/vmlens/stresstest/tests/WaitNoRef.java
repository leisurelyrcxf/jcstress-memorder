package com.vmlens.stresstest.tests;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Mode;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.Signal;
import org.openjdk.jcstress.annotations.State;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest(Mode.Termination)
@Outcome(id = "TERMINATED", expect = ACCEPTABLE, desc = "Gracefully finished")
@Outcome(id = "STALE", expect = FORBIDDEN, desc = "Test is stuck")
@State
public class WaitNoRef {

    final AtomicInteger refCount = new AtomicInteger(0);
    volatile Datasource datasource = new Datasource();

    volatile CompletableFuture<Void> waitress;

    private static class Datasource {
        volatile boolean isPoisoned = false;

        int read() {
            if (isPoisoned) {
                System.out.println("Poisoned");
                throw new RuntimeException("Poisoned");
            }
            return ThreadLocalRandom.current().nextInt();
        }
    }

    @Signal
    public void signaller() {
        refCount.incrementAndGet();
        try {
            Datasource ds = this.datasource;
            if (ds != null) {
                ds.read();
            }
        } finally {
            if (0 == refCount.decrementAndGet()) {
                Thread.yield();
                CompletableFuture<Void>  wai = waitress;
                if (wai != null) {
                    wai.complete(null);
                }
            }
        }
    }

//    @Actor
//    public void actor2() {
//        var ds = this.datasource;
//        if (ds == null) {
//            return;
//        }
//        refCount.incrementAndGet();
//        try {
//            ds.read();
//        } finally {
//            if (0 == refCount.decrementAndGet()) {
//                if (waitress != null) {
//                    waitress.complete(null);
//                }
//            }
//        }
//    }

    @Actor
    public void poisoner() {
        Datasource old = datasource;
        datasource = null;

        waitress = new CompletableFuture<>();
        Thread.yield();
        if (refCount.get() != 0) {
            waitress.join();
        }
        waitress = null;
        old.isPoisoned = true;
    }

//    @Actor
//    public void poisoner_may_stuck() {
//        var old = datasource;
//        datasource = null;
//        if (refCount.get() != 0) {
//            waitress = new CompletableFuture<>();
//            waitress.join();
//        }
//        old.isPoisoned = true;
//    }
}
