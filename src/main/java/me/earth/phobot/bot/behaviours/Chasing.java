package me.earth.phobot.bot.behaviours;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.holes.Hole;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.util.math.MathUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

/**
 * Chases the current target.
 */
@Slf4j
public class Chasing extends Behaviour {
    private volatile CompletableFuture<?> future;

    public Chasing(Bot bot) {
        super(bot, PRIORITY_CHASE);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (phobot.getPathfinder().isFollowingPath() || this.future != null || bot.getJumpDownFromSpawn().isAboveSpawn(player)) {
            return;
        }

        Entity target = bot.getTarget();
        if (target == null || player.distanceToSqr(target) <= 49.0) { // TODO: bring ourselves into KillAura teleport range?
            return;
        }

        boolean isSurrounded = target instanceof Player enemy && bot.getSurroundService().isSurrounded(enemy);
        Hole best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Hole hole : phobot.getHoleManager().getMap().values()) {
            double distance = hole.getDistanceSqr(target);
            if ((isSurrounded ? distance < 36.0 : distance < 4.0)) {
                continue;
            }

            if (best == null || bestDistance > distance) {
                best = hole;
                bestDistance = distance;
            }
        }

        if (best == null) {
            log.info("Failed to chase " + target + " no hole in range found.");
        } else {
            BlockPos pos = best.getAirParts().stream()
                    .min(Comparator.comparingDouble(p -> MathUtil.distance2dSq(p.getX(), p.getZ(), player.getX(), player.getZ())))
                    .orElseThrow();
            MeshNode goal = phobot.getNavigationMeshManager().getMap().get(pos);
            if (goal == null) {
                log.warn("Failed to find MeshNode for pos " + pos);
            } else {
                var future = phobot.getPathfinder().findPath(player, goal, true);
                this.future = future;
                future.whenComplete((r,t) -> mc.submit(() -> this.future = null));
                Hole finalBest = best;
                future.thenAccept(result -> {
                    Vec3 goalPos = finalBest.getCenter();
                    phobot.getPathfinder().follow(phobot, result, goalPos);
                });
            }
        }
    }

}
