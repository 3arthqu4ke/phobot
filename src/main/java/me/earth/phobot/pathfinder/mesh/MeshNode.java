package me.earth.phobot.pathfinder.mesh;

import lombok.Getter;
import lombok.Setter;
import me.earth.phobot.invalidation.CanBeInvalidated;
import me.earth.phobot.invalidation.ChunkWorker;
import me.earth.phobot.pathfinder.algorithm.Abstract3iNode;
import net.minecraft.core.Vec3i;

/**
 * A node in a mesh managed by {@link NavigationMeshManager}s for pathfinding.
 * <p> a
 * <p> n
 * <p> b
 * <p>If b is a solid block, and n and non-collidable blocks (e.g. air), then n is a node.
 */
@Getter
@Setter
public class MeshNode extends Abstract3iNode<MeshNode> implements CanBeInvalidated {
    public static final int[] OPPOSITE_INDEX = {1, 0, 3, 2};
    public static final Vec3i[] OFFSETS = new Vec3i[] {
        new Vec3i(1, 0, 0),
        new Vec3i(-1, 0, 0),
        new Vec3i(0, 0, 1),
        new Vec3i(0, 0, -1)
    };

    // TODO: also track how "safe" (how many blocks around) a node are, and prefer strafing between bedrock blocks before strafing above

    private final MeshNode[] adjacent = new MeshNode[4];
    private final ChunkWorker chunk;
    private final int version;

    private boolean headSpace = false;
    private boolean valid = true;

    public MeshNode(ChunkWorker chunk, int x, int y, int z) {
        this(chunk, chunk.getVersion(), x, y, z);
    }

    public MeshNode(ChunkWorker chunk, int version, int x, int y, int z) {
        super(x, y, z);
        this.chunk = chunk;
        this.version = version;
    }

    @Override
    public boolean isValid() {
        return valid && getChunk().getVersion() == this.getVersion();
    }

    @Override
    public void invalidate() {
        setValid(false);
    }

}
