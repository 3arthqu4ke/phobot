package me.earth.phobot.util.entity;

import lombok.experimental.UtilityClass;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.pingbypass.PingBypass;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;

@UtilityClass
public class EntityUtil {
    public static final double RANGE = 6.0;
    public static final double RANGE_SQ = ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE;

    public static boolean isDeadOrRemoved(Entity entity) {
        return !entity.isAlive() || entity.isRemoved() || entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying();
    }

    public static boolean isDead(LivingEntity entity) {
        return !entity.isAlive() || entity.isDeadOrDying();
    }

    public static float getHealth(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    public static boolean isEnemyInRange(PingBypass pingBypass, Player self, Player player, double range) {
        return isEnemy(pingBypass, self, player) && self.distanceToSqr(player) <= range * range;
    }

    public static boolean isEnemy(PingBypass pingBypass, Player self, Player player) {
        return player != null && !isDead(player) && !self.equals(player) && !pingBypass.getFriendManager().contains(player.getUUID());
    }

    public static Iterable<EndCrystal> getCrystalsInRange(Player player, ClientLevel level) {
        return level.getEntities(EntityType.END_CRYSTAL, PositionUtil.getAABBOfRadius(player, 8.0),
                endCrystal -> endCrystal.isAlive()
                        && isInAttackRange(player, endCrystal)
                        && level.getWorldBorder().isWithinBounds(endCrystal.blockPosition()));
    }

    public static boolean isInAttackRange(Player player, Entity entity) {
        return entity.getBoundingBox().distanceToSqr(player.getEyePosition()) < RANGE_SQ;
    }

}
