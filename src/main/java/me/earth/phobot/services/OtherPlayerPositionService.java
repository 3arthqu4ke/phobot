package me.earth.phobot.services;

import net.minecraft.world.entity.player.Player;

public class OtherPlayerPositionService extends PlayerPositionService {
    private final Player player;

    public OtherPlayerPositionService(Player player, int threshold) {
        super(threshold);
        this.player = player;
        // TODO: listeners
        //ClientboundMoveEntityPacket
    }

}
