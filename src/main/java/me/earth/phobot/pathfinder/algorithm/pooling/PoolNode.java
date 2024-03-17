package me.earth.phobot.pathfinder.algorithm.pooling;

import me.earth.phobot.pathfinder.algorithm.PathfindingNode;
import org.jetbrains.annotations.Nullable;

public interface PoolNode<N extends PathfindingNode<N>> extends PathfindingNode<N> {
    void setCurrentAlgorithmProperties(int poolIndex, int algorithmId);

    N getCameFrom(int poolIndex);

    void setCameFrom(int poolIndex, @Nullable N node);

    int getHeapIndex(int poolIndex);

    void setHeapIndex(int poolIndex, int heapIndex);

    double getScore(int poolIndex);

    void setScore(int poolIndex, double score);

    double getGScore(int poolIndex);

    void setGScore(int poolIndex, double gScore);

}
