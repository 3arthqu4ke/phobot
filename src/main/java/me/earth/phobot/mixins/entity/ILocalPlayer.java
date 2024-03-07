package me.earth.phobot.mixins.entity;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface ILocalPlayer {
    @Accessor("xLast")
    void setXLast(double xLast);

    @Accessor("yLast1") // TODO: next update check, was this a typo?
    void setYLast1(double yLast1);

    @Accessor("zLast")
    void setZLast(double zLast);

    @Accessor("yRotLast")
    void setYRotLast(float yRotLast);

    @Accessor("xRotLast")
    void setXRotLast(float xRotLast);

    @Accessor("lastOnGround")
    void setLastOnGround(boolean onGround);

    @Accessor("positionReminder")
    void setPositionReminder(int positionReminder);

}
