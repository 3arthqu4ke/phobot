package me.earth.phobot.mixins.entity;

import me.earth.phobot.ducks.IEntity;
import me.earth.phobot.modules.movement.Velocity;
import me.earth.pingbypass.api.util.mixin.MixinHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEntity {
    @Unique
    private volatile long phobot$attackTime;

    @Override
    public long phobot$getAttackTime() {
        return phobot$attackTime;
    }

    @Override
    public void phobot$setAttackTime(long time) {
        this.phobot$attackTime = time;
    }

    // not great since it prevents us from pushing entities in SinglePlayer, but at least no @Redirect
    @Inject(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;sqrt(D)D", shift = At.Shift.BEFORE),
            cancellable = true)
    private void entityPushHook(Entity entity, CallbackInfo ci) {
        //noinspection EqualsBetweenInconvertibleTypes
        if (Objects.equals(this, Minecraft.getInstance().player) || Objects.equals(entity, Minecraft.getInstance().player)) {
            MixinHelper.hook(new Velocity.EntityPush(), ci);
        }
    }

}
