package me.earth.phobot.modules.combat.autocrystal;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.damagecalc.DamageCalculator;
import me.earth.phobot.damagecalc.TerrainIgnoringLevel;
import me.earth.phobot.ducks.IAbstractClientPlayer;
import me.earth.phobot.ducks.IEntity;
import me.earth.phobot.util.RetryUtil;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.phobot.util.player.PredictionPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.mutable.MutableObject;

@Slf4j
@Setter
public class BreakCalculation {
    protected final CrystalPlacingModule module;
    protected final Minecraft mc;
    protected final Phobot phobot;
    protected final LocalPlayer player;
    protected final ClientLevel level;
    protected final DamageCalculator calculator;

    protected boolean breakCrystals = true;
    protected int crystalBreakDelay;
    protected boolean crystalExists;
    protected boolean attacked;

    public BreakCalculation(CrystalPlacingModule module, Minecraft mc, Phobot phobot, LocalPlayer player, ClientLevel level, DamageCalculator calculator) {
        this.crystalBreakDelay = module.breakDelay().getValue();
        this.module = module;
        this.mc = mc;
        this.phobot = phobot;
        this.player = player;
        this.level = module.terrain().getValue() ? new TerrainIgnoringLevel(level) : level;
        this.calculator = calculator;
    }

    public void breakCrystals() {
        EndCrystal bestCrystal = null;
        MutableObject<Float> bestRatio = new MutableObject<>(0.0f);
        MutableObject<Boolean> bestIsKilling = new MutableObject<>(false);
        for (EndCrystal crystal : EntityUtil.getCrystalsInRange(player, level)) {
            if (calculateSingleCrystal(crystal, bestRatio, bestIsKilling)) {
                bestCrystal = crystal;
            }
        }

        if (bestCrystal != null && breakCrystals) {
            attack(bestCrystal);
        }

        crystalExists = bestCrystal != null;
    }

    public boolean attack(Entity crystal) {
        if (module.breakTimer().passed(crystalBreakDelay)) {
            // TODO: ROTATE
            phobot.getAttackService().attack(player, crystal);
            module.breakTimer().reset();
            attacked = true;
            return true;
        }

        return false;
    }

    public boolean calculateSingleCrystal(EndCrystal crystal, MutableObject<Float> bestRatio, MutableObject<Boolean> bestIsKilling) {
        if (((IEntity) crystal).phobot$GetTimeSinceAttack() < module.maxDeathTime().getValue()) {
            return false;
        }

        float selfDamage = getSelfBreakDamage(crystal);
        boolean better = false;
        if (selfDamage < EntityUtil.getHealth(player)) {
            for (Player enemy : level.players()) {
                if (isValidPlayer(enemy, crystal.getX(), crystal.getY(), crystal.getZ())) {
                    float damage = getBreakDamage(crystal, enemy);
                    float ratio = damage / (selfDamage <= 0.0f ? 0.1f : selfDamage);
                    boolean isKilling = damage >= EntityUtil.getHealth(enemy);
                    if (ratio > bestRatio.getValue() && (isKilling || damage >= 0.5) && (!bestIsKilling.getValue() || isKilling)) {
                        bestIsKilling.setValue(isKilling);
                        bestRatio.setValue(ratio);
                        better = true;
                    }
                }
            }
        }

        return better;
    }

    protected float getSelfBreakDamage(Entity crystal) {
        // for breaking we never use a prediction player for ourselves
        return RetryUtil.retryOrThrow(3, () -> RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD, () -> calculator.getDamage(player, level, crystal));
    }

    protected float getBreakDamage(Entity crystal, Entity enemy) {
        return RetryUtil.retryOrThrow(3, () -> RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD, () -> calculator.getDamage(getDamageEntity(enemy, false), level, crystal));
    }

    protected Entity getDamageEntity(Entity entity, boolean place) {
        int predictionTicks = place ? module.placePrediction().getValue() : module.breakPrediction().getValue();
        if (predictionTicks > 0 && entity instanceof IAbstractClientPlayer abstractClientPlayer) {
            PredictionPlayer[] predictionPlayers = abstractClientPlayer.phobot$getPredictions();
            PredictionPlayer predictionPlayer = predictionPlayers[Math.min(predictionTicks - 1, predictionPlayers.length - 1)];
            if (predictionPlayer != null) {
                return predictionPlayer;
            }
        }

        return entity;
    }

    protected boolean isValidPlayer(Player enemy, double x, double y, double z) {
        return !(enemy == null
                || enemy.isDeadOrDying()
                || enemy.equals(player)
                || !enemy.isAlive()
                || enemy.distanceToSqr(player) > Mth.square(EntityUtil.RANGE + CrystalPlacingModule.CRYSTAL_RADIUS)
                || phobot.getPingBypass().getFriendManager().contains(enemy.getUUID())
                || enemy.distanceToSqr(x, y, z) > CrystalPlacingModule.CRYSTAL_RADIUS_SQ);
    }

}
