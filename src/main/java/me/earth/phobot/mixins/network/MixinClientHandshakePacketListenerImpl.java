package me.earth.phobot.mixins.network;

import me.earth.phobot.event.AuthenticationEvent;
import me.earth.pingbypass.api.util.mixin.MixinHelper;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientHandshakePacketListenerImpl.class)
public abstract class MixinClientHandshakePacketListenerImpl {
    @Inject(method = "authenticateServer", at = @At("HEAD"), cancellable = true)
    private void authenticateServerHook(String serverId, CallbackInfoReturnable<Component> cir) {
        // in this case this works out well because we can return null
        MixinHelper.hook(new AuthenticationEvent(), cir);
    }

}
