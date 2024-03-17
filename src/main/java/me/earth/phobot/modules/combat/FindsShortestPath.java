package me.earth.phobot.modules.combat;

import me.earth.phobot.modules.ChecksBlockPlacingValidity;
import me.earth.phobot.pathfinder.blocks.BlockPathfinder;
import me.earth.phobot.services.BlockPlacer;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

/**
 * Uses a {@link BlockPathfinder} to find the shortest path to a certain {@link BlockPos}.
 */
public interface FindsShortestPath extends ChecksBlockPlacingValidity {
    BlockPathfinder getBlockPathfinder();

    Setting<Integer> getMaxHelping();

    BlockPlacer getBlockPlacer();

    default @Nullable Map.Entry<BlockPos, List<BlockPos>> findShortestPath(Player player, ClientLevel level, List<BlockPos> positions, Block block) {
        List<BlockPos> shortest = null;
        BlockPos best = null;
        for (BlockPos pos : positions) {
            List<BlockPos> path = getBlockPathfinder().getShortestPath(pos, block, player, level, getMaxHelping().getValue(), this, getBlockPlacer());
            if (!path.isEmpty() && (shortest == null || path.size() < shortest.size())) {
                shortest = path;
                best = pos;
            }
        }

        return shortest == null ? null : new AbstractMap.SimpleEntry<>(best, shortest);
    }

}
