package me.earth.phobot.services;

import me.earth.phobot.ducks.IDamageProtectionEntity;
import me.earth.phobot.event.SetEquipmentEvent;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class ProtectionCacheService extends SubscriberImpl {
    public ProtectionCacheService(Minecraft mc) {
        listen(new Listener<SetEquipmentEvent>() {
            @Override
            public void onEvent(SetEquipmentEvent event) {
                ClientLevel level;
                if (event.packet().getSlots().stream().anyMatch(p -> p.getFirst().isArmor()) && (level = mc.level) != null && event.entity() instanceof LivingEntity livingEntity) {
                    updateDamageProtection(livingEntity, level);
                }
            }
        });
    }

    public void updateDamageProtection(LivingEntity entity, ClientLevel level) {
        int damageProtection = EnchantmentHelper.getDamageProtection(entity.getArmorSlots(), level.damageSources().explosion(null, null));
        ((IDamageProtectionEntity) entity).phobot$setDamageProtection(damageProtection);
    }

}