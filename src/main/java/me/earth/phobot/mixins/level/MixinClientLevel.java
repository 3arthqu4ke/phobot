package me.earth.phobot.mixins.level;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * @see me.earth.phobot.asm.FutureLevelPatch
 */
@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void initHook(ClientPacketListener clientPacketListener,
                          ClientLevel.ClientLevelData clientLevelData,
                          ResourceKey<?> resourceKey,
                          Holder<?> holder, int i, int j, Supplier<?> supplier,
                          LevelRenderer levelRenderer, boolean bl, long l, CallbackInfo ci) {
        // dummy method, just here for the future level patch.
    }

}
