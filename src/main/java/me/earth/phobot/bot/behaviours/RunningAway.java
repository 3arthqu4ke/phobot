package me.earth.phobot.bot.behaviours;

import lombok.Getter;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.holes.Hole;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.util.MultiPathSearch;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.math.MathUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

@Getter
public class RunningAway extends Behaviour {
    private final Set<Object> runningAwayRequests = new HashSet<>();
    private MultiPathSearch multiPathSearch;

    public RunningAway(Bot bot) {
        super(bot, PRIORITY_RUN_AWAY);
        ResetUtil.onRespawnOrWorldChange(this, mc, runningAwayRequests::clear);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (phobot.getPathfinder().isFollowingPath() || bot.getJumpDownFromSpawn().isAboveSpawn(player) || runningAwayRequests.isEmpty() || multiPathSearch != null) {
            runningAwayRequests.clear();
            return;
        }
        // TODO: trap enemy if we leave the same hole?
        var multiPathSearch = new MultiPathSearch();
        this.multiPathSearch = multiPathSearch;
        gotoHoles(multiPathSearch, player, 64.0);
        if (multiPathSearch.getFutures().isEmpty()) {
            gotoHoles(multiPathSearch, player, 4096.0);
        }

        multiPathSearch.allFuturesAdded();
        if (multiPathSearch.getFutures().isEmpty()) {
            this.multiPathSearch = null;
        }

        multiPathSearch.getFuture().thenAccept(path -> {
            path.order(Algorithm.Result.Order.GOAL_TO_START);
            if (path.getPath().isEmpty()) {
                return;
            }

            MeshNode goal = path.getPath().get(0);
            // TODO: this is ugly!!! theres gotta be a better way!!!!
            Hole hole = phobot.getHoleManager().getMap().get(new BlockPos(goal.getX(), goal.getY(), goal.getZ()));
            if (hole != null) {
                mc.submit(() -> {
                    Vec3 goalPos = hole.getCenter();
                    phobot.getPathfinder().follow(phobot, path, goalPos);
                });
            }
        });

        multiPathSearch.getFuture().whenComplete((r,t) -> mc.submit(() -> {
            if (this.multiPathSearch == multiPathSearch) {
                this.multiPathSearch = null;
            }
        }));
    }

    private void gotoHoles(MultiPathSearch multiPathSearch, LocalPlayer player, double distanceSq) {
        Set<Hole> holes = new HashSet<>();
        for (Hole hole : phobot.getHoleManager().getMap().values()) {
            if (!holes.contains(hole) && hole.getDistanceSqr(player) <= distanceSq) {
                holes.add(hole);
                BlockPos pos = hole.getAirParts().stream()
                        .min(Comparator.comparingDouble(p -> MathUtil.distance2dSq(p.getX(), p.getZ(), player.getX(), player.getZ())))
                        .orElseThrow();

                MeshNode goal = phobot.getNavigationMeshManager().getMap().get(pos);
                if (goal != null) {
                    multiPathSearch.findPath(phobot.getPathfinder(), player, goal, true);
                    if (multiPathSearch.getFutures().size() >= 10) {
                        return;
                    }
                }
            }
        }
    }

}
