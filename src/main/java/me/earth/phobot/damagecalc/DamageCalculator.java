package me.earth.phobot.damagecalc;

import lombok.RequiredArgsConstructor;
import me.earth.phobot.ducks.IDamageProtectionEntity;
import me.earth.phobot.util.mutables.MutAABB;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@RequiredArgsConstructor
public class DamageCalculator {
    public static final float END_CRYSTAL_EXPLOSION = 6.0f;
    public static final float ANCHOR_EXPLOSION = 5.0f;
    public static final float BED_EXPLOSION = 5.0f;

    private final Raytracer raytracer;

    public float getDamage(Entity entity, Level level, Entity crystal) {
        return getDamage(entity, level, END_CRYSTAL_EXPLOSION, crystal.getX(), crystal.getY(), crystal.getZ());
    }

    public float getDamage(Entity entity, Level level, BlockPos position) {
        return getDamage(entity, level, END_CRYSTAL_EXPLOSION, position.getX() + 0.5, position.getY() + 1, position.getZ() + 0.5);
    }

    public float getDamage(Entity entity, Level level, float power, double x, double y, double z) {
        float diameter = power * 2.0f;
        double distance = Math.sqrt(entity.distanceToSqr(x, y, z)) / diameter;
        if (distance > 1.0 || ignoreExplosion(entity) || getDifficulty(level) == Difficulty.PEACEFUL) {
            return 0.0f;
        }

        double seenPercent = getSeenPercent(raytracer, level, entity, x, y, z);
        double seenVsDistance = (1.0 - distance) * seenPercent;
        float damage = (int) ((seenVsDistance * seenVsDistance + seenVsDistance) / 2.0 * 7.0 * diameter + 1.0);
        if (damage == 0.0f) {
            return 0.0f;
        }

        if (entity instanceof LivingEntity livingEntity) {
            if (entity instanceof Player) {
                damage = scaleByDifficulty(level, damage);
            }

            // TODO: java.util.ConcurrentModificationException on MultiThreading, cache armor value!
            damage = CombatRules.getDamageAfterAbsorb(damage, livingEntity.getArmorValue(), (float) livingEntity.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            MobEffectInstance damageResistance = livingEntity.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (damageResistance != null) {
                int damageResistanceAmplifier = (damageResistance.getAmplifier() + 1) * 5;
                damage = Math.max((damage * (float) (25 - damageResistanceAmplifier)) / 25.0F, 0.0F);
                if (damage <= 0.0f) {
                    return 0.0f;
                }
            }

            int damageProtection = getDamageProtection(livingEntity);
            if (damageProtection > 0) {
                damage = CombatRules.getDamageAfterMagicAbsorb(damage, damageProtection);
            }
        }

        return Math.max(damage, 0.0f);
    }

    // TODO: this!
    /**
     * Legacy method for {@link Entity#ignoreExplosion(Explosion)}.
     */
    private boolean ignoreExplosion(Entity entity) {
        return false;
    }

    protected int getDamageProtection(LivingEntity livingEntity) {
        return ((IDamageProtectionEntity) livingEntity).phobot$damageProtection();
    }

    protected float scaleByDifficulty(Level level, float damage) {
        return switch (getDifficulty(level)) {
            case PEACEFUL -> 0.0f;
            case EASY -> Math.min(damage / 2.0f + 1.0f, damage);
            case HARD -> damage * 3.0f / 2.0f;
            default -> damage;
        };
    }

    protected Difficulty getDifficulty(Level level) {
        return level.getDifficulty();
    }

    // TODO: this is a bottleneck
    // TODO: solve everything with mutables!
    protected float getSeenPercent(Raytracer raytracer, Level level, Entity entity, double x, double y, double z) {
        MutAABB mutAABB = new MutAABB();
        AABB bb = entity.getBoundingBox();
        Vec3 to = new Vec3(x, y, z);
        double xD = 1.0 / ((bb.maxX - bb.minX) * 2.0 + 1.0);
        double yD = 1.0 / ((bb.maxY - bb.minY) * 2.0 + 1.0);
        double zD = 1.0 / ((bb.maxZ - bb.minZ) * 2.0 + 1.0);
        double xDD = (1.0 - Math.floor(1.0 / xD) * xD) / 2.0;
        double zDD = (1.0 - Math.floor(1.0 / zD) * zD) / 2.0;
        if (xD < 0.0 || yD < 0.0 || zD < 0.0) {
            return 0.0f;
        }

        int missed = 0;
        int blocks = 0;
        for (double xFactor = 0.0; xFactor <= 1.0; xFactor += xD) {
            for (double yFactor = 0.0; yFactor <= 1.0; yFactor += yD) {
                for (double zFactor = 0.0; zFactor <= 1.0; zFactor += zD) {
                    double fromX = Mth.lerp(xFactor, bb.minX, bb.maxX);
                    double fromY = Mth.lerp(yFactor, bb.minY, bb.maxY);
                    double fromZ = Mth.lerp(zFactor, bb.minZ, bb.maxZ);
                    Vec3 from = new Vec3(fromX + xDD, fromY, fromZ + zDD);
                    if (raytracer.clip(level, mutAABB, new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() == HitResult.Type.MISS) {
                        ++missed;
                    }

                    ++blocks;
                }
            }
        }

        return (float) missed / (float) blocks;
    }

}
