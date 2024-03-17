package me.earth.phobot.pathfinder.algorithm;

import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.pathfinder.util.OpenSet;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * An implementation of Dijkstra's algorithm for {@link PathfindingNode}s.
 * @param <N> the type of node to use for pathfinding.
 */
public class Dijkstra<N extends PathfindingNode<N>> extends AbstractAlgorithm<N> {
    protected final Map<N, Double> gScore = new HashMap<>();

    public Dijkstra(N start, N goal) {
        super(start, goal);
    }

    @Override
    public @Nullable Algorithm.Result<N> run(Cancellation cancellation) {
        setGScore(start, 0.0);
        return super.run(cancellation);
    }

    @Override
    protected OpenSet<N> createOpenSet() {
        return OpenSet.wrap(new TreeSet<>(Comparator.comparingDouble(this::getGScore).thenComparing(o -> o)));
    }

    @Override
    protected void evaluate(N current, N neighbour) {
        double tentative_gScore = getGScore(current) + getCost(current, neighbour);
        if (tentative_gScore < getGScore(neighbour)) {
            addDijkstra(current, neighbour, tentative_gScore);
        }
    }

    protected void setGScore(N node, double g) {
        gScore.put(node, g);
    }

    protected double getGScore(N node) {
        return gScore.getOrDefault(node, Double.POSITIVE_INFINITY);
    }

    protected double getCost(@SuppressWarnings("unused") N current, @SuppressWarnings("unused") N neighbour) {
        return 1.0;
    }

    protected void addDijkstra(N current, N neighbour, double tentative_gScore) {
        cameFrom.put(neighbour, current);
        if (gScore.containsKey(neighbour)) {
            openSet.update(neighbour, () -> gScore.put(neighbour, tentative_gScore));
        } else {
            gScore.put(neighbour, tentative_gScore);
            openSet.add(neighbour);
        }
    }

}
