package me.earth.phobot.pathfinder.blocks;

import lombok.RequiredArgsConstructor;
import me.earth.phobot.modules.ChecksBlockPlacingValidity;
import me.earth.phobot.services.BlockPlacer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.function.BiPredicate;

@RequiredArgsConstructor
public class BlockPathfinderWithBlacklist extends BlockPathfinder {
    private final Map<BlockPos, Long> blacklist;

    @Override
    public BiPredicate<BlockPos, BlockPos> getDefaultValidityCheck(Block block, Player player, ClientLevel level, ChecksBlockPlacingValidity module, BlockPlacer blockPlacer) {
        BiPredicate<BlockPos, BlockPos> predicate = super.getDefaultValidityCheck(block, player, level, module, blockPlacer);
        return (currentPos, testPos) -> !blacklist.containsKey(testPos) && predicate.test(currentPos, testPos);
    }
}
