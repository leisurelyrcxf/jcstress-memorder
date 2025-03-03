package com.vmlens.stresstest.datastruct.concurrent.queue;

import com.vmlens.stresstest.datastruct.OfferResult;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public interface DrainClosableQueue<T> {
    boolean reopen();

    boolean isClosed();

    OfferResult tryOffer(T data, boolean canBlockHint) throws ExecutionException, InterruptedException;

    int drainAndClose(Consumer<T> consumer);


}
