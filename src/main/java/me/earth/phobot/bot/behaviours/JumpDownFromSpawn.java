package me.earth.phobot.bot.behaviours;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.holes.Hole;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.math.MathUtil;
import me.earth.phobot.util.time.StopWatch;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Objects;

/**
 * Detects if we are up at spawn and then jumps down.
 */
@Slf4j
public class JumpDownFromSpawn extends Behaviour {
    private static final String CHAT_ID = "JumpDownFromSpawn";
    private final StopWatch.ForSingleThread screenTimer = new StopWatch.ForSingleThread();

    public JumpDownFromSpawn(Bot bot) {
        super(bot, PRIORITY_JUMP_DOWN);
        ResetUtil.onRespawnOrWorldChange(this, mc, screenTimer::reset);
    }

    public boolean isAboveSpawn(Entity player) {
        return player.getY() >= bot.getSpawnHeight().getValue();
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        // TODO: make sure the NavigationMesh has been setup enough until now!
        if (phobot.getPathfinder().isFollowingPath() || pathSearchManager.isAtLeastEquallyImportantTo(this) || bot.isDueling()) {
            return;
        }

        if (mc.screen != null) {
            screenTimer.reset();
            return;
        }

        if (player.getY() >= bot.getSpawnHeight().getValue()) {
            if (!screenTimer.passed(3_000L)) {
                double time = MathUtil.round((3_000L - screenTimer.getPassedTime()) / 1000.0, 1);
                pingBypass.getChat().sendWithoutLogging(Component.literal("Phobot dropping in " + time + "s..."), CHAT_ID);
                return;
            } else {
                pingBypass.getChat().send(Component.literal("Dropping..."), CHAT_ID);
            }

            pathSearchManager.<Hole>applyForPathSearch(this, pathSearch -> {
                phobot.getHoleManager()
                        .stream()
                        .filter(h -> h.getY() < bot.getSpawnHeight().getValue())
                        .sorted(Comparator.comparingDouble(h -> MathUtil.distance2dSq(h.getX(), h.getZ(), player.getX(), player.getZ())))
                        .map(hole -> {
                            BlockPos pos = hole.getAirParts()
                                    .stream()
                                    .min(Comparator.comparingDouble(p -> MathUtil.distance2dSq(p.getX(), p.getZ(), player.getX(), player.getZ())))
                                    .orElse(null);
                            if (pos != null) {
                                MeshNode meshNode = phobot.getNavigationMeshManager().getMap().get(pos);
                                return meshNode == null ? null : new AbstractMap.SimpleEntry<>(hole, meshNode);
                            }

                            return null;
                        }).filter(Objects::nonNull)
                        .limit(bot.getParallelSearches().getValue())
                        .forEach(goal -> pathSearch.findPath(goal.getKey(), phobot.getPathfinder(), player, goal.getValue(), true));

                pathSearch.getFuture().whenComplete((result,t) -> {
                    if (result != null) {
                        phobot.getPathfinder().follow(phobot, result.algorithmResult(), result.key().getCenter());
                    }

                    mc.submit(() -> pingBypass.getChat().delete(CHAT_ID));
                });
            });
        } else {
            pingBypass.getChat().delete(CHAT_ID);
        }
    }

}
