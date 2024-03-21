package me.earth.phobot.pathfinder.movement;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Introduces an additional Strafe node alongside each bunny hop node and vice versa every time we are on the ground.
 * Experiment to see if strafing to avoid a collision caused by a bhop jump into some block makes the path shorter.
 */
@Slf4j
public class SuperOptimizingNode extends OptimizingNode {
    final Map<MovementNode, OptimizingNode> nodes;

    public SuperOptimizingNode(Map<MovementNode, OptimizingNode> nodes, SplittingPointAlgorithm splitAlg, MovementPathfindingAlgorithm algorithm, MovementAlgorithmState state,
                               MovementNode movementNode, int horizontalCollisions, boolean bunnyHopping, boolean reachedMeshNode, int pathLength) {
        super(splitAlg, algorithm, state, movementNode, horizontalCollisions, bunnyHopping, reachedMeshNode, pathLength);
        this.nodes = nodes;
    }

    @Override
    protected OptimizingNode[] actualAdjacent(MovementPathfindingAlgorithm algorithm, SplittingPointAlgorithm splitAlg) {
        if (reachedMeshNode) {
            return split(splitAlg);
        } else {
            List<OptimizingNode> nodes = new ArrayList<>(movementNode.onGround() ? 2 : 1);
            state.copyInto(algorithm);
            OptimizingNode node = moveTick(algorithm, splitAlg);
            if (node != null && addToVisitedNodes(node)) {
                nodes.add(node);
            }

            if (movementNode.onGround()) {
                state.copyInto(algorithm);
                algorithm.strafe = !algorithm.strafe;
                OptimizingNode differentMoveNode = moveTick(algorithm, splitAlg);
                if (differentMoveNode != null && addToVisitedNodes(differentMoveNode)) {
                    nodes.add(differentMoveNode);
                }
            }

            return nodes.toArray(new OptimizingNode[0]);
        }
    }

    protected boolean addToVisitedNodes(OptimizingNode optimizingNode) {
        OptimizingNode node = nodes.get(optimizingNode.movementNode);
        boolean visitedSamePathLengthAlready = true;
        if (node == null
                || (visitedSamePathLengthAlready = node.pathLength > optimizingNode.pathLength)
                || !node.visitedSamePathLengthAlready && node.bunnyHopping != optimizingNode.bunnyHopping/* TODO: this might alternate?*/) {
            nodes.put(optimizingNode.movementNode, optimizingNode);
            optimizingNode.visitedSamePathLengthAlready = node != null && visitedSamePathLengthAlready;
            return true;
        }

        return false;
    }

    @Override
    protected OptimizingNode[] split(SplittingPointAlgorithm splitAlg) {
        if (movementNode.onGround()) {
            splitAlg.preMoveTick = () -> {
                MovementAlgorithmState before = splitAlg.copyState();
                splitAlg.strafe = !splitAlg.strafe;
                splitAlg.moveTickWithoutPreMoveTick();
                before.copyInto(splitAlg);
            };
        }

        OptimizingNode[] result = super.split(splitAlg);
        splitAlg.preMoveTick = () -> {};
        return result;
    }

    @Override
    protected void splitConsumer(MovementNode node, boolean jumpReachedMeshNode, List<OptimizingNode> nodes) {
        if (jumpReachedMeshNode) {
            algorithm.postMoveTick();
        }

        int collisions = node.snapshot().isHorizontalCollision() ? horizontalCollisions + 1 : horizontalCollisions;
        var optNode = newOptimizingNode(splitAlg, algorithm, splitAlg.copyState(), node, collisions, splitAlg.strafe, jumpReachedMeshNode, pathLength + 1);
        if (this.addToVisitedNodes(optNode)) {
            nodes.add(optNode);
        }
    }

    @Override
    public OptimizingNode newOptimizingNode(SplittingPointAlgorithm splitAlg, MovementPathfindingAlgorithm algorithm, MovementAlgorithmState state,
                                            MovementNode movementNode, int horizontalCollisions, boolean bunnyHopping, boolean reachedMeshNode, int pathLength) {
        return new SuperOptimizingNode(nodes, splitAlg, algorithm, state, movementNode, horizontalCollisions, bunnyHopping, reachedMeshNode, pathLength);
    }

    @Override
    public OptimizingNode newStartNode(SplittingPointAlgorithm splitAlg, MovementPathfindingAlgorithm algorithm, MovementAlgorithmState state,
                                       MovementNode movementNode, int horizontalCollisions, boolean bunnyHopping, boolean reachedMeshNode, int pathLength) {
        return new SuperOptimizingNode(nodes, splitAlg, algorithm, state, movementNode, horizontalCollisions, bunnyHopping, reachedMeshNode, pathLength) {
            @Override
            public OptimizingNode[] getAdjacent() {
                return this.split(splitAlg); // on the start node split only!
            }
        };
    }

    static OptimizingNodeFactory getSuperOptimizingNodeFactory() {
        return new SuperOptimizingNode(new HashMap<>(), null, null, null, null, 0, false, false, 0);
    }

}
