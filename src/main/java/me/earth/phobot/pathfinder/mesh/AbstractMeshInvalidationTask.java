package me.earth.phobot.pathfinder.mesh;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.invalidation.ChunkWorker;
import me.earth.phobot.pathfinder.algorithm.pooling.AbstractPooled3iNode;
import me.earth.phobot.util.mutables.MutPos;
import net.minecraft.core.BlockPos;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public abstract class AbstractMeshInvalidationTask extends MeshTask {
    protected final BlockPos changedPos;

    public AbstractMeshInvalidationTask(BlockableEventLoop<Runnable> scheduler, Level level, ChunkWorker worker, NavigationMeshManager manager, BlockPos pos) {
        super(manager.getMap(), scheduler, level, new MutPos(), manager, worker, 0, 0, manager.getConfig().getMinHeight(), manager.getConfig().getMaxHeight(), 0, 0);
        this.changedPos = pos;
    }

    @Override
    public abstract void execute();

    @Override
    protected void putNode(BlockPos pos, MeshNode node) {
        super.putNode(pos, node);
        getManager().getXZMap().computeIfAbsent(pos, v -> new HashSet<>()).add(node);
    }

    @Override
    public void run() {
        try {
            execute(); // no need for submitFinishTask, changes are present immediately.
        } catch (Exception e) {
            log.error("Exception occurred in InvalidationTask", e);
            throw e;
        }
    }

    @Override
    protected void finishTask() {
        // NOP, this uses the Graphs maps, changes are present immediately.
    }

    @Override
    protected void submitFinishTask() {
        // NOP, this uses the Graphs maps, changes are present immediately.
    }

    protected void removeNode(BlockPos pos, MeshNode node) {
        node.invalidate();
        getMap().remove(pos, node);
        removeFromXZMap(node);
        overwriteAdjacent(node, null);
        cleanupCameFromPools(node);
    }

    protected void cleanupCameFromPools(MeshNode node) {
        cleanupCameFromPools(getManager().getXZMap().get(node.getX() + 1, node.getZ()));
        cleanupCameFromPools(getManager().getXZMap().get(node.getX() - 1, node.getZ()));
        cleanupCameFromPools(getManager().getXZMap().get(node.getX(), node.getZ() + 1));
        cleanupCameFromPools(getManager().getXZMap().get(node.getX(), node.getZ() - 1));
    }

    protected void cleanupCameFromPools(@Nullable Set<MeshNode> meshNodes) {
        if (meshNodes != null) {
            meshNodes.forEach(AbstractPooled3iNode::cleanupCameFromPool);
        }
    }

}
