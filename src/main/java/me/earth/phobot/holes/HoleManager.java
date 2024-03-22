package me.earth.phobot.holes;

import me.earth.phobot.invalidation.*;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.mutables.MutPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

public class HoleManager extends AbstractInvalidationManager<Hole, ConfigWithMinMaxHeight> implements HoleBlocks, HoleOffsets {
    public HoleManager(ConfigWithMinMaxHeight config, Map<BlockPos, Hole> map) {
        super(config, map);
    }

    @Override
    protected AbstractInvalidationTask<?, ?> getChunkTask(LevelChunk levelChunk, ChunkWorker chunk) {
        return new HoleTask(mc, levelChunk, chunk, config.getMaxHeight(), config.getMinHeight(), this);
    }

    @Override
    protected ChunkWorker getChunkWorker(LevelChunk chunk) {
        return ((ChunkWorkerProvider) chunk).phobot$getHoleChunkWorker();
    }

    @Override
    protected void addPostWorkingTask(BlockPos pos, BlockState state, ChunkWorker worker, LevelChunk chunk) {
        worker.addTask(() -> {
            Block block = state.getBlock();
            if (noBlastBlocks().contains(block)) {
                BlockHoleTask onBlockAdded = new BlockHoleTask(mc, chunk.getLevel(), this);
                onBlockAdded.setPos(pos);
                onBlockAdded.setChunk(worker);
                onBlockAdded.execute();
            } else if (state.getCollisionShape(chunk.getLevel(), pos).isEmpty()) {
                AirHoleTask onAirAdded = new AirHoleTask(mc, chunk.getLevel(), this);
                onAirAdded.setPos(pos);
                onAirAdded.setChunk(worker);
                onAirAdded.execute();
            }
        });
    }

    @Override
    protected void invalidate(LevelChunk chunk, MutPos pos, BlockState state, ChunkWorker worker) {
        if (noBlastBlocks().contains(state.getBlock())) {
            invalidate(BLOCK_OFFSETS);
        } else if (state.getCollisionShape(chunk.getLevel(), pos).isEmpty()) {
            invalidate(AIR_OFFSETS);
        } else {
            int x = mutPos.getX();
            int y = mutPos.getY();
            int z = mutPos.getZ();
            invalidate(AIR_OFFSETS);
            mutPos.set(x, y, z);
            invalidate(BLOCK_OFFSETS);
        }
    }

    public @Nullable Hole getHolePlayerIsIn(Player player) {
        double y = player.getBoundingBox().minY - 1;
        if (y > Math.floor(y) + 0.5) {
            y++;
        }

        BlockPos closestPos = null;
        double closestDistance = Double.MAX_VALUE;
        for (BlockPos pos : PositionUtil.getPositionsBlockedByEntityAtY(player, y)) {
            if (player.level().getBlockState(pos).isAir()) {
                double distance = player.distanceToSqr(pos.getX() + 0.5, player.getY(), pos.getZ() + 0.5);
                if (closestPos == null || distance < closestDistance) {
                    closestPos = pos;
                    closestDistance = distance;
                }
            }
        }

        return closestPos == null ? null : map.get(closestPos);
    }

    private void invalidate(Vec3i... offsets) {
        for (Vec3i vec3i : offsets) {
            mutPos.incrementX(vec3i.getX());
            mutPos.incrementY(vec3i.getY());
            mutPos.incrementZ(vec3i.getZ());
            Hole hole = map.get(mutPos);
            if (hole != null && hole.isAirPart(mutPos)) {
                // TODO: remove hole properly with all of its offsets!
                map.remove(mutPos);
                hole.invalidate();
            }
        }
    }

    public Stream<Hole> stream() {
        return getMap().values().stream().distinct();
    }

    public Stream<Hole> holesClosestTo(Entity entity) {
        return stream().sorted(Comparator.comparingDouble(hole -> entity.distanceToSqr(hole.getCenter())));
    }

}
