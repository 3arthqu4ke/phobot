package me.earth.phobot.util.player;

import lombok.Getter;
import me.earth.phobot.damagecalc.DamageCalculator;
import me.earth.phobot.ducks.IDamageProtectionEntity;
import me.earth.phobot.ducks.ITotemPoppingEntity;
import me.earth.phobot.util.time.StopWatch;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import org.jetbrains.annotations.Nullable;

/**
 * An {@link AbstractClientPlayer} which delegates some methods to a given player.
 * When performing a damage calculation with a {@link DamageCalculator}, this players
 * position will be taken into account, but armor and attribute values from the given player.
 */
@Getter
public class DamageCalculatorPlayer extends MovementPlayer implements IDamageProtectionEntity, ITotemPoppingEntity {
    private final AbstractClientPlayer player;

    public DamageCalculatorPlayer(AbstractClientPlayer player) {
        super(player.clientLevel, player.getGameProfile());
        this.player = player;
        this.copyPosition(player);
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return player.ignoreExplosion(explosion);
    }

    @Override
    public int getArmorValue() {
        return player.getArmorValue();
    }

    @Override
    public double getAttributeValue(Attribute attribute) {
        //noinspection ConstantValue
        if (player == null) {
            if (attribute == Attributes.MAX_HEALTH) {
                return 20.0f;
            }

            return 0.0;
        }

        return player.getAttributeValue(attribute);
    }

    @Nullable
    @Override
    public MobEffectInstance getEffect(MobEffect mobEffect) {
        return player.getEffect(mobEffect);
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return player.getArmorSlots();
    }

    @Override
    public int phobot$damageProtection() {
        return ((IDamageProtectionEntity) player).phobot$damageProtection();
    }

    @Override
    public void phobot$setDamageProtection(int damageProtection) {
        ((IDamageProtectionEntity) player).phobot$setDamageProtection(damageProtection);
    }

    @Override
    public StopWatch phobot$getLastTotemPop() {
        return ((ITotemPoppingEntity) player).phobot$getLastTotemPop();
    }

}
