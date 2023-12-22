package me.earth.phobot.mixins.network;

import me.earth.phobot.ducks.IServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerboundInteractPacket.class)
public abstract class MixinServerboundInteractPacket implements IServerboundInteractPacket {
    @Unique
    private Entity phobot$entity;
    @Unique
    private boolean phobot$attack;

    @Inject(method = "createAttackPacket", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void createAttackPacketHook(Entity entity, boolean bl, CallbackInfoReturnable<ServerboundInteractPacket> cir) {
        ServerboundInteractPacket packet = cir.getReturnValue();
        if (packet != null) {
            ((IServerboundInteractPacket) packet).setPhobot$attack(true);
            ((IServerboundInteractPacket) packet).setPhobot$entity(entity);
        }
    }

    @Override
    public Entity getPhobot$entity() {
        return phobot$entity;
    }

    @Override
    public void setPhobot$entity(Entity phobot$entity) {
        this.phobot$entity = phobot$entity;
    }

    @Override
    public boolean isPhobot$attack() {
        return phobot$attack;
    }

    @Override
    public void setPhobot$attack(boolean phobot$attack) {
        this.phobot$attack = phobot$attack;
    }

}
