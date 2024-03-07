package me.earth.phobot.event;

import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

/**
 * Fired on the main thread after a {@link ClientboundPlayerPositionPacket} has been received and handled via
 * {@link ClientGamePacketListener#handleMovePlayer(ClientboundPlayerPositionPacket)}
 *
 * @see me.earth.phobot.mixins.network.MixinClientPacketListener
 */
public class LagbackEvent {

}
