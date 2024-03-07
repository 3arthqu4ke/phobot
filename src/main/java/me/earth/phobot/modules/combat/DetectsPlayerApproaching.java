package me.earth.phobot.modules.combat;

import me.earth.phobot.ducks.IAbstractClientPlayer;
import me.earth.phobot.holes.Hole;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.phobot.util.player.PredictionPlayer;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashSet;
import java.util.Set;

/**
 * Detects when a player is approaching a {@link Hole}.
 *
 * @see HoleFiller
 * @see SelfTrap
 */
// TODO: vertical distance?!
public interface DetectsPlayerApproaching {
    PingBypass getPingBypass();

    Setting<Double> getDistance();

    Setting<Integer> getPrediction();

    SurroundService getSurroundService();

    boolean onPlayerApproachingHole(Hole hole, Block block, PredictionPlayer pp, LocalPlayer localPlayer, ClientLevel level);

    default void checkApproachingPlayers(LocalPlayer player, Block block, ClientLevel level, @Unmodifiable Set<Hole> holes) {
        // TODO: distance to hole?
        Set<Hole> checked = new HashSet<>();
        for (Player enemy : level.players()) {
            PredictionPlayer pp;
            if (EntityUtil.isEnemyInRange(getPingBypass(), player, enemy, 8.0)
                    && !getSurroundService().isSurrounded(enemy)
                    && enemy instanceof IAbstractClientPlayer access
                    && (pp = access.phobot$getPredictions()[Math.max(0, Math.min(getPrediction().getValue() - 1, access.phobot$getPredictions().length - 1))]) != null) {
                Hole closest = null;
                double closestDistance = Double.MAX_VALUE;
                for (Hole hole : holes) {
                    double distance = hole.getDistanceSqr(pp);
                    double distanceBefore = hole.getDistanceSqr(enemy);
                    if (distance < distanceBefore && distance < closestDistance) {
                        closest = hole;
                        closestDistance = distance;
                    }
                }

                if (closest != null
                        && closestDistance <= Mth.square(getDistance().getValue())
                        && checked.add(closest)
                        && onPlayerApproachingHole(closest, block, pp, player, level)) {
                    break;
                }
            }
        }
    }

}
