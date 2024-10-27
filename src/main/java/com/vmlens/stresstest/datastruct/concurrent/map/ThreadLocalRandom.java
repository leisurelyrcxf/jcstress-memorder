package com.vmlens.stresstest.datastruct.concurrent.map;

public class ThreadLocalRandom {
    private static final ThreadLocal<Integer> PROBE = ThreadLocal.withInitial(() -> 0);

    public static int getProbe() {
        return PROBE.get();
    }

    public static int advanceProbe(int h) {
        // Apply a XorShift to generate a new hash code
        h ^= h << 13;
        h ^= h >>> 17;
        h ^= h << 5;
        PROBE.set(h);
        return h;
    }

    public static void localInit() {
        // Force initialization if not already done
        PROBE.set(java.util.concurrent.ThreadLocalRandom.current().nextInt());
    }
}
