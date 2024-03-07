package me.earth.phobot.mixins;

import me.earth.phobot.event.ChangeWorldEvent;
import me.earth.phobot.event.LocalPlayerDeathEvent;
import me.earth.phobot.event.PreKeybindHandleEvent;
import me.earth.phobot.modules.misc.AutoRespawn;
import me.earth.pingbypass.PingBypassApi;
import me.earth.pingbypass.api.util.mixin.MixinHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow @Nullable public LocalPlayer player;

    @Inject(method = "clearClientLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("RETURN"))
    private void clearClientLevelHook(Screen screen, CallbackInfo ci) {
        PingBypassApi.getEventBus().post(new ChangeWorldEvent(null));
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void setLevelHook(ClientLevel clientLevel, CallbackInfo ci) {
        PingBypassApi.getEventBus().post(new ChangeWorldEvent(clientLevel));
    }

    @Inject(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0, shift = At.Shift.BEFORE),
        cancellable = true)
    private void handleKeybindsHook(CallbackInfo ci) {
        MixinHelper.hook(new PreKeybindHandleEvent(), ci);
    }

    @Inject(method = "setScreen", at = @At(value = "NEW", target = "(Lnet/minecraft/network/chat/Component;Z)Lnet/minecraft/client/gui/screens/DeathScreen;"), cancellable = true)
    private void deathScreenHook(Screen screen, CallbackInfo ci) {
        MixinHelper.hook(new AutoRespawn.DeathScreenEvent(), ci);
    }

    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;shouldShowDeathScreen()Z", shift = At.Shift.BEFORE))
    private void shouldShowDeathScreenHook(Screen screen, CallbackInfo ci) {
        PingBypassApi.getEventBus().post(new LocalPlayerDeathEvent(player));
    }

}
