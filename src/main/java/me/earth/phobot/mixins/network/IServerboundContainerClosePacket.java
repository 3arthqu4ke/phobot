package me.earth.phobot.mixins.network;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundContainerClosePacket.class)
public interface IServerboundContainerClosePacket {
    @Accessor("containerId")
    int getContainerId();

}
