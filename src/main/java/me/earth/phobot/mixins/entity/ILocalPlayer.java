package me.earth.phobot.mixins.entity;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface ILocalPlayer {
    @Accessor("lastOnGround")
    void setLastOnGround(boolean onGround);

}
