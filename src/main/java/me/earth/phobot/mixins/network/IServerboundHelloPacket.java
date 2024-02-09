package me.earth.phobot.mixins.network;

import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(ServerboundHelloPacket.class)
public interface IServerboundHelloPacket {
    @Mutable
    @Accessor("name")
    void setName(String name);

    @Mutable
    @Accessor("profileId")
    void setProfileId(UUID profileId);

}
