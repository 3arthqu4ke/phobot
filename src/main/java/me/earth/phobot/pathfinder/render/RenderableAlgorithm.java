package me.earth.phobot.pathfinder.render;

import me.earth.phobot.pathfinder.algorithm.PathfindingNode;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a pathfinding process that can be rendered.
 * @see AlgorithmRenderer
 */
public interface RenderableAlgorithm<N extends PathfindingNode<N>> {
    /**
     * @return the starting point of the algorithm.
     */
    N getStart();

    /**
     * @return the starting goal of the algorithm.
     */
    N getGoal();

    /**
     * @return the node currently being checked.
     */
    @Nullable N getCurrent();

    /**
     * This method allows you, given {@link #getCurrent()}, to follow the current path this
     * algorithm is calculating back until {@link #getStart()}.
     *
     * @param node the node to get the previous node from.
     * @return the previous node for the given one.
     */
    @Nullable N getCameFrom(N node);

}
