package me.earth.phobot.mixins.network;

import me.earth.phobot.event.ContinueDestroyBlockEvent;
import me.earth.phobot.event.StartDestroyBlockEvent;
import me.earth.phobot.event.StopDestroyBlockEvent;
import me.earth.phobot.modules.misc.Reach;
import me.earth.pingbypass.PingBypassApi;
import me.earth.pingbypass.api.util.mixin.MixinHelper;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {
    @Inject(method = "getPickRange", at = @At("HEAD"), cancellable = true)
    private void getPickRangeHook(CallbackInfoReturnable<Float> cir) {
        Reach.Event event = new Reach.Event();
        PingBypassApi.getEventBus().post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(event.getRange());
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void startDestroyBlockHook(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        StartDestroyBlockEvent event = new StartDestroyBlockEvent(pos, direction);
        PingBypassApi.getEventBus().post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void continueDestroyBlockHook(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        ContinueDestroyBlockEvent event = new ContinueDestroyBlockEvent(pos, direction);
        PingBypassApi.getEventBus().post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void stopDestroyBlockHook(CallbackInfo ci) {
        MixinHelper.hook(new StopDestroyBlockEvent(), ci);
    }

}
