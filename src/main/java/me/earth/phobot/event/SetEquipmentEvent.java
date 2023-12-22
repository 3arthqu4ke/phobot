package me.earth.phobot.event;

import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.Entity;

public record SetEquipmentEvent(ClientboundSetEquipmentPacket packet, Entity entity) {

}
