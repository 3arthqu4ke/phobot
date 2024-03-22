package me.earth.phobot.bot.behaviours;

import me.earth.phobot.bot.Bot;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.util.math.PositionUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;
import java.util.List;

/**
 * Starts to run around randomly if it cannot find any players.
 */
public class Roaming extends Behaviour {
    private final @Unmodifiable List<Vec3> anchorPoints = List.of(
            new Vec3(0.0, 6.0, 0.0),
            new Vec3(40.0, 6.0, 0.0),
            new Vec3(40.0, 6.0, 40.0),
            new Vec3(0.0, 6.0, 40.0),
            new Vec3(-40.0, 6.0, 40.0),
            new Vec3(-40.0, 6.0, 0.0),
            new Vec3(-40.0, 6.0, -40.0),
            new Vec3(0.0, 6.0, -40.0),
            new Vec3(40.0, 6.0, -40.0));

    public Roaming(Bot bot) {
        super(bot, PRIORITY_LAST);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (bot.getTarget() == null
                && !pathSearchManager.isSearching()
                && !phobot.getPathfinder().isFollowingPath()
                && !bot.getJumpDownFromSpawn().isAboveSpawn(player)
                && level.players().stream().filter(p -> !bot.getJumpDownFromSpawn().isAboveSpawn(p) && !pingBypass.getFriendManager().contains(p.getUUID())).noneMatch(p -> p != player)) {
            if (player.distanceToSqr(0.0, player.getY(), 0.0) > 40_000.0 && !bot.isDueling() && bot.getSuicide().getValue()) { // further than 200 blocks away from spawn, why go back
                bot.getModules().getSuicide().enable();
            } else {
                Vec3 anchor = anchorPoints.stream().min(Comparator.comparingDouble(vec -> vec.distanceToSqr(player.position()))).orElse(anchorPoints.get(0));
                int nextIndex = anchorPoints.indexOf(anchor);
                nextIndex = nextIndex >= anchorPoints.size() - 1 ? 0 : nextIndex + 1;
                Vec3 goal = anchorPoints.get(nextIndex);
                var nodes = phobot.getNavigationMeshManager().getMap().values()
                        .stream()
                        .sorted(Comparator.comparingDouble(node -> node.distanceSq(goal)))
                        .limit(bot.getParallelSearches().getValue())
                        .toList();

                pathSearchManager.<MeshNode>applyForPathSearch(this, search -> {
                    nodes.forEach(node -> search.findPath(node, phobot.getPathfinder(), player, node, true));
                    search.getFuture().thenAccept(result -> {
                        mc.submit(() -> pingBypass.getChat().send(Component.literal("Roaming towards " + PositionUtil.toSimpleString(result.key()) + "..."), "Roaming"));
                        Vec3 exactGoal = result.key().getCenter(new BlockPos.MutableBlockPos(), level);
                        phobot.getPathfinder().follow(phobot, result.algorithmResult(), exactGoal);
                    });
                });
            }
        }
    }

}
