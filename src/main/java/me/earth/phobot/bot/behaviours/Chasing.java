package me.earth.phobot.bot.behaviours;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.holes.Hole;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.parallelization.CancellableSearch;
import me.earth.phobot.pathfinder.parallelization.PrioritizingParallelPathSearch;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.phobot.util.time.StopWatch;
import me.earth.phobot.util.time.TimeUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Chases the current target.
 */
@Slf4j
@Getter
public class Chasing extends Behaviour {
    private static final double DISTANCE_SQ = (6.0 + Math.sqrt(2)) * (6.0 + Math.sqrt(2));
    /** It can be that an enemy is not surrounded for a short time and then surrounded again, we do not want to let that fool us */
    private final StopWatch.ForSingleThread enemyNotSafeTimer = new StopWatch.ForSingleThread();
    /** Last time we have chased someone. */
    private final StopWatch.ForSingleThread lastChasing = new StopWatch.ForSingleThread();
    private final StopWatch.ForSingleThread lastFailedPathSearch = new StopWatch.ForSingleThread();
    private boolean enemySurrounded;
    private int lastFailedId;
    private int currentTargetId;
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

        if (pathSearchManager.isAtLeastEquallyImportantTo(this)) {
            return;
        }

        updateSurrounded(target);
        /*
        Ok, let's plan what we want to do:
        - We want to chase a player
            - search for a hole close to them
            - follow path to that hole
            - if hole too far away, search a new hole, repeat
        - When the player is in a hole of their own, we want to go just in range of them.
        - We need to check if the hole is valid or not, it could be that we got hole filled
        - Do not leave our hole if we do not have full absorption. TODO: also take health into account
        - Go into nearest hole quickly if our health is below a certain threshold
         */
        boolean weAreSafe = bot.getSurroundService().isSurrounded() || !bot.getSurroundService().getLastSafe().passed(100L);
        boolean enemyIsSafe = enemySurrounded || !enemyNotSafeTimer.passed(300L);
        double distanceToEnemySq = player.distanceToSqr(target);

        Goal goal = this.goal;
        Goal holeFilledGoal = null;
        if (goal != null && goal.id == currentTargetId) { // we already have a goal belonging to our current target
            if (!goal.hole.isValid()) {
                Set<BlockPos> airParts = goal.hole.getAirParts();
                if (airParts.stream().allMatch(pos -> level.getBlockState(pos).isAir())) { // hole has not been filled
                    Hole hole = phobot.getHoleManager().getMap().get(airParts.stream().findFirst().orElseThrow());
                    if (hole != null && hole.isValid()) { // it could be that the hole was temporarily not valid and a new hole formed on the same position.
                        goal = new Goal(hole, currentTargetId);
                        this.goal = goal;
                    } else {
                        holeFilledGoal = goal;
                        goal = null;
                    }
                } else {
                    holeFilledGoal = goal;
                    goal = null;
                }
            } // TODO: check if the hole got trapped via bot.getEscape?
        } else {
            goal = null; // we do not have a goal, lets find one!
        }

        if (holeFilledGoal != null
                && phobot.getPathfinder().isFollowingPath()
                && player.distanceToSqr(holeFilledGoal.hole.getCenter()) <= 4.0
                && target.distanceToSqr(holeFilledGoal.hole.getCenter()) <= DISTANCE_SQ) {
            // we are so close to the filled hole, just surround on that thing?
            this.goal = holeFilledGoal;
            return;
        }

        if (weAreSafe && (player.getAbsorptionAmount() < 16.0f || player.getHealth() < bot.getLeaveHealth().getValue())) { // do not leave our hole until we have eaten up
            return;
        }

        // it is time to get in a hole!
        if (EntityUtil.getHealth(player) <= phobot.getDamageService().getHighestDamage()/* TODO: - leniency?*/) {
            if (weAreSafe) { // already safe!
                return;
            }

            var holeStream = phobot.getHoleManager().holesClosestTo(player);
            if (goal != null && goal.hole.getDistanceSqr(player) < 4.0) { // we already have a goal which is very close, only find improving holes that are even closer
                Goal finalGoal = goal;
                holeStream = holeStream.filter(hole -> hole.getDistanceSqr(player) < finalGoal.hole.getDistanceSqr(player));
            } else {
                // TODO: check this function
                holeStream = holeStream.sorted((hole1, hole2) -> {
                    double distance1ToUs = hole1.getDistanceSqr(player);
                    double distance2ToUs = hole2.getDistanceSqr(player);
                    if (distance1ToUs < 4.0 || distance2ToUs < 4.0 || !enemyIsSafe) { // we are already very close to this hole, or enemy is unsafe then we can approach just fine
                        return Double.compare(distance1ToUs, distance2ToUs); // this will just find the closest hole to us
                    }

                    // the idea of this comparator is that we generally do not want to go closer to the enemy
                    // so if the hole lowers our distance to the enemy we add that distance as a weight to it
                    double distance1ToEnemy = hole1.getDistanceSqr(target);
                    boolean hole1BringsUsCloserToEnemy = distance1ToEnemy < distanceToEnemySq;
                    double distance2ToEnemy = hole2.getDistanceSqr(target);
                    boolean hole2BringsUsCloserToEnemy = distance2ToEnemy < distanceToEnemySq;

                    return Double.compare(distance1ToUs + (hole1BringsUsCloserToEnemy ? distance1ToEnemy : 0), distance2ToUs + (hole2BringsUsCloserToEnemy ? distance2ToEnemy : 0));
                });
            }

            log.info("We are unsafe, attempting to get to a hole.");
            gotoHoles(player, holeStream);
            return;
        }

