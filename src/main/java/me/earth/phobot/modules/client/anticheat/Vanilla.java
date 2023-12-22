package me.earth.phobot.modules.client.anticheat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

public enum Vanilla implements StrictDirection {
    INSTANCE;

    @Override
    public Direction getStrictDirection(BlockPos pos, Player player, ClientLevel level) {
        return Direction.DOWN;
    }

    @Override
    public boolean strictDirectionCheck(BlockPos pos, Direction direction, ClientLevel level, Player player) {
        return true;
    }

}
