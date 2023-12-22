package me.earth.phobot.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.command.CommandSource;
import me.earth.pingbypass.commons.command.AbstractPbCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class TeleportCommand extends AbstractPbCommand {
    public TeleportCommand(PingBypass pingBypass) {
        super("Teleport", "Teleports you somewhere.", pingBypass, pingBypass.getMinecraft());
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(arg("position", new Vec3Argument()).executes(ctx -> {
            Vec3 pos = ctx.getArgument("position", Vec3.class);
            LocalPlayer player = mc.player;
            if (player != null) {
                pingBypass.getChat().send(Component.literal("Teleporting you to " + PositionUtil.toSimpleString(pos)).withStyle(ChatFormatting.GREEN));
                player.setPos(pos);
            } else {
                pingBypass.getChat().send(Component.literal("You need to be ingame to use this command.").withStyle(ChatFormatting.RED));
            }
        }));
    }

}
