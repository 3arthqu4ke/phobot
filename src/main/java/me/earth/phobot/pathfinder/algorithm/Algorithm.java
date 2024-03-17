package me.earth.phobot.pathfinder.algorithm;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.earth.phobot.pathfinder.blocks.BlockPathfinderAlgorithm;
import me.earth.phobot.pathfinder.movement.MovementPathfindingAlgorithm;
import me.earth.phobot.pathfinder.render.RenderableAlgorithm;
import me.earth.phobot.pathfinder.util.CancellableTask;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Represents a pathfinding algorithm that finds a path of {@link PathfindingNode}s.
 *
 * @param <N> the type of PathfindingNode this Algorithm can operate on.
 * @see AStar
 * @see Dijkstra
 * @see BlockPathfinderAlgorithm
 * @see MovementPathfindingAlgorithm
 */
public interface Algorithm<N extends PathfindingNode<N>> extends RenderableAlgorithm<N>, CancellableTask<Algorithm.@Nullable Result<N>> {
    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor(access = AccessLevel.NONE)
    class Result<N extends PathfindingNode<N>> {
        private final List<N> path;
        private Order order;

        public Result<N> order(Order order) {
            if (this.order != order) {
                this.order = order;
                Collections.reverse(path);
            }

            return this;
        }

        public enum Order {
            GOAL_TO_START,
            START_TO_GOAL
        }
    }

}