        if (goal == null) {
            if (weAreSafe && distanceToEnemySq <= DISTANCE_SQ) { // no need to move, we are in range
                return;
            }

            log.info("No goal, chasing " + target.getScoreboardName());
            chase(player, target, level, weAreSafe, enemyIsSafe);
        } else {
            // we already have a goal, lets check if we need to get in a safe hole quick, or if there is a better goal now
            if (goal.hole.getDistanceSqr(target) > DISTANCE_SQ) { // Oh, no! the target is running away, we got to find a new hole closer to them
                log.info("Target moved away from previous goal, chasing: " + target.getScoreboardName());
                chase(player, target, level, weAreSafe, enemyIsSafe);
            } else if (goal.hole.getDistanceSqr(target) < goal.hole.getDistanceSqr(player) && distanceToEnemySq <= DISTANCE_SQ) {
                // the enemy is actually closer to the hole than we are, this is dangerous!
                log.info("Target is closer to goal than us: " + target.getScoreboardName());
                chase(player, target, level, weAreSafe, enemyIsSafe);
            }
        }
    }

    private void chase(Player player, Entity target, Level level, boolean weAreSafe, boolean enemyIsSafe) {
        var stream = phobot.getHoleManager().holesClosestTo(player);
        // if we are approaching from far get in a hole that is further away from the enemy first V
        var list = stream.filter(hole -> hole.getDistanceSqr(target) <= (enemyIsSafe && !weAreSafe ? 100.0 : DISTANCE_SQ)).toList();
        if (list.isEmpty()) { // TODO: start scaffolding
            targetSomeoneElse(target);
            return;
        }

        var future = gotoHoles(player, list.stream());
        if (future != null) {
            future.whenComplete((r,t) -> {
                if (t != null) { // we failed to chase the player
                    mc.submit(() -> {
                        if (mc.level == level && currentTargetId == target.getId()) {
                            lastFailedId = target.getId();
                            lastFailedPathSearch.reset();
                            targetSomeoneElse(target);
                        }
                    });
                }
            });
        }
    }

    private void targetSomeoneElse(Entity target) {
        bot.getTargeting().setTarget(null); // TODO: set to other target
        bot.getTargeting().getInvalidTargets().put(target.getId(), TimeUtil.getMillis());
        log.info("Failed to chase " + target.getScoreboardName() + ", targeting someone else instead.");
    }

    private @Nullable CompletableFuture<CancellableSearch.Result<Hole>> gotoHoles(Player player, Stream<Hole> holeStream) {
        int id = currentTargetId;
        return pathSearchManager.<Hole, PrioritizingParallelPathSearch<Hole>>applyForPathSearch(this, PrioritizingParallelPathSearch::new, search -> {
            holeStream
                    .filter(hole -> !bot.getEscape().isTrapped(phobot.getNavigationMeshManager().findFirst(hole.getAirParts())))
                    .limit(bot.getParallelSearches().getValue())
                    .forEach(hole -> {
                        MeshNode meshNode = phobot.getNavigationMeshManager().findFirst(hole.getAirParts());
                        if (meshNode != null) {
                            search.findPath(hole, phobot.getPathfinder(), player, meshNode, true);
                        }
                    });

            search.getFuture().thenAccept(result -> {
                var holeReference = new Goal(result.key(), id);
                this.goal = holeReference;
                mc.submit(() -> {
                    Entity currentTarget = bot.getTarget();
                    if (currentTarget == null
                            || currentTarget.getId() != id
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

            search.registerTimeoutManager(phobot.getTaskService(), 35L); // after 35ms accept any of the futures, not just the closest hole
        });
    }

    private void updateSurrounded(Entity target) {
        if (target.getId() != currentTargetId) {
            currentTargetId = target.getId();
            enemySurrounded = bot.getSurroundService().isSurrounded(target);
        } else {
            boolean surrounded = bot.getSurroundService().isSurrounded(target);
            if (enemySurrounded && !surrounded) {
                enemyNotSafeTimer.reset();
            }

            enemySurrounded = surrounded;
        }
    }

    private record Goal(Hole hole, int id) { }

}
