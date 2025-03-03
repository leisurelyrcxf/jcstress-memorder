package com.vmlens.stresstest.datastruct.concurrent.queue;

import com.vmlens.stresstest.datastruct.OfferResult;

import java.util.ConcurrentModificationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ConcurrentLinkedDrainClosableQueue<T> implements DrainClosableQueue<T> {

    static class Node<T> {
        Node<T> next = null; // an auxiliary field to help drainAndClose()

        final Node<T> prev;

        final T data;

        public Node(T data) {
            prev = null;
            this.data = data;
        }

        public Node(T data, Node<T> prev) {
            this.prev = prev;
            this.data = data;
        }
    }

    private final int maxCapacity;
    private final boolean hasMaxCapacity;

    private final AtomicInteger size;
    private final AtomicReference<Node<T>> tail;
    private final AtomicReference<CompletableFuture<Void>> closing;

    public ConcurrentLinkedDrainClosableQueue(boolean open) {
        this(open, -1);
    }

    public ConcurrentLinkedDrainClosableQueue(boolean open, int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.hasMaxCapacity = maxCapacity > 0;
        this.tail = new AtomicReference<>(null);
        this.closing = new AtomicReference<>(null);
        this.size = new AtomicInteger(0);
        if (open) {
            reopen();
        }
    }

    /**
     * open the queue for offer data.
     *
     * @return true if the queue is successfully opened, false if the queue is already opened.
     */
    @Override
    public boolean reopen() {
        return tail.compareAndSet(null, /* dummy head */ new Node<>(null));
    }

    @Override
    public boolean isClosed() {
        return tail.get() == null;
    }


    /**
     * try offer data to the queue, return false if the queue is closed.
     * <p>
     * This can be called from multiple threads concurrently.
     *
     * @param data         the data to be offered
     * @param canBlockHint true if the method can block, false if the method cannot block (e.g. called from an IO thread).
     * @return 1. SUCCESS if the data is successfully offered to the queue
     * 2. QUEUE_CLOSED if the queue is closed.
     * 3. QUEUE_FULL if the queue is full.
     */
    @SuppressWarnings("java:S3776")
    @Override
    public OfferResult tryOffer(T data, boolean canBlockHint) throws ExecutionException, InterruptedException {
        Node<T> oldTail;
        Node<T> newTail;
        while (true) {
            oldTail = tail.get();
            if (oldTail == null) {
                // closed for append
                return OfferResult.QUEUE_CLOSED;
            }
            CompletableFuture<Void> closingFuture = closing.get();
            if (closingFuture != null && canBlockHint) {
                // Optimization: if the queue is closed, and canBlock is true, then block until the queue is closed.
                // so that drainAndClose() can return much faster.
                closingFuture.get();
                continue;
            }
            if (hasMaxCapacity && size.get() >= maxCapacity) {
                return OfferResult.QUEUE_FULL;
            }
            newTail = new Node<>(data, oldTail);
            if (tail.weakCompareAndSet(oldTail, newTail)) {
                if (hasMaxCapacity) size.incrementAndGet();
                return OfferResult.SUCCESS;
            }
        }
    }

    /**
     * poll all the elements in the queue and close the queue.
     * <p>
     * This can only be called from a single thread.
     *
     * @param consumer the consumer to consume the data
     */
    @SuppressWarnings("java:S3776")
    @Override
    public int drainAndClose(Consumer<T> consumer) {
        Node<T> currTail = tail.get();
        if (currTail == null) {
            // already closed
            return 0;
        }

        CompletableFuture<Void> closingFuture = new CompletableFuture<>();
        if (!closing.compareAndSet(null, closingFuture)) {
            throw new ConcurrentModificationException("closing != null");
        }

        int polled = 0;
        try {
            Node<T> oldTail = null;
            while (true) {
                final Node<T> oldTailNext = getNext(oldTail, currTail);
                for (Node<T> curr = oldTailNext; curr != currTail; curr = curr.next) {
                    if (curr.data != null) {
                        polled++;
                        consumer.accept(curr.data);
                    }
                }
                if (currTail.data != null) {
                    polled++;
                    consumer.accept(currTail.data);
                }
                if (tail.compareAndSet(currTail, null)) {
                    if (hasMaxCapacity) size.set(0);
                    break;
                }
                oldTail = currTail; // retry from the old tail
                currTail = tail.get();
                if (currTail == null) {
                    throw new ConcurrentModificationException("currTail == null");
                }
            }
        } finally {
            closingFuture.complete(null);
            closing.set(null);
        }
        return polled;
    }

    /**
     * get next node of start, and link all nodes between start and next.
     *
     * @param start null means start from head, otherwise the node to find next.
     * @param tail  the tail node
     * @return the node which satisfy node.prev == start
     */
    private Node<T> getNext(final Node<T> start, Node<T> tail) {
        Node<T> next = tail;
        Node<T> curr = tail.prev;
        if (curr != null) curr.next = next;
        while (curr != start) {
            next = curr;
            curr = curr.prev;
            if (curr != null) curr.next = next;
        }
        return next;
    }
}
