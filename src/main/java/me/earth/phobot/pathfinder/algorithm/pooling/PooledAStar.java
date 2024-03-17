package me.earth.phobot.pathfinder.algorithm.pooling;

import me.earth.phobot.pathfinder.algorithm.AStar;
import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.pathfinder.util.OpenSet;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PooledAStar<N extends PoolNode<N>> extends AStar<N> {
    private final NodeParallelizationPooling pooling;
    protected int algorithmId;
    protected int index;

    public PooledAStar(N start, N goal, NodeParallelizationPooling pooling) {
        super(start, goal);
        this.pooling = pooling;
    }

    @Override
    public @Nullable Result<N> run(Cancellation cancellation) {
        try (NodeParallelizationPooling.PoolReference indexReference = pooling.requestIndex(cancellation)) {
            if (indexReference == null) {
                return null;
            }

            index = indexReference.getIndex();
            algorithmId = indexReference.getAlgorithmId();
            cameFrom = new PooledCameFromMap<>(index);
            start.setCurrentAlgorithmProperties(index, algorithmId);
            goal.setCurrentAlgorithmProperties(index, algorithmId);
            return super.run(cancellation);
        } catch (InterruptedException e) {
            cancellation.setCancelled(true);
            return null;
        }
    }

    @Override
    protected OpenSet<N> createOpenSet() {
        return new PooledBinaryHeap<>(index);
    }

    @Override
    protected void evaluate(N current, N neighbour) {
        current.setCurrentAlgorithmProperties(index, algorithmId);
        neighbour.setCurrentAlgorithmProperties(index, algorithmId);
        super.evaluate(current, neighbour);
    }

    @Override
    protected boolean hasFScore(N node) {
        return node.getScore(index) != Double.POSITIVE_INFINITY;
    }

    @Override
    protected void setFScore(N node, double f) {
        node.setScore(index, f);
    }

    @Override
    protected double getFScore(N node) {
        return node.getScore(index);
    }

    @Override
    protected void setGScore(N node, double g) {
        node.setGScore(index, g);
    }

    @Override
    protected double getGScore(N node) {
        return node.getGScore(index);
    }

}
