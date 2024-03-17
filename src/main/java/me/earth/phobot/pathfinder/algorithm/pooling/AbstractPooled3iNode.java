package me.earth.phobot.pathfinder.algorithm.pooling;

import me.earth.phobot.pathfinder.algorithm.Abstract3iNode;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

public abstract class AbstractPooled3iNode<N extends AbstractPooled3iNode<N>> extends Abstract3iNode<N> implements PoolNode<N> {
    // TODO: this is potentially a "memory leak", as it could keep referencing nodes that have been invalidated and removed!
    // TODO: going through the XZMap in AbstractMeshInvalidationTask might not guarantee that its gonna get cleaned up!
    // First step for the TODOs has been made, which is the function cleanupCameFromPool
    private final N[] cameFromPool;

    private final int[] algorithmIndices;
    private final int[] heapIndexPool;
    private final double[] scorePool;
    private final double[] gScorePool;

    @SuppressWarnings("unchecked")
    public AbstractPooled3iNode(NodeParallelizationPooling pooling, int x, int y, int z) {
        super(x, y, z);
        int poolSize = pooling.getPoolSize();
        this.cameFromPool = (N[]) Array.newInstance(this.getClass(), poolSize);
        this.algorithmIndices = new int[poolSize];
        this.heapIndexPool = new int[poolSize];
        this.scorePool = new double[poolSize];
        this.gScorePool = new double[poolSize];
    }

    @Override
    public void setCurrentAlgorithmProperties(int poolIndex, int algorithmId) {
        if (algorithmIndices[poolIndex] != algorithmId) {
            algorithmIndices[poolIndex] = algorithmId;
            cameFromPool[poolIndex] = null;
            heapIndexPool[poolIndex] = -1;
            scorePool[poolIndex] = Double.POSITIVE_INFINITY;
            gScorePool[poolIndex] = Double.POSITIVE_INFINITY;
        }
    }

    @Override
    public N getCameFrom(int poolIndex) {
        return cameFromPool[poolIndex];
    }

    @Override
    public void setCameFrom(int poolIndex, @Nullable N node) {
        cameFromPool[poolIndex] = node;
    }

    @Override
    public int getHeapIndex(int poolIndex) {
        return heapIndexPool[poolIndex];
    }

    @Override
    public void setHeapIndex(int poolIndex, int heapIndex) {
        heapIndexPool[poolIndex] = heapIndex;
    }

    @Override
    public double getScore(int poolIndex) {
        return scorePool[poolIndex];
    }

    @Override
    public void setScore(int poolIndex, double score) {
        scorePool[poolIndex] = score;
    }

    @Override
    public double getGScore(int poolIndex) {
        return gScorePool[poolIndex];
    }

    @Override
    public void setGScore(int poolIndex, double gScore) {
        gScorePool[poolIndex] = gScore;
    }

    // TODO: still not perfect, theres a chance that an algorithm the invalid node later?
    // This risks an algorithm failing when collecting the path, but nothing we can really do about it
    public void cleanupCameFromPool() {
        for (int i = 0; i < cameFromPool.length; i++) {
            N node = cameFromPool[i];
            if (node != null && !node.isValid()) {
                cameFromPool[i] = null;
            }
        }
    }

}
