package me.earth.phobot.pathfinder.movement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.pathfinder.algorithm.pooling.PoolNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class OptimizingNode implements PoolNode<OptimizingNode>, OptimizingNodeFactory {
    public static final OptimizingNodeFactory OPTIMIZING_NODE_FACTORY = new OptimizingNode(null, null, null, null, 0, false, false, 0);

    final SplittingPointAlgorithm splitAlg;
    final MovementPathfindingAlgorithm algorithm;
    final MovementAlgorithmState state;
    final MovementNode movementNode;
    final int horizontalCollisions;
    final boolean bunnyHopping;
    final boolean reachedMeshNode;
    final int pathLength;

    @Nullable OptimizingNode previous;
    int heapIndex = -1;
    double score = Double.POSITIVE_INFINITY;
    double gScore = Double.POSITIVE_INFINITY;
    boolean visitedSamePathLengthAlready;

    @Override
    public OptimizingNode[] getAdjacent() {
        return actualAdjacent(algorithm, splitAlg);
    }

    protected OptimizingNode[] actualAdjacent(MovementPathfindingAlgorithm algorithm, SplittingPointAlgorithm splitAlg) {
        if (reachedMeshNode) {
            return split(splitAlg);
        } else {
            state.copyInto(algorithm);
            OptimizingNode node = moveTick(algorithm, splitAlg);
            return node == null ? new OptimizingNode[0] : new OptimizingNode[]{node};
        }
    }

    protected OptimizingNode[] split(SplittingPointAlgorithm splitAlg) {
        // TODO: find new neighbors in any direction!
        List<OptimizingNode> nodes = new ArrayList<>();
        state.copyInto(splitAlg);
        if (state.currentJump != null) {
            state.currentJump.apply(splitAlg.getPlayer());
        }

        splitAlg.angles.clear();
        splitAlg.moveConsumer = (node, jumpReachedMeshNode) -> {
            splitConsumer(node, jumpReachedMeshNode, nodes);
            state.copyInto(splitAlg);
        };

        updateSplitAlg(splitAlg);
        return nodes.toArray(new OptimizingNode[0]);
    }

    protected void updateSplitAlg(SplittingPointAlgorithm splitAlg) {
        splitAlg.update();
    }

    protected void splitConsumer(MovementNode node, boolean jumpReachedMeshNode, List<OptimizingNode> nodes) {
        if (jumpReachedMeshNode) {
            algorithm.postMoveTick();
        }

        int collisions = node.snapshot().isHorizontalCollision() ? horizontalCollisions + 1 : horizontalCollisions;
        var optNode = newOptimizingNode(splitAlg, algorithm, splitAlg.copyState(), node, collisions, !splitAlg.strafe, jumpReachedMeshNode, pathLength + 1);
        nodes.add(optNode);
    }

    protected @Nullable OptimizingNode moveTick(MovementPathfindingAlgorithm algorithm, SplittingPointAlgorithm splitAlg) {
        assert state != null;
        state.currentJump.apply(algorithm.getPlayer());
        boolean reachedMeshNode = false;
        while (algorithm.currentJump == this.movementNode) {
            Boolean tickResult = algorithm.moveTick();
            if (tickResult != null) {
                if (!tickResult) {
                    return null;
                } else {
                    reachedMeshNode = true;
                    algorithm.postMoveTick();
                    break;
                }
            }
        }

        if (algorithm.currentJump == null) {
            return null;
        }

        int collisions = algorithm.currentJump.snapshot().isHorizontalCollision() ? horizontalCollisions + 1 : horizontalCollisions;
        return newOptimizingNode(splitAlg, algorithm, algorithm.copyState(), algorithm.currentJump, collisions, !algorithm.strafe, reachedMeshNode, pathLength + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptimizingNode that = (OptimizingNode) o;
        return movementNode.positionEquals(that.movementNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(movementNode.positionHashCode());
    }

    @Override
    public int compareTo(@NotNull OptimizingNode o) {
        return this.movementNode.compareTo(o.movementNode);
    }

    @Override
    public double getRenderX() {
        return movementNode.getRenderX();
    }

    @Override
    public double getRenderY() {
        return movementNode.getRenderY();
    }

    @Override
    public double getRenderZ() {
        return movementNode.getRenderZ();
    }

    @Override
    public int getChunkX() {
        return movementNode.getChunkX();
    }

    @Override
    public int getChunkZ() {
        return movementNode.getChunkZ();
    }

    @Override
    public double distanceSq(OptimizingNode node) {
        return movementNode.distanceSq(node.movementNode);
    }

    @Override
    public void setCurrentAlgorithmProperties(int poolIndex, int algorithmId) {

    }

    @Override
    public OptimizingNode getCameFrom(int poolIndex) {
        return previous;
    }

    @Override
    public void setCameFrom(int poolIndex, @Nullable OptimizingNode node) {
        this.previous = node;
    }

    @Override
    public int getHeapIndex(int poolIndex) {
        return heapIndex;
    }

    @Override
    public void setHeapIndex(int poolIndex, int heapIndex) {
        this.heapIndex = heapIndex;
    }

    @Override
    public double getScore(int poolIndex) {
        return score;
    }

    @Override
    public void setScore(int poolIndex, double score) {
        this.score = score;
    }

    @Override
    public double getGScore(int poolIndex) {
        return gScore;
    }

    @Override
    public void setGScore(int poolIndex, double gScore) {
        this.gScore = gScore;
    }

}
