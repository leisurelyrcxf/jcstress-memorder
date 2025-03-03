package com.vmlens.stresstest.datastruct.concurrent;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

/**
 * Single flight helps threads share the same task to execute a heavy operations (e.g. fetching data from database).
 * It is pretty much similar to the golang singleflight package.
 *
 * @param <K> key type
 * @param <V> value type
 * @author chenxiaofan
 */
public class SingleFlight<K, V> {
    private static final long WAIT_TIMEOUT_NANOS = Duration.ofSeconds(10).toNanos();
    private static final String ERROR_WAIT_TIMEOUT = "wait timeout";

    private static class BooleanWrapper {
        boolean isTrue;

        public BooleanWrapper(boolean isTrue) {
            this.isTrue = isTrue;
        }
    }

    private final ConcurrentMap<K, Call<V>> calls = new ConcurrentHashMap<>();

    /**
     * @param key      task key, it is used to identify the task.
     * @param callable task to execute
     * @return execution result of the task
     * @throws Exception exception thrown by the task.
     */
    public V execute(K key, Callable<V> callable) throws Exception {
        final BooleanWrapper isNewCall = new BooleanWrapper(false);
        final Call<V> call = calls.computeIfAbsent(key, k -> {
            Call<V> newCall = new Call<>();
            isNewCall.isTrue = true;
            return newCall;
        });
        if (isNewCall.isTrue) {
            try {
                return call.exec(callable);
            } finally {
                calls.remove(key);
            }
        }
        return call.acquire();
    }

    private static class Call<V> {
        private V result = null; // no need to be volatile, because its memory effect is guaranteed by `countDown`
        private Exception error; // no need to be volatile, because its memory effect is guaranteed by `countDown`
        private final CountDownLatch countDown = new CountDownLatch(1);

        V acquire() throws Exception {
            if (!countDown.await(WAIT_TIMEOUT_NANOS, java.util.concurrent.TimeUnit.NANOSECONDS)) {
                throw new TimeoutException(ERROR_WAIT_TIMEOUT);
            }
            if (error != null) {
                throw error;
            }
            return result;
        }

        V exec(Callable<V> callable) throws Exception {
            V res = null;
            Exception exception = null;
            try {
                res = callable.call();
                return res;
            } catch (Exception e) {
                exception = e;
                throw e;
            } finally {
                release(res, exception);
            }
        }

        private void release(V res, Exception exception) {
            this.result = res;
            this.error = exception;
            countDown.countDown();
        }
    }
}
