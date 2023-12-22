package me.earth.phobot.pathfinder;

import lombok.Data;
import me.earth.phobot.invalidation.CanBeInvalidated;
import me.earth.phobot.pathfinder.algorithm.PathfindingNode;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

@Data
public class Path<T extends PathfindingNode<T>> implements CanBeInvalidated {
    private static final Path<?> INVALID = new Path<>(Vec3.ZERO, Vec3.ZERO, BlockPos.ZERO, BlockPos.ZERO, emptySet(), emptyList(), MeshNode.class) {
        @Override
        public boolean isValid() {
            return false;
        }
    };

    private final Vec3 exactStart;
    private final Vec3 exactGoal;
    private final BlockPos start;
    private final BlockPos goal;
    // TODO: remove?
    private final @Unmodifiable Set<BlockPos> affectedPositions;
    private final @Unmodifiable List<T> path;
    private final Class<T> type;
    private boolean valid = true;

    @Override
    public void invalidate() {
        valid = false;
    }

    @SuppressWarnings("unchecked")
    public static <T extends PathfindingNode<T>> Path<T> invalid() {
        return (Path<T>) INVALID;
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

    public static @Nullable Path<MeshNode> createAndValidate(List<MeshNode> path, Vec3 exactStart, Vec3 exactGoal) {
        for (int i = 0; i < path.size(); i++) {
            MeshNode meshNode = path.get(i);
            if (!meshNode.isValid()) {
                return null;
            }

            if (i + 1 >= path.size()) {
                break;
            }

            MeshNode next = path.get(i + 1);
            boolean found = false;
            for (MeshNode adjacent : meshNode.getAdjacent()) {
                if (adjacent.equals(next)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return null;
            }
        }

        return null;
    }

}
