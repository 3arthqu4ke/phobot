package me.earth.phobot.pathfinder;

import lombok.Data;
import me.earth.phobot.invalidation.CanBeInvalidated;
import me.earth.phobot.pathfinder.algorithm.PathfindingNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

@Data
public class Path<T extends PathfindingNode<T>> implements CanBeInvalidated {
    private final Vec3 exactStart;
    private final Vec3 exactGoal;
    private final BlockPos start;
    private final BlockPos goal;
    private final @Unmodifiable List<T> path;
    private final Class<T> type;
    private boolean valid = true;

    @Override
    public void invalidate() {
        valid = false;
    }

    public boolean validate() {
        for (int i = 0; i < path.size(); i++) {
            T meshNode = path.get(i);
            if (!meshNode.isValid()) {
                return false;
            }

            if (i + 1 >= path.size()) {
                break;
            }

            T next = path.get(i + 1);
            boolean found = false;
            for (T adjacent : meshNode.getAdjacent()) {
                if (next.equals(adjacent)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

}
