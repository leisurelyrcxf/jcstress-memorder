package com.vmlens.stresstest.datastruct.concurrent.lock;

import java.util.concurrent.TimeUnit;

public interface AsyncLock {
    void lock();

    boolean tryLock(long timeout, TimeUnit timeUnit);

    void unlock();
}
