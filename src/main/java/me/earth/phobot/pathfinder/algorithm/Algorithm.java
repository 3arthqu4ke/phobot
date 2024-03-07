package me.earth.phobot.pathfinder.algorithm;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.pathfinder.blocks.BlockPathfinderAlgorithm;
import me.earth.phobot.pathfinder.movement.MovementPathfindingAlgorithm;
import me.earth.phobot.pathfinder.render.RenderableAlgorithm;
import me.earth.phobot.pathfinder.util.CancellableTask;
import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.pathfinder.util.OpenSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a pathfinding algorithm that finds a path of {@link PathfindingNode}s.
 *
 * @param <N> the type of PathfindingNode this Algorithm can operate on.
 * @see AStar
 * @see Dijkstra
 * @see BlockPathfinderAlgorithm
 * @see MovementPathfindingAlgorithm
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public abstract class Algorithm<N extends PathfindingNode<N>> implements RenderableAlgorithm<N>, CancellableTask<Algorithm.@Nullable Result<N>> {
    @Getter(AccessLevel.NONE)
    private final AtomicBoolean hasRun = new AtomicBoolean();
    protected final Map<N, @Nullable N> cameFrom = new HashMap<>();
    protected final N start;
    protected final N goal;
    protected OpenSet<N> openSet;
    protected N current;

    @Override
    public @Nullable N getCameFrom(N node) {
        return cameFrom.get(node);
    }

    /**
     * @param cancellation if {@link Cancellation#isCancelled()} is true for this, this method will abort and might return {@code null}.
     * @return a path of nodes in <strong>reverse order</strong>!
     */
    @Override
    public @Nullable Result<N> run(Cancellation cancellation) {
        synchronized (hasRun) {
            if (hasRun.getAndSet(true)) {
                throw new IllegalStateException("Algorithm " + this + " has run twice");
            }
        }

        if (start.equals(goal)) {
            return null;
        }

        openSet = createOpenSet();
        cameFrom.put(start, null);
        openSet.add(start);
        cancellation.init();
        while (!openSet.isEmpty() && !cancellation.isCancelled()) {
            current = openSet.removeFirst();
            assert current != null;
            if (isGoal(current, goal)) {
                List<N> nodes = new ArrayList<>();
                while (!cancellation.isCancelled()) {
                    nodes.add(current);
                    if (current == null) {
                        throw new IllegalStateException("current was null, type: " + start.getClass());
                    }

                    if (current.equals(start)) {
                        return new Algorithm.Result<>(nodes, Result.Order.GOAL_TO_START);
                    }

                    current = cameFrom.get(current);
                }
            }

            for (N neighbor : current.getAdjacent()) {
                if (neighbor != null && isValid(current, neighbor)) {
                    evaluate(current, neighbor);
                }
            }
        }

        return null;
    }

    protected abstract OpenSet<N> createOpenSet();

    protected abstract void evaluate(N current, N neighbour);

    protected boolean isValid(N current, N neighbour) {
        return neighbour.isValid();
    }

    protected boolean isGoal(N current, N goal) {
        return current.equals(goal);
    }

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor(access = AccessLevel.NONE)
    public static class Result<N extends PathfindingNode<N>> {
        private final List<N> path;
        private Order order;

        public Result<N> order(Order order) {
            if (this.order != order) {
                this.order = order;
                Collections.reverse(path);
            }

            return this;
        }

        public enum Order {
            GOAL_TO_START,
            START_TO_GOAL
        }
    }

}
