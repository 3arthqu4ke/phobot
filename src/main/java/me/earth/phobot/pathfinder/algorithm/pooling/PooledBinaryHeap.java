package me.earth.phobot.pathfinder.algorithm.pooling;

import lombok.RequiredArgsConstructor;
import me.earth.phobot.pathfinder.util.OpenSet;
import org.jetbrains.annotations.Nullable;

/**
 * BinaryHeap implementation for {@link PoolNode}s based on {@link net.minecraft.world.level.pathfinder.BinaryHeap}.
 * @param <N> the type of node handled by this BinaryHeap.
 */
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class PooledBinaryHeap<N extends PoolNode<N>> implements OpenSet<N> {
    private final int poolIndex;

    private Object[] heap = new Object[1024];
    private int size;

    @Override
    public void add(N point) {
        if (this.size == this.heap.length) {
            Object[] nodes = new Object[this.size << 1];
            System.arraycopy(this.heap, 0, nodes, 0, this.size);
            this.heap = nodes;
        }

        this.heap[this.size] = point;
        point.setHeapIndex(poolIndex, this.size);
        this.upHeap(this.size++);
    }

    @Override
    public @Nullable N removeFirst() {
        N head = (N) this.heap[0];
        this.heap[0] = this.heap[--this.size];
        this.heap[this.size] = null;
        if (this.size > 0) {
            this.downHeap(0);
        }

        head.setHeapIndex(poolIndex, -1);
        return head;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void update(N point, Runnable actionThatUpdatesScore) {
        double costBeforeUpdate = point.getScore(poolIndex);
        actionThatUpdatesScore.run();
        double cost = point.getScore(poolIndex);
        if (cost < costBeforeUpdate) {
            this.upHeap(point.getHeapIndex(poolIndex));
        } else {
            this.downHeap(point.getHeapIndex(poolIndex));
        }
    }

    private void upHeap(int index) {
        N point = (N) this.heap[index];

        int j;
        for(double score = point.getScore(poolIndex); index > 0; index = j) {
            j = index - 1 >> 1;
            N biggerChild = (N) this.heap[j];
            if (score >= biggerChild.getScore(poolIndex)) {
                break;
            }

            this.heap[index] = biggerChild;
            biggerChild.setHeapIndex(poolIndex, index);
        }

        this.heap[index] = point;
        point.setHeapIndex(poolIndex, index);
    }

    private void downHeap(int index) {
        N point = (N) this.heap[index];
        double score = point.getScore(poolIndex);

        while(true) {
            int currentIndex = 1 + (index << 1);
            int nextIndex = currentIndex + 1;
            if (currentIndex >= this.size) {
                break;
            }

            N current = (N) this.heap[currentIndex];
            double currentScore = current.getScore(poolIndex);
            N next;
            double nextScore;
            if (nextIndex >= this.size) {
                next = null;
                nextScore = Float.POSITIVE_INFINITY;
            } else {
                next = (N) this.heap[nextIndex];
                nextScore = next.getScore(poolIndex);
            }

            if (currentScore < nextScore) {
                if (!(currentScore < score)) {
                    break;
                }

                this.heap[index] = current;
                current.setHeapIndex(poolIndex, index);
                index = currentIndex;
            } else {
                if (!(nextScore < score)) {
                    break;
                }

                this.heap[index] = next;
                next.setHeapIndex(poolIndex, index);
                index = nextIndex;
            }
        }

        this.heap[index] = point;
        point.setHeapIndex(poolIndex, index);
    }

}
