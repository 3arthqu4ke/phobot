package me.earth.phobot.pathfinder.mesh;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.invalidation.*;
import me.earth.phobot.movement.Movement;
import me.earth.phobot.util.collections.XZMap;
import me.earth.phobot.util.mutables.MutPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.Set;

// TODO: maintain "jump nodes" if we can jump from one node to the other?
// TODO: maintain multiple graphs and keep a pool, so we can calculate multiple paths at the same time?
/**
 * For fast pathfinding we maintain a navigation mesh.
 * It consists of {@link MeshNode}s, the positions a player can be in and also via {@link MeshNode#getAdjacent()}
 * which nodes can be reached from such a position.
 * That task is done by this {@link AbstractInvalidationManager} which calculates the mesh for every chunk that is
 * loaded and then rearranges it whenever a block within a chunk changes.
 */
@Slf4j
@Getter
public class NavigationMeshManager extends AbstractInvalidationManager<MeshNode, ConfigWithMinMaxHeight> {
    private final XZMap<Set<MeshNode>> xZMap;
    private final Movement movement;

    public NavigationMeshManager(ConfigWithMinMaxHeight config, Movement movement, Map<BlockPos, MeshNode> map, XZMap<Set<MeshNode>> xZMap) {
        super(config, map);
        this.xZMap = xZMap;
        this.movement = movement;
    }

    @Override
    protected AbstractInvalidationTask<?, ?> getChunkTask(LevelChunk lvlChunk, ChunkWorker chunk) {
        return new MeshTask(mc, lvlChunk, chunk, config.getMaxHeight(), config.getMinHeight(), this);
    }

    @Override
    protected ChunkWorker getChunkWorker(LevelChunk chunk) {
        return ((ChunkWorkerProvider) chunk).phobot$getGraphChunkWorker();
    }

    @Override
    protected void addPostWorkingTask(BlockPos pos, BlockState state, ChunkWorker worker, LevelChunk chunk) {
        if (pos.getY() <= getConfig().getMaxHeight() && pos.getY() >= getConfig().getMinHeight()) {
            if (state.getCollisionShape(mc.level, pos).isEmpty()) {
                worker.addTask(new AirBlockTask(mc, chunk.getLevel(), worker, this, pos));
            } else {
                worker.addTask(new SolidBlockTask(mc, chunk.getLevel(), worker, this, pos));
            }
        }
    }

    @Override
    protected void addInvalidateTask(BlockPos pos, BlockState state, ChunkWorker worker) {
        // NOP, BlockChangeTask does that
    }

    @Override
    protected void invalidate(MutPos pos, BlockState state, ChunkWorker chunk) {
        // NOP, BlockChangeTask does that
    }

    @Override
    protected void removeInvalids(LevelChunk chunk) {
        MutPos pos = new MutPos();
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                Set<MeshNode> nodes = xZMap.get(x, z);
                if (nodes != null) {
                    nodes.forEach(node -> {
                        pos.set(node.getX(), node.getY(), node.getZ());
                        map.remove(pos);
                    });

                    xZMap.remove(x, z);
                }
            }
        }
    }

    @Override
    protected void reset() {
        super.reset();
        xZMap.clear();
    }

}