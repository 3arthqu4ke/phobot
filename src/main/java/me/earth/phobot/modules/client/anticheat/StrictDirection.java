package me.earth.phobot.modules.client.anticheat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface StrictDirection {
    @Nullable Direction getStrictDirection(BlockPos pos, Player player, ClientLevel level);

    boolean strictDirectionCheck(BlockPos pos, Direction direction, ClientLevel level, Player player);

    enum Type {
        Vanilla,
        NCP,
        Grim,
        Combined
        // TODO: I think cc uses both, so we might need a combined AntiCheat?
    }

}
