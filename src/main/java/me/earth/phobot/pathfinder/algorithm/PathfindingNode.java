package me.earth.phobot.pathfinder.algorithm;

import me.earth.phobot.pathfinder.mesh.MeshNode;

/**
 * Represents a node in a graph that we can use in a pathfinding {@link Algorithm}, such as {@link AStar} or {@link Dijkstra}.
 *
 * @param <N> the self typing of this node, should always be the own type.
 */
public interface PathfindingNode<N extends PathfindingNode<N>> extends Comparable<N> {
    /**
     * @return the nodes this node is connected to.
     */
    N[] getAdjacent();

    /**
     * @return an x coordinate for visualizing this node in 3-dimensions.
     */
    double getRenderX();

    /**
     * @return a y coordinate for visualizing this node in 3-dimensions.
     */
    double getRenderY();

    /**
     * @return a z coordinate for visualizing this node in 3-dimensions.
     */
    double getRenderZ();

    /**
     * @return the x chunk coordinate for this node, for finding this node in 2-dimensions.
     */
    int getChunkX();

    /**
     * @return the z chunk coordinate for this node, for finding this node in 2-dimensions.
     */
    int getChunkZ();

    /**
     * Calculates the square of the Euclidean distance between this node and another node of the same type.
     *
     * @param node the node to get the distance to.
     * @return the square of the Euclidean distance between this node and the other node.
     */
    double distanceSq(N node);

    /**
     * Calculates the Euclidean distance between this node and another node of the same type.
     *
     * @param node the node to get the distance to.
     * @return the Euclidean distance between this node and the other node.
     */
    default double distance(N node) {
        return Math.sqrt(distanceSq(node));
    }

    /**
     * Since pathfinding can happen asynchronously, it is possible that a node gets invalidated while we are pathfinding.
     * This method returns whether a node is valid or has been invalidated.
     *
     * @return a boolean signalizing whether this node is currently valid or not.
     * @see MeshNode#isValid()
     */
    default boolean isValid() {
        return true;
    }

}
