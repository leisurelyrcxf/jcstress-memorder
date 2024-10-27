package com.vmlens.stresstest.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.L_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@State
@Outcome(id = "10", expect = ACCEPTABLE, desc = "Boring")
@Outcome(expect = FORBIDDEN, desc = "Whoa")
public class ParallelStream {

    List<Integer> h = new ArrayList<>();

    List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    @Actor
    public void actor() {
        //        input.parallelStream().forEach(h::add); // unsafe
        h.addAll(input.parallelStream().toList()); // safe
    }

    @Arbiter
    public void arbiter(L_Result r) {
        r.r1 = h.size();
    }

}
