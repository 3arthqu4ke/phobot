package me.earth.phobot.pathfinder.mesh;

import me.earth.phobot.holes.HoleBlocks;
import me.earth.phobot.holes.HoleOffsets;
import me.earth.phobot.invalidation.AbstractInvalidationTask;
import me.earth.phobot.invalidation.ChunkWorker;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.mutables.MutPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Calculates the mesh in a given area.
 */
public class MeshTask extends AbstractInvalidationTask<MeshNode, NavigationMeshManager> implements HoleBlocks, HoleOffsets {
    public MeshTask(BlockableEventLoop<Runnable> scheduler, LevelChunk chunk, ChunkWorker worker, int h, int minH, NavigationMeshManager manager) {
        super(scheduler, chunk, worker, h, minH, manager);
    }

    public MeshTask(Map<BlockPos, MeshNode> map, BlockableEventLoop<Runnable> scheduler, Level level, MutPos pos, NavigationMeshManager manager,
                    @Nullable ChunkWorker chunk, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        super(map, scheduler, level, pos, manager, chunk, minX, maxX, minY, maxY, minZ, maxZ);
    }

    @Override
    protected void handleOverwritten(BlockPos pos, MeshNode overwritten, @Nullable MeshNode by) {
        overwritten.invalidate();
        removeFromXZMap(overwritten);
        // if we overwrite a node we need to reconnect it to the new node
        overwriteAdjacent(overwritten, by);
    }

    protected void overwriteAdjacent(MeshNode overwritten, @Nullable MeshNode by) {
        for (int i = 0; i < MeshNode.OFFSETS.length; i++) {
            if (by != null && by.getAdjacent()[i] == null) { // probably at chunk border
                by.getAdjacent()[i] = overwritten.getAdjacent()[i];
            }

            Vec3i offset = MeshNode.OFFSETS[i];
            for (MeshNode adjacent : getManager().getXZMap().getOrDefault(overwritten.getX() + offset.getX(), overwritten.getZ() + offset.getZ(), Collections.emptySet())) {
                if (adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]] == overwritten) {
                    adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[i]] = by;
                }
            }
        }
    }

    protected void removeFromXZMap(MeshNode node) {
        Set<MeshNode> nodes = getManager().getXZMap().get(node.getX(), node.getZ());
        if (nodes != null) {
            nodes.remove(node);
        }
    }

    @Override
    protected void finishTask() {
        super.finishTask();
        getMap().forEach((pos, value) -> getManager().getXZMap().computeIfAbsent(pos, v -> new HashSet<>()).add(value));
    }

    @Override
    public void calc(MutPos pos) {
        calcNode(pos);
    }

    protected @Nullable MeshNode calcNode(MutPos pos) {
        BlockState state = Objects.requireNonNull(getLevel()).getBlockState(pos);
        if (!state.getCollisionShape(getLevel(), pos).isEmpty()) {
            return null;
        }

        pos.incrementY(-1);
        BlockState underneath = getLevel().getBlockState(pos);
        //VoxelShape collisionShape;
        if ((/*collisionShape = */underneath.getCollisionShape(getLevel(), pos)).isEmpty()) { // TODO: fix isSolid, maybe check if coll
            return null;
        }

        pos.incrementY(2);
        BlockState above = getLevel().getBlockState(pos);
        if (!above.getCollisionShape(getLevel(), pos).isEmpty()) { // we could also check if collision shape is empty?
            return null;
        }

        pos.incrementY(1);
        boolean headspace = getLevel().getBlockState(pos).getCollisionShape(getLevel(), pos).isEmpty();

        int x = pos.getX();
        int y = pos.getY() - 2;
        int z = pos.getZ();

        // double underneathY = collisionShape.max(Direction.Axis.Y) + pos.getY() - 3; // y when we stand on the solid underneath position
        MeshNode node = new MeshNode(getManager().getPooling(), getChunk(), x, y, z);
        node.setHeadSpace(headspace);
        putNode(new BlockPos(x, y, z), node);

        for (int i = 0; i < MeshNode.OFFSETS.length; i++) {
            Vec3i offset = MeshNode.OFFSETS[i];
            pos.set(x + offset.getX(), y, z + offset.getZ());
            findAdjacentNode(pos, node, getMap(), i, y);
        }

        return node;
    }

    protected void putNode(BlockPos pos, MeshNode node) {
        getMap().put(pos, node);
    }

    protected void findAdjacentNode(BlockPos.MutableBlockPos pos, MeshNode node, Map<BlockPos, MeshNode> map, int index, int nodeY) {
        pos.setY(nodeY);
        if (!Objects.requireNonNull(getLevel()).getBlockState(pos).getCollisionShape(getLevel(), pos).isEmpty()) {
            return;
        }

        pos.setY(nodeY + 1);
        if (!Objects.requireNonNull(getLevel()).getBlockState(pos).getCollisionShape(getLevel(), pos).isEmpty()) {
            return;
        }

        int minHeight = Math.min(nodeY, getManager().getConfig().getMinHeight());
        for (int y = nodeY; y >= minHeight; y--) {
            pos.setY(y);
            MeshNode adjacent = map.get(pos);
            if (adjacent != null) {
                node.getAdjacent()[index] = adjacent;
                if (nodeY - adjacent.getY() <= 2 && canStepUp(pos, adjacent, node)) { // we can step up from adjacent node to this node
                    adjacent.getAdjacent()[MeshNode.OPPOSITE_INDEX[index]] = node;
                }

                return;
            }
        }
    }

    // TODO: test thoroughly that this did not break anything
    protected boolean canStepUp(BlockPos.MutableBlockPos pos, MeshNode from, MeshNode to) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        // TODO: this is not 100% accurate, the max y also needs to be at the point were we step up,
        //  this could be a problem with e.g. stairs?
        pos.set(from.getX(), from.getY() - 1, from.getZ());
        assert getLevel() != null;
        double yAtFrom = PositionUtil.getMaxYAtPosition(pos, getLevel());

        pos.set(to.getX(), to.getY() - 1, to.getZ());
        double yAtTo = PositionUtil.getMaxYAtPosition(pos, getLevel());

        pos.set(x, y, z); // reset pos
        return yAtTo - yAtFrom <= getManager().getMovement().getStepHeight();
    }

}
