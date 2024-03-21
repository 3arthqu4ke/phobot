package me.earth.phobot.pathfinder.movement;

import java.util.HashMap;
import java.util.Map;

public class SuperSinglePathOptimizingNode extends SuperOptimizingNode {
    public SuperSinglePathOptimizingNode(Map<MovementNode, OptimizingNode> nodes, SplittingPointAlgorithm splitAlg, MovementPathfindingAlgorithm algorithm,
                                         MovementAlgorithmState state, MovementNode movementNode, int collisions, boolean bunnyHopping, boolean reachedMeshNode, int l) {
        super(nodes, splitAlg, algorithm, state, movementNode, collisions, bunnyHopping, reachedMeshNode, l);
    }

    @Override
    protected void updateSplitAlg(SplittingPointAlgorithm splitAlg) {
        splitAlg.bunnyHopTowards(splitAlg.getPath().getPath().size() - 1); // bunny hop only towards goal
    }

    @Override
    public OptimizingNode newOptimizingNode(SplittingPointAlgorithm splitAlg, MovementPathfindingAlgorithm algorithm, MovementAlgorithmState state, MovementNode movementNode,
                                            int horizontalCollisions, boolean bunnyHopping, boolean reachedMeshNode, int pathLength) {
        return new SuperSinglePathOptimizingNode(nodes, splitAlg, algorithm, state, movementNode, horizontalCollisions, bunnyHopping, reachedMeshNode, pathLength);
    }

    @Override
    public OptimizingNode newStartNode(SplittingPointAlgorithm splitAlg, MovementPathfindingAlgorithm algorithm, MovementAlgorithmState state, MovementNode movementNode,
                                       int horizontalCollisions, boolean bunnyHopping, boolean reachedMeshNode, int pathLength) {
        return new SuperSinglePathOptimizingNode(nodes, splitAlg, algorithm, state, movementNode, horizontalCollisions, bunnyHopping, reachedMeshNode, pathLength) {
            @Override
            public OptimizingNode[] getAdjacent() {
                return super.split(splitAlg);
            }
        };
    }

    static OptimizingNodeFactory getSuperSinglePathOptimizingNodeFactory() {
        return new SuperSinglePathOptimizingNode(new HashMap<>(), null, null, null, null, 0, false, false, 0);
    }

}
