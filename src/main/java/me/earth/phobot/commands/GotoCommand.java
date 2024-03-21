package me.earth.phobot.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.earth.phobot.Phobot;
import me.earth.phobot.commands.arguments.LiteralsOr;
import me.earth.phobot.commands.arguments.Vec3Argument;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.util.CancellableFuture;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.mutables.MutPos;
import me.earth.pingbypass.api.command.CommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class GotoCommand extends AbstractPhobotCommand {
    private final Object lock = new Object();
    private CancellableFuture<?> pathFuture;
    private CancellableFuture<?> followFuture;

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
        }).then(arg("position", new LiteralsOr(new Vec3Argument(), "cancel")).executes(ctx -> {
            synchronized (lock) {
                cancel();
                LocalPlayer player = mc.player;
                Object p = ctx.getArgument("position", Object.class);
                if (player != null && p instanceof Vec3 vec3) {
                    BlockPos pos = BlockPos.containing(vec3);
                    MeshNode goal = phobot.getNavigationMeshManager().getMap().values().stream().min(Comparator.comparingDouble(n -> n.distanceSq(pos))).orElse(null);
                    if (goal != null) {
                        pingBypass.getChat().send(
                                Component.literal("Going from " + PositionUtil.toSimpleString(player.blockPosition()) + " to " + PositionUtil.toSimpleString(goal)));
                        var pathFuture = phobot.getPathfinder().findPath(player, goal, true);
                        pathFuture.whenComplete((result,t) -> {
                            synchronized (lock) {
                                if (this.pathFuture == pathFuture) {
                                    this.pathFuture = null;
                                }

                                Level level = mc.level;
                                if (result != null && level != null) {
                                    MutPos mutPos = new MutPos();
                                    mutPos.set(goal.getX(), goal.getY() - 1, goal.getZ());
                                    double y = PositionUtil.getMaxYAtPosition(mutPos, level);
                                    Vec3 exactGoal = new Vec3(goal.getX() + 0.5, y, goal.getZ() + 0.5);
                                    followFuture = phobot.getPathfinder().follow(phobot, result, exactGoal);
                                }
                            }
                        });

                        this.pathFuture = pathFuture;
                    }
                } else if (player == null) {
                    pingBypass.getChat().send(Component.literal("You need to be in game to use this command.").withStyle(ChatFormatting.RED));
                }
            }
        }));
    }

    private void cancel() {
        synchronized (lock) {
            phobot.getPathfinder().cancel();
            var pf = this.pathFuture;
            if (pf != null) {
                pf.cancel(true);
            }

            var ff = this.followFuture;
            if (ff != null) {
                ff.cancel(true);
            }

            pathFuture = null;
            followFuture = null;
        }
    }

}
