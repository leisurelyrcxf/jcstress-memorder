package com.vmlens.stresstest.tests;

import com.vmlens.stresstest.datastruct.OfferResult;
import com.vmlens.stresstest.datastruct.concurrent.queue.ConcurrentLinkedDrainClosableQueue;
import org.apache.mina.util.ConcurrentHashSet;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.L_Result;

import java.util.concurrent.ExecutionException;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@State
@Outcome(id = {"1"}, expect = ACCEPTABLE, desc = "Boring")
@Outcome(expect = FORBIDDEN, desc = "Processed != Offered")
public class TestDrainClosableQueue {
    private final ConcurrentLinkedDrainClosableQueue<Integer> queue = new ConcurrentLinkedDrainClosableQueue<>(true);
    private final ConcurrentHashSet<Integer> offered = new ConcurrentHashSet<>();
    private final ConcurrentHashSet<Integer> processed = new ConcurrentHashSet<>();


    @Actor
    void thread1() {
        queue.drainAndClose(processed::add);
    }

    @Actor
    void thread2() {
        int i = 0;
        while (true) {
            try {
                if (queue.tryOffer(i, false) != OfferResult.SUCCESS)
                    break;
                offered.add(i);
                i = i + 3;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Actor
    void thread3() {
        int i = 1;
        while (true) {
            try {
                if (queue.tryOffer(i, false) != OfferResult.SUCCESS)
                    break;
                offered.add(i);
                i = i + 3;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Actor
    void thread4() {
        int i = 2;
        while (true) {
            try {
                if (queue.tryOffer(i, false) != OfferResult.SUCCESS)
                    break;
                offered.add(i);
                i = i + 3;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Arbiter
    public void arbiter(L_Result r) {
        r.r1 = processed.equals(offered) ? 1 : 0;
    }
}
