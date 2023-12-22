package me.earth.phobot.pathfinder.algorithm;

import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.pathfinder.util.OpenSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AStar<N extends PathfindingNode<N>> extends Dijkstra<N> {
    protected final Map<N, Double> fScore = new HashMap<>();

    public AStar(N start, N goal) {
        super(start, goal);
    }

    @Override
    public @Nullable List<N> run(Cancellation cancellation) {
        fScore.put(start, heuristic(goal, start));
        return super.run(cancellation);
    }

    @Override
    protected OpenSet<N> createOpenSet() {
        return OpenSet.wrap(new TreeSet<>(Comparator.comparingDouble((N o) -> fScore.get(o)).thenComparing(o -> heuristic(goal, o)).thenComparing(o -> o)));
    }

    @Override
    protected void evaluate(N current, N neighbour) {
        double tentative_gScore = gScore.getOrDefault(current, Double.POSITIVE_INFINITY) + getCost(current, neighbour);
        if (tentative_gScore < gScore.getOrDefault(neighbour, Double.POSITIVE_INFINITY)) {
            cameFrom.put(neighbour, current);
            gScore.put(neighbour, tentative_gScore);
            if (fScore.containsKey(neighbour)) {
                openSet.update(neighbour, () -> fScore.put(neighbour, tentative_gScore + heuristic(goal, neighbour)));
            } else {
                fScore.put(neighbour, tentative_gScore + heuristic(goal, neighbour));
                openSet.add(neighbour);
            }
        }
    }

    protected double heuristic(N goal, N node) {
        return goal.distance(node);
    }

}
