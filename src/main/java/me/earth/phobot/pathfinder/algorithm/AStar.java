package me.earth.phobot.pathfinder.algorithm;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.pathfinder.util.OpenSet;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * An implementation of the A-Star algorithm for {@link PathfindingNode}s.
 * @param <N> the type of node to use for pathfinding.
 */
@Slf4j
public class AStar<N extends PathfindingNode<N>> extends Dijkstra<N> {
    protected final Map<N, Double> fScore = new HashMap<>();

    public AStar(N start, N goal) {
        super(start, goal);
    }

    @Override
    public @Nullable Algorithm.Result<N> run(Cancellation cancellation) {
        setFScore(start, heuristic(goal, start));
        return super.run(cancellation);
    }

    @Override
    protected OpenSet<N> createOpenSet() {
        return OpenSet.wrap(new TreeSet<>(Comparator.comparingDouble(this::getFScore).thenComparing(o -> heuristic(goal, o)).thenComparing(o -> o)));
    }

    @Override
    protected void evaluate(N current, N neighbour) {
        double tentative_gScore = getGScore(current) + getCost(current, neighbour);
        if (tentative_gScore < getGScore(neighbour)) {
            cameFrom.put(neighbour, current);
            setGScore(neighbour, tentative_gScore);
            if (hasFScore(neighbour)) {
                openSet.update(neighbour, () -> setFScore(neighbour, tentative_gScore + heuristic(goal, neighbour)));
            } else {
                setFScore(neighbour, tentative_gScore + heuristic(goal, neighbour));
                openSet.add(neighbour);
            }
        }
    }

    protected double heuristic(N goal, N node) {
        return goal.distance(node);
    }

    protected boolean hasFScore(N node) {
        return fScore.containsKey(node);
    }

    protected void setFScore(N node, double f) {
        fScore.put(node, f);
    }

    protected double getFScore(N node) {
        return fScore.get(node);
    }

}
