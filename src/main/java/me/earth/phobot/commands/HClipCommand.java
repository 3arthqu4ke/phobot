package me.earth.phobot.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.earth.phobot.Phobot;
import me.earth.phobot.commands.arguments.DoubleArgumentType;
import me.earth.pingbypass.api.command.CommandSource;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import static me.earth.pingbypass.api.command.CommandSource.argument;

public class HClipCommand extends AbstractPhobotCommand {
    public HClipCommand(Phobot phobot) {
        super("hclip", "Moves you forward by a certain amount.", phobot);
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("amount", DoubleArgumentType.doubleArg()).executes(ctx -> {
            LocalPlayer player = ctx.getSource().getMinecraft().player;
            if (player == null) {
                return;
            }

            double amount = ctx.getArgument("amount", Double.class);
            Vec3 dir = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(amount);
            player.setPos(player.position().add(dir));
        }));
    }

}
