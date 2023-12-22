package me.earth.phobot.mixins.network;

import me.earth.phobot.event.DisconnectEvent;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class MixinConnection {
    @Inject(
        method = "disconnect",
        at = @At(
            value = "INVOKE",
            target = "Lio/netty/channel/ChannelFuture;awaitUninterruptibly()Lio/netty/channel/ChannelFuture;",
            shift = At.Shift.AFTER,
            remap = false))
    private void disconnectHook(Component component, CallbackInfo ci) {
        PingBypassApi.getEventBus().post(new DisconnectEvent());
    }

}
