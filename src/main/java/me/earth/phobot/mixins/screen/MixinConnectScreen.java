package me.earth.phobot.mixins.screen;

import me.earth.phobot.event.ConnectEvent;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class MixinConnectScreen {
    @Inject(method = "connect", at = @At("HEAD"))
    private void connectHook(Minecraft minecraft, ServerAddress serverAddress, ServerData serverData, CallbackInfo ci) {
        PingBypassApi.getEventBus().post(new ConnectEvent(serverAddress, serverData));
    }

}
