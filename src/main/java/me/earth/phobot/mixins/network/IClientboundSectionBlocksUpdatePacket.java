package me.earth.phobot.mixins.network;

import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public interface IClientboundSectionBlocksUpdatePacket {
    @Accessor("positions")
    short[] getPositions();

}
