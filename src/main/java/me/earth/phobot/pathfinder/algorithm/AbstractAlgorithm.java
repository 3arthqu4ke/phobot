package me.earth.phobot.pathfinder.algorithm;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.pathfinder.blocks.BlockPathfinderAlgorithm;
import me.earth.phobot.pathfinder.movement.MovementPathfindingAlgorithm;
import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.pathfinder.util.OpenSet;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract base class for {@link Algorithm}s. Represents a pathfinding algorithm that finds a path of {@link PathfindingNode}s.
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
public abstract class AbstractAlgorithm<N extends PathfindingNode<N>> implements Algorithm<N> {
    @Getter(AccessLevel.NONE)
    private final AtomicBoolean hasRun = new AtomicBoolean();
    protected final N start;
    protected final N goal;
    protected Map<N, @Nullable N> cameFrom = createCameFrom();
    protected OpenSet<N> openSet;
    protected N current;
    public int loops = 0;

    @Override
    public @Nullable N getCameFrom(N node) {
        return cameFrom.get(node);
    }

    /**
     * @param cancellation if {@link Cancellation#isCancelled()} is true for this, this method will abort and might return {@code null}.
     * @return a path of nodes in <strong>reverse order</strong>!
     */
    @Override
    public @Nullable Algorithm.Result<N> run(Cancellation cancellation) {
        try {
            synchronized (hasRun) {
                if (hasRun.getAndSet(true)) {
                    throw new IllegalStateException("Algorithm " + this + " has run twice");
                }
            }

            if (start.equals(goal)) {
                return null;
            }

            //long startTime = TimeUtil.getMillis();
            openSet = createOpenSet();
            cameFrom.put(start, null);
            openSet.add(start);
            cancellation.init();
            // TODO: prevent out of memory problems?
            while (!openSet.isEmpty() && !cancellation.isCancelled()) {
                current = openSet.removeFirst();
                assert current != null;
                if (isGoal(current, goal)) {
                    return buildResult(cancellation, current);
                }

                loops++;
                for (N neighbor : current.getAdjacent()) {
                    if (neighbor != null && isValid(current, neighbor)) {
                        evaluate(current, neighbor);
                    }
                }
            }

            return null;
        } catch (Throwable throwable) {
            log.error("Error occurred in Algorithm " + this, throwable);
            throwable.printStackTrace(); // TODO: investigate ArrayIndexOutOfBounds in PooledAStar? Cannot recreate?!
            throw throwable;
        }
    }

    protected @Nullable Algorithm.Result<N> buildResult(Cancellation cancellation, N current) {
        List<N> nodes = new ArrayList<>();
        while (!cancellation.isCancelled()) {
            nodes.add(current);
            if (current == null) {
                throw new IllegalStateException("current was null, type: " + start.getClass());
            }

            if (current.equals(start)) //noinspection CommentedOutCode
            {
                /*if (goal != null) {
                    long timeNeeded = TimeUtil.getPassedTimeSince(startTime);
                    double distance = round(start.distance(goal), 1);
                    double hDistance = round(sqrt(distance2dSq(start.getRenderX(), start.getRenderZ(), goal.getRenderX(), goal.getRenderZ())), 1);
                    double vDistance = round(abs(start.getRenderY() - goal.getRenderY()), 1);
                    log.info("A* took {}ms for a distance of {}m, h: {}m, v: {}m, nodes: {}", timeNeeded, distance, hDistance, vDistance, nodes.size());
                }*/
                return new Algorithm.Result<>(nodes, Algorithm.Result.Order.GOAL_TO_START);
            }

            current = cameFrom.get(current);
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

    protected Map<N, @Nullable N> createCameFrom() {
        return new HashMap<>();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{cameFrom=" + cameFrom.size() +
                ", start=" + start +
                ", goal=" + goal +
                ", openSet=" + (openSet == null ? 0 : openSet.size()) +
                '}';
    }

}
