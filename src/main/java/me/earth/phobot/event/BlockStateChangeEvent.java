package me.earth.phobot.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public record BlockStateChangeEvent(BlockPos pos, BlockState state, LevelChunk chunk) {
}
