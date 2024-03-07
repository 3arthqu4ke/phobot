package me.earth.phobot.mixins.entity;

import me.earth.phobot.ducks.IAbstractClientPlayer;
import me.earth.phobot.modules.render.NoRender;
import me.earth.phobot.util.player.PredictionPlayer;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer extends MixinLivingEntity implements IAbstractClientPlayer {
    @Unique
    private final PredictionPlayer[] phobot$predictionPlayers = new PredictionPlayer[5];

    @Override
    public PredictionPlayer[] phobot$getPredictions() {
        return phobot$predictionPlayers;
    }

    @Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true)
    private void getFieldOfViewModifierHook(CallbackInfoReturnable<Float> cir) {
        NoRender.Fov event = new NoRender.Fov();
        PingBypassApi.getEventBus().post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(1.0f);
        }
    }

}
