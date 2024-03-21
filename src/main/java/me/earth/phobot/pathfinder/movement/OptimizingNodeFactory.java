package me.earth.phobot.pathfinder.movement;

public interface OptimizingNodeFactory {
    default OptimizingNode newOptimizingNode(SplittingPointAlgorithm splitAlg, MovementPathfindingAlgorithm algorithm, MovementAlgorithmState state,
                                     MovementNode movementNode, int horizontalCollisions, boolean bunnyHopping, boolean reachedMeshNode, int pathLength) {
        return new OptimizingNode(splitAlg, algorithm, state, movementNode, horizontalCollisions, bunnyHopping, reachedMeshNode, pathLength);
    }

    default OptimizingNode newStartNode(SplittingPointAlgorithm splitAlg, MovementPathfindingAlgorithm algorithm, MovementAlgorithmState state,
                                        MovementNode movementNode, int horizontalCollisions, boolean bunnyHopping, boolean reachedMeshNode, int pathLength) {
        return new OptimizingNode(splitAlg, algorithm, state, movementNode, horizontalCollisions, bunnyHopping, reachedMeshNode, pathLength) {
            @Override
            public OptimizingNode[] getAdjacent() {
                return super.split(splitAlg); // on the start node split only!
            }
        };
    }

}
