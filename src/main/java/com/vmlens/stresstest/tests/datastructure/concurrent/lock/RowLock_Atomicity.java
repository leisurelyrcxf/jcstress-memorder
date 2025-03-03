package com.vmlens.stresstest.tests.datastructure.concurrent.lock;

import com.vmlens.stresstest.datastruct.concurrent.lock.RowLockManager;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@State
@Outcome(id = "3", expect = ACCEPTABLE, desc = "Boring")
@Outcome(expect = FORBIDDEN, desc = "Cannot happen")
public class RowLock_Atomicity {
    public static final String KEY = "key";
    final RowLockManager<String> rowLockManager = new RowLockManager<>();

    int val = 0;

    @Actor
    public void actor1() {
        lockAdd();
    }

    @Actor
    public void actor2() {
        lockAdd();
    }

    @Actor
    public void actor3() {
        lockAdd();
    }

    @Arbiter
    public void arbiter(I_Result r) {
        RowLockManager.Lock rowLock = rowLockManager.getLock(KEY);
        rowLock.lock();
        r.r1 = val;
        rowLock.unlock();
    }

    private void lockAdd() {
        RowLockManager.Lock rowLock = rowLockManager.getLock(KEY);
        rowLock.lock();
        val++;
        rowLock.unlock();
    }

}
