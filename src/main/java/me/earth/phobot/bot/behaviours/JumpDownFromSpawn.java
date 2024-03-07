package me.earth.phobot.bot.behaviours;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.holes.Hole;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.util.math.MathUtil;
import me.earth.phobot.util.time.StopWatch;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

/**
 * Detects if we are up at spawn and then jumps down.
 */
@Slf4j
public class JumpDownFromSpawn extends Behaviour {
    private final StopWatch.ForSingleThread screenTimer = new StopWatch.ForSingleThread();
    private volatile CompletableFuture<?> future;

    public JumpDownFromSpawn(Bot bot) {
        super(bot, PRIORITY_JUMP_DOWN);
    }

    public boolean isAboveSpawn(LocalPlayer player) {
        return player.getY() >= bot.getSpawnHeight().getValue();
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (phobot.getPathfinder().isFollowingPath() || future != null || bot.isDueling() || !screenTimer.passed(2_000L)) {
            return;
        }

        if (mc.screen != null) {
            screenTimer.reset();
            return;
        }

        if (player.getY() >= bot.getSpawnHeight().getValue()) {
            Hole hole = phobot.getHoleManager().getMap().values().stream()
                    .filter(h -> h.getY() < bot.getSpawnHeight().getValue())
                    .min(Comparator.comparingDouble(h -> MathUtil.distance2dSq(h.getX(), h.getZ(), player.getX(), player.getZ())))
                    .orElse(null);
            if (hole == null) {
                log.warn("Failed to find hole from spawn!");
            } else {
                BlockPos pos = hole.getAirParts().stream()
                                   .min(Comparator.comparingDouble(p -> MathUtil.distance2dSq(p.getX(), p.getZ(), player.getX(), player.getZ())))
                                   .orElseThrow();
                MeshNode goal = phobot.getNavigationMeshManager().getMap().get(pos);
                if (goal == null) {
                    log.warn("Failed to find MeshNode for pos " + pos);
                } else {
                    var future = phobot.getPathfinder().findPath(player, goal, true);
                    this.future = future;
                    future.whenComplete((r,t) -> mc.submit(() -> this.future = null));
                    future.thenAccept(result -> {
                        Vec3 goalPos = hole.getCenter();
                        phobot.getPathfinder().follow(phobot, result, goalPos);
                    });
                }
            }
        }
    }

}
