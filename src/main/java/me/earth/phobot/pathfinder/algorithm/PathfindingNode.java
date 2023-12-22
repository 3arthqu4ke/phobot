package me.earth.phobot.pathfinder.algorithm;

public interface PathfindingNode<N extends PathfindingNode<N>> extends Comparable<N> {
    N[] getAdjacent();

    double getRenderX();

    double getRenderY();

    double getRenderZ();

    int getChunkX();

    int getChunkZ();

    double distanceSq(N node);

    default double distance(N node) {
        return Math.sqrt(distanceSq(node));
    }

    default boolean isValid() {
        return true;
    }

}
