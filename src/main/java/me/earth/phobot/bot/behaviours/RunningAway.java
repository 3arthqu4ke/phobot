package me.earth.phobot.bot.behaviours;

import lombok.Getter;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.holes.Hole;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.parallelization.ParallelPathSearch;
import me.earth.phobot.util.ResetUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

// TODO: this could potentially need some improvements?
@Getter
public class RunningAway extends Behaviour {
    private final Set<Object> runningAwayRequests = new HashSet<>();

    public RunningAway(Bot bot) {
        super(bot, PRIORITY_RUN_AWAY);
        ResetUtil.onRespawnOrWorldChange(this, mc, runningAwayRequests::clear);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (phobot.getPathfinder().isFollowingPath()
                || bot.getJumpDownFromSpawn().isAboveSpawn(player)
                || runningAwayRequests.isEmpty()
                || pathSearchManager.isAtLeastEquallyImportantTo(this)
                || player.getAbsorptionAmount() < 16.0f) {
            runningAwayRequests.clear();
            return;
        }

        pathSearchManager.<Hole>applyForPathSearch(this, parallelPathSearch -> {
            gotoHoles(parallelPathSearch, player, 64.0);
            if (parallelPathSearch.getFutures().isEmpty()) {
                gotoHoles(parallelPathSearch, player, 4096.0);
            }

            parallelPathSearch.getFuture().thenAccept(result -> {
                Hole hole = result.key();
                mc.submit(() -> {
                    Vec3 goalPos = hole.getCenter();
                    phobot.getPathfinder().follow(phobot, result.algorithmResult(), goalPos);
                });
            });
        });
    }

    private void gotoHoles(ParallelPathSearch<Hole> multiPathSearch, LocalPlayer player, double distanceSq) {
        Set<Hole> holes = new HashSet<>();
        for (Hole hole : phobot.getHoleManager().getMap().values()) {
            if (!holes.contains(hole) && hole.getDistanceSqr(player) <= distanceSq) {
                holes.add(hole);
                MeshNode goal = phobot.getNavigationMeshManager().findFirst(hole.getAirParts());
                if (goal != null) {
                    multiPathSearch.findPath(hole, phobot.getPathfinder(), player, goal, true);
                    if (multiPathSearch.getFutures().size() >= bot.getParallelSearches().getValue()) {
                        return;
                    }
                }
            }
        }
    }

}
