package me.earth.phobot.mixins.entity;

import me.earth.phobot.event.DeathEvent;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SynchedEntityData.class)
public abstract class MixinSyncedEntityData<T> {
    @Shadow @Final private Entity entity;

    @Inject(method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/network/syncher/SynchedEntityData;isDirty:Z"))
    private void setHook(EntityDataAccessor<T> entityDataAccessor, T object, boolean bl, CallbackInfo ci) {
        if (ILivingEntity.getDataHealthId().equals(entityDataAccessor) && entity instanceof LivingEntity && object instanceof Float f && f <= 0.0f) {
            PingBypassApi.getEventBus().post(new DeathEvent(entity));
        }
    }

}
