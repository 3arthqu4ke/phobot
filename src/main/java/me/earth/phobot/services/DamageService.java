package me.earth.phobot.services;

import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.damagecalc.DamageCalculator;
import me.earth.phobot.damagecalc.Raytracer;
import me.earth.phobot.event.InventorySetItemEvent;
import me.earth.phobot.util.NullabilityUtil;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.AbstractEventListener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.function.Function;

@Slf4j
@Getter
public class DamageService extends SubscriberImpl {
    private final HighestDamageCalculator damageCalculator = new HighestDamageCalculator();
    private final ProtectionCacheService protectionCache;
    /**
     * Highest damage that can currently be taken by us during an invincibility frame (by taking a crystal point blank).
     */
    private volatile float highestDamage = Float.MAX_VALUE;

    public DamageService(ProtectionCacheService protectionCache, Minecraft mc) {
        this.protectionCache = protectionCache;
        listenToPacket(mc, ClientboundSetEntityDataPacket.class, ClientboundSetEntityDataPacket::id);
        listenToPacket(mc, ClientboundUpdateAttributesPacket.class, ClientboundUpdateAttributesPacket::getEntityId);
        listenToPacket(mc, ClientboundUpdateMobEffectPacket.class, ClientboundUpdateMobEffectPacket::getEntityId);
        listen(new SafeListener<InventorySetItemEvent>(mc) {
            @Override
            public void onEvent(InventorySetItemEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (event.player().equals(player)) {
                    protectionCache.updateDamageProtection(player, level);
                    updateHighestDamage(player, level);
                }
            }
        });
    }

    private <T extends Packet<?>> void listenToPacket(Minecraft mc, Class<T> packet, Function<T, Integer> getEntityId) {
        listen(new AbstractEventListener.Unsafe<PacketEvent.PostReceive<T>>(PacketEvent.PostReceive.class, packet) {
            @Override
            public void onEvent(PacketEvent.PostReceive<T> event) {
                mc.submit(() -> NullabilityUtil.safe(mc, ((player, level, gameMode) -> {
                    if (getEntityId.apply(event.getPacket()) == player.getId()) {
                        updateHighestDamage(player, level);
                    }
                })));
            }
        });
    }

    @Synchronized
    private void updateHighestDamage(LocalPlayer player, ClientLevel level) {
        this.highestDamage = damageCalculator.getHighestDamage(player, level);
    }

    private static final class HighestDamageCalculator extends DamageCalculator {
        public HighestDamageCalculator() {
            super(Raytracer.level());
        }

        public float getHighestDamage(Entity entity, Level level) {
            return getDamage(entity, level, DamageCalculator.END_CRYSTAL_EXPLOSION, entity.getX(), entity.getY(), entity.getZ());
        }

        @Override
        protected float getSeenPercent(Raytracer raytracer, Level level, Entity entity, double x, double y, double z) {
            return 1.0f;
        }
    }

}
