package me.earth.phobot.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.earth.phobot.Phobot;
import me.earth.phobot.pathfinder.Path;
import me.earth.phobot.pathfinder.PathRenderer;
import me.earth.phobot.pathfinder.algorithm.AStar;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.algorithm.AlgorithmRenderer;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.util.CancellationTaskUtil;
import me.earth.pingbypass.api.command.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Comparator;

public class GotoCommand extends AbstractPhobotCommand {
    public GotoCommand(Phobot phobot) {
        super("goto", "Go to the specified location.", phobot);
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            if (mc.player != null) {
                mc.gui.getChat().addMessage(Component.literal("Use 'goto <position>' to go to a position. Your current position is " + mc.player.blockPosition()));
            }

            return Command.SINGLE_SUCCESS;
        }).then(arg("position", new Vec3Argument()).executes(ctx -> {
            if (mc.player != null) {
                Vec3 startPos = mc.player.position();
                Vec3 pos = ctx.getArgument("position", Vec3.class);
                MeshNode start = phobot.getNavigationMeshManager().getMap().values().stream().min(Comparator.comparingDouble(n -> n.distanceSq(startPos))).orElse(null);
                MeshNode goal = phobot.getNavigationMeshManager().getMap().values().stream().min(Comparator.comparingDouble(n -> n.distanceSq(pos))).orElse(null);
                if (start != null && goal != null) {
                    mc.gui.getChat().addMessage(Component.literal("Going from " + start + " to " + goal));
                    Algorithm<MeshNode> algorithm = new AStar<>(start, goal);
                    var future = CancellationTaskUtil.runWithTimeOut(algorithm, phobot.getTaskService(), 10_000, phobot.getExecutorService());
                    AlgorithmRenderer.render(future, pingBypass.getEventBus(), algorithm);
                    future.thenAccept(list -> {
                        if (list == null) {
                            // fail!
                            return;
                        }

                        Path<MeshNode> path = new Path<>(startPos, pos, BlockPos.containing(startPos), BlockPos.containing(pos), Collections.emptySet(), Algorithm.reverse(list), MeshNode.class);
                        pingBypass.getEventBus().subscribe(new PathRenderer(path, pingBypass));
                        mc.submit(() -> mc.gui.getChat().addMessage(Component.literal("Found path from " + start + " to " + goal)));
                        phobot.getPathfinder().getMovementPathfinder().follow(phobot, path);
                    });
                }
            }

            //phobot.getPathfinder().gotoPosition(pos, MovementNodeMapper.INSTANCE, false);
        }));
    }

}
