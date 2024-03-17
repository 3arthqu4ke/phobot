package me.earth.phobot.mixins.entity;

import me.earth.phobot.ducks.IDamageProtectionEntity;
import me.earth.phobot.ducks.ITotemPoppingEntity;
import me.earth.phobot.event.LerpToEvent;
import me.earth.phobot.event.StepHeightEvent;
import me.earth.phobot.util.time.StopWatch;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity implements ITotemPoppingEntity, IDamageProtectionEntity {
    @Unique
    private final StopWatch.ForSingleThread phobot$lastTotemPop = new StopWatch.ForSingleThread();
    @Unique
    private long phobot$lastLerp = 0L;
    @Unique
    private int phobot$damageProtection;

    @Inject(method = "maxUpStep", at = @At("RETURN"), cancellable = true)
    private void maxUpStepHook(CallbackInfoReturnable<Float> cir) {
        //noinspection ConstantValue
        if (LocalPlayer.class.isInstance(this)) {
            StepHeightEvent event = new StepHeightEvent(LocalPlayer.class.cast(this), cir.getReturnValueF());
            PingBypassApi.getEventBus().post(event);
            if (event.isCancelled()) {
                cir.setReturnValue(event.getHeight());
            }
        }
    }

    @Inject(method = "lerpTo", at = @At("HEAD"))
    private void lerpToHook(double x, double y, double z, float yRot, float xRot, int steps, CallbackInfo ci) {
        PingBypassApi.getEventBus().post(new LerpToEvent(LivingEntity.class.cast(this), x, y, z, yRot, xRot, phobot$lastLerp));
        phobot$lastLerp = TimeUtil.getMillis();
    }

    @Override
    public StopWatch.ForSingleThread phobot$getLastTotemPop() {
        return phobot$lastTotemPop;
    }

    @Override
    public int phobot$damageProtection() {
        return phobot$damageProtection;
    }

    @Override
    public void phobot$setDamageProtection(int damageProtection) {
        phobot$damageProtection = damageProtection;
    }

}
