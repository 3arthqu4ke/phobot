package me.earth.phobot.pathfinder.movement;


import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.algorithm.pooling.PooledAStar;
import me.earth.phobot.pathfinder.util.Cancellation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link MovementPathfindingAlgorithm} which optimizes the movement on a given path.
 * The normal algorithm just finds the first path that reaches the goal.
 * The goal of this algorithm is to find the best path that reaches the goal.
 */
public final class OptimizingMovementPathfindingAlgorithm extends PooledAStar<OptimizingNode> {
    public OptimizingMovementPathfindingAlgorithm(OptimizingNode start, OptimizingNode goal) {
        super(start, goal, DummyPooling.INSTANCE);
    }

    @Override
    protected double heuristic(OptimizingNode goal, OptimizingNode node) {
        //System.out.println("Checking " + node.distance(goal) + ": " + node.horizontalCollisions + " " + node);
        // node.distance(goal) * node.horizontalCollisions;
        return node.distance(goal) * node.horizontalCollisions;
    }

    @Override
    protected double getCost(OptimizingNode current, OptimizingNode neighbour) {
        //return super.getCost(current, neighbour);
        //return neighbour.pathLength - current.pathLength;
        return neighbour.pathLength - current.pathLength;
    }

    @Override
    protected @Nullable Algorithm.Result<OptimizingNode> buildResult(Cancellation cancellation, OptimizingNode current) {
        List<OptimizingNode> nodes = new ArrayList<>();
        while (!cancellation.isCancelled()) {
            nodes.add(current);
            if (current == null) {
                throw new IllegalStateException("current was null, type: " + start.getClass());
            }

            if (current.equals(start)) {
                return new Algorithm.Result<>(nodes, Algorithm.Result.Order.GOAL_TO_START);
            }

            OptimizingNode previous = current.previous;
            while (previous != null && previous.movementNode == current.movementNode) { // can happen, the MovementPathfindingAlgorithm might assign the same movementNode twice
                previous = previous.previous;
            }

            current = previous;
        }

        return null;
    }

    public static OptimizingMovementPathfindingAlgorithm optimize(MovementPathfindingAlgorithm alg) {
        return optimize(alg, OptimizingNode.OPTIMIZING_NODE_FACTORY);
    }

    public static OptimizingMovementPathfindingAlgorithm superOptimize(MovementPathfindingAlgorithm alg) {
        return optimize(alg, SuperOptimizingNode.getSuperOptimizingNodeFactory());
    }

    public static OptimizingMovementPathfindingAlgorithm superOptimizeSingleDirectPath(MovementPathfindingAlgorithm alg) {
        return optimize(alg, SuperSinglePathOptimizingNode.getSuperSinglePathOptimizingNodeFactory());
    }

    public static OptimizingMovementPathfindingAlgorithm optimize(MovementPathfindingAlgorithm alg, OptimizingNodeFactory factory) {
        MovementPathfindingAlgorithm copy = new MovementPathfindingAlgorithm(alg.getPhobot(), alg.getLevel(), alg.getPath(), alg.getPlayer(), alg.getStart(), alg.getGoal());
        SplittingPointAlgorithm split = new SplittingPointAlgorithm(copy);
        OptimizingNode goal = factory.newOptimizingNode(split, copy, split.copyState(), split.getGoal(), 0, false, false, 0);
        OptimizingNode start = factory.newStartNode(split, copy, split.copyState(), split.getStart(), 0, false, false, 0);
        return new OptimizingMovementPathfindingAlgorithm(start, goal);
    }

}
