package me.earth.phobot.bot.behaviours;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.holes.Hole;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.entity.EntityUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.List;

/**
 * Chases the current target.
 */
@Slf4j
public class Chasing extends Behaviour {
    private volatile Goal goal;

    public Chasing(Bot bot) {
        super(bot, PRIORITY_CHASE);
        ResetUtil.onRespawnOrWorldChange(this, mc, () -> goal = null);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        Entity target = bot.getTarget();
        if (target == null
                || EntityUtil.isDeadOrRemoved(target)
                || !bot.getRunningAway().getRunningAwayRequests().isEmpty()
                || bot.getJumpDownFromSpawn().isAboveSpawn(player)) {
            pathSearchManager.cancel(this);
            return;
        }

        if (pathSearchManager.isAtLeastEquallyImportantTo(this)
                || target.distanceToSqr(player) <= 36.0
                || player.getAbsorptionAmount() < 16.0f && bot.getSurroundService().isSurrounded()) {
            return;
        }

        // TODO:!!!
        boolean isSurrounded = target instanceof Player enemy && bot.getSurroundService().isSurrounded(enemy);
        List<Hole> holes;
        if (isSurrounded && player.distanceToSqr(target) <= 36.0 && !bot.getSurroundService().isSurrounded()) { // target is safe, and we are close, gotta get in a hole fast!
            holes = phobot.getHoleManager().getMap().values().stream().distinct().sorted(Comparator.comparingDouble(hole -> hole.getDistanceSqr(player))).toList();
            // TODO: check against currentTargetHole?
        } else {
            // search holes near the target, if it is surrounded a bit further away
            holes = getHoles(player, target, isSurrounded);
            Goal currentTargetHole = this.goal;
            if (currentTargetHole != null && (!isSurrounded || currentTargetHole.hole.getDistanceSqr(target) >= 25.0) && holes.contains(currentTargetHole.hole)) {
                return;
            }
        }

        if (holes.isEmpty()) {
            bot.getTargeting().setTarget(null);
        }

        pathSearchManager.<Hole>applyForPathSearch(this, multiPathSearch -> {
            for (Hole hole : holes) {
                MeshNode goal = phobot.getNavigationMeshManager().findFirst(hole.getAirParts());
                if (goal != null) {
                    multiPathSearch.findPath(hole, phobot.getPathfinder(), player, goal, true);
                }

                if (multiPathSearch.getFutures().size() >= bot.getParallelSearches().getValue()) {
                    break;
                }
            }

            multiPathSearch.getFuture().thenAccept(result -> {
                var holeReference = new Goal(result.key());
                this.goal = holeReference;
                mc.submit(() -> {
                    Entity currentTarget = bot.getTarget();
                    if (currentTarget == null
                            || currentTarget != target
                            || EntityUtil.isDeadOrRemoved(currentTarget)
                            || !bot.getRunningAway().getRunningAwayRequests().isEmpty()
                            || bot.getJumpDownFromSpawn().isAboveSpawn(player)) {
                        synchronized (pathSearchManager) {
                            if (this.goal == holeReference) {
                                this.goal = null;
                            }
                        }

                        return;
                    }

                    phobot.getPathfinder().follow(phobot, result.algorithmResult(), result.key().getCenter()).thenAccept(r -> {
                        synchronized (pathSearchManager) {
                            if (this.goal == holeReference) {
                                this.goal = null;
                            }
                        }
                    });
                });
            });
        });
    }

    private List<Hole> getHoles(Player player, Entity target, boolean isSurrounded) {
        return phobot.getHoleManager()
                .getMap()
                .values()
                .stream()
                .distinct()
                .filter(hole -> hole.getDistanceSqr(target) <= 100.0) // holes need to be near the target
                .filter(hole -> hole.getDistanceSqr(target) >= (isSurrounded ? 25.0 : 0.0)) // if the target is surrounded we want to keep our distance
                .sorted(Comparator.comparing(hole -> isSurrounded ? hole.getDistanceSqr(player) : hole.getDistanceSqr(target)))
                .toList();
    }

    private record Goal(Hole hole) { }

}
