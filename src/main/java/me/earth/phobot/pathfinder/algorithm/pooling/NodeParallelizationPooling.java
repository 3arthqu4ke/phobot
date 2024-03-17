package me.earth.phobot.pathfinder.algorithm.pooling;

import lombok.Data;
import lombok.Getter;
import me.earth.phobot.pathfinder.Pathfinder;
import me.earth.phobot.pathfinder.algorithm.AStar;
import me.earth.phobot.pathfinder.algorithm.AbstractAlgorithm;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.algorithm.Dijkstra;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.util.Cancellation;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: this seems to be A LOT faster, try to migrate to this everywhere? (BlockPathfinder, Dijkstra, etc.)?
/**
 * Every {@link MeshNode} is immutable.
 * This allows us to run multiple {@link Algorithm}s in parallel on the same mesh.
 * <p>However, this comes with a drawback for Algorithm design: we cannot store any information inside the node itself.
 * Algorithms now need to use Maps to lookup information they would usually store within a node, this regards:
 * <ul>
 * <li>the {@link AbstractAlgorithm#getCameFrom()} map, with mutable nodes we could just give every node a 'previous' field.</li>
 * <li>the {@link AbstractAlgorithm#getOpenSet()} you would use a BinaryHeap and store the index the node has inside that heap,
 * inside the node itself, allowing for quick updates.</li>
 * <li>{@link Dijkstra}s gScore</li>
 * <li>{@link AStar}s fScore</li>
 * </ul>
 * The goal of this is to give our nodes these properties.
 * Instead of a single 'previous' and 'heapIndex' field, each Node gets an array of these.
 * When an algorithm runs, it requests an index for this array.
 * As long as this index is owned by an algorithm it cannot be used by another algorithm,
 * allowing the running algorithm to safely store their information inside each node at the given index.
 * @see PoolNode
 * @see PooledAStar
 */
public class NodeParallelizationPooling {
    @VisibleForTesting
    final Set<Integer> indicesInUse = Collections.newSetFromMap(new ConcurrentHashMap<>());
    @VisibleForTesting
    final Set<Integer> openIndices = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final AtomicInteger algorithmIds = new AtomicInteger();
    private final Object lock = new Object();
    @Getter
    private final int poolSize;

    public NodeParallelizationPooling(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size for HeapIndexAndCameFromPooling was " + size);
        }

        this.poolSize = size;
        for (int i = 0; i < size; i++) {
            openIndices.add(i);
        }
    }

    public @Nullable NodeParallelizationPooling.PoolReference requestIndex(Cancellation cancellation) throws InterruptedException {
        synchronized (lock) {
            while (!cancellation.isCancelled()) {
                Optional<Integer> index = openIndices.stream().findFirst();
                if (index.isEmpty()) {
                    lock.wait(Pathfinder.DEFAULT_TIME_OUT + 10); // periodically check cancellation.isCancelled() // TODO: worth it? use Thread interruptions?
                    continue;
                }

                openIndices.remove(index.get());
                indicesInUse.add(index.get());
                return new PoolReference(algorithmIds.incrementAndGet(), index.get());
            }
        }

        cancellation.setCancelled(true);
        return null;
    }

    @Data
    public class PoolReference implements AutoCloseable {
        private final int algorithmId;
        private final int index;

        @Override
        public void close() {
            synchronized (lock) {
                indicesInUse.remove(index);
                openIndices.add(index);
                lock.notify();
            }
        }
    }

}
