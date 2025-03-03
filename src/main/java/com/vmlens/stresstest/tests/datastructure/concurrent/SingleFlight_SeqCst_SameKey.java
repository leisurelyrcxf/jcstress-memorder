package com.vmlens.stresstest.tests.datastructure.concurrent;

import com.vmlens.stresstest.datastruct.concurrent.SingleFlight;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * @author chenxiaofan
 */
@JCStressTest
@Outcome(id = {"1, 1"}, expect = ACCEPTABLE, desc = "Trivial under sequential consistency")
@Outcome(expect = FORBIDDEN, desc = "Violates sequential consistency")
@State
public class SingleFlight_SeqCst_SameKey {
    private static class IntegerWrapper {
        int value = 0;

        @SuppressWarnings("UnusedReturnValue")
        int getAndIncrement() {
            int old = value;
            value++;
            return old;
        }

        int get() {
            return value;
        }
    }

    IntegerWrapper integerWrapper = new IntegerWrapper();
    SingleFlight<String, Integer> singleFlight = new SingleFlight<>();

    int[] results = new int[3];

    @Actor
    public void actor0() {
        run(0);
    }

    @Actor
    public void actor1() {
        run(1);
    }

    @Actor
    public void actor2() {
        run(2);
    }

    @Arbiter
    public void arbiter(II_Result r) {
        r.r1 = allSameOrMinusOne(results) ? 1 : 0;
        r.r2 = integerWrapper.get();
    }

    private void run(int j) {
        results[j] = run0(j);
    }

    private int run0(int j) {
        try {
            return singleFlight.execute("k1", () -> {
                if (integerWrapper.value == 0) {
                    integerWrapper.getAndIncrement();
                    return j;
                }
                // run only once
                return -1;
            });
        } catch (Exception e) {
            return -2;
        }
    }

    private boolean allSameOrMinusOne(int[] results) {
        int firstNonMinusOne = getFirstNonMinusOne(results);
        for (int result : results) {
            if (result != firstNonMinusOne && result != -1) {
                return false;
            }
        }
        return true;
    }

    private int getFirstNonMinusOne(int[] results) {
        for (int result : results) {
            if (result != -1) {
                return result;
            }
        }
        throw new IllegalStateException("all -1");
    }
}
