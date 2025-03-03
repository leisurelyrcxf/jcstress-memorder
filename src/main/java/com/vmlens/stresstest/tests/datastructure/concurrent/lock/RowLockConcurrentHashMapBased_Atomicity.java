package com.vmlens.stresstest.tests.datastructure.concurrent.lock;

import com.vmlens.stresstest.datastruct.concurrent.lock.RowLockManagerConcurrentHashMapBased;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * @author chenxiaofan
 */
@SuppressWarnings("unused")
@JCStressTest
@State
@Outcome(id = "5", expect = ACCEPTABLE, desc = "Boring")
@Outcome(expect = FORBIDDEN, desc = "Cannot happen")
public class RowLockConcurrentHashMapBased_Atomicity {
    public static final String KEY = "key";
    final RowLockManagerConcurrentHashMapBased<String> rowLockManager = new RowLockManagerConcurrentHashMapBased<>();

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

    @Actor
    public void actor4() {
        lockAdd();
    }

    @Actor
    public void actor5() {
        lockAdd();
    }

    @Arbiter
    public void arbiter(I_Result r) {
        RowLockManagerConcurrentHashMapBased.Lock rowLock = rowLockManager.getLock(KEY);
        rowLock.lock();
        r.r1 = val;
        rowLock.unlock();
    }

    private void lockAdd() {
        RowLockManagerConcurrentHashMapBased.Lock rowLock = rowLockManager.getLock(KEY);
        rowLock.lock();
        val++;
        rowLock.unlock();
    }

}
