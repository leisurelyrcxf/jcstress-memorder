package com.vmlens.stresstest.datastruct;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public interface DrainClosableQueue<T> {
    boolean reopen();

    boolean isClosed();

    OfferResult tryOffer(T data, boolean canBlockHint) throws ExecutionException, InterruptedException;

    int drainAndClose(Consumer<T> consumer);


}
