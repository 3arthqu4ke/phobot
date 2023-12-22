package me.earth.phobot.pathfinder.algorithm;

import org.jetbrains.annotations.Nullable;

public interface RenderableAlgorithm<N extends PathfindingNode<N>> {
    N getStart();

    N getGoal();

    @Nullable N getCurrent();

    @Nullable N getCameFrom(N node);

}
