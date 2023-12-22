package me.earth.phobot.damagecalc;

import me.earth.phobot.util.world.DelegatingClientLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class TerrainIgnoringLevel extends DelegatingClientLevel {
    public TerrainIgnoringLevel(ClientLevel level) {
        super(level);
    }

    @Override
    public @NotNull BlockState getBlockState(BlockPos blockPos) {
        BlockState state = super.getBlockState(blockPos);
        if (state.getBlock().getExplosionResistance() < 600) {
            state = Blocks.AIR.defaultBlockState();
        }

        return state;
    }

}
