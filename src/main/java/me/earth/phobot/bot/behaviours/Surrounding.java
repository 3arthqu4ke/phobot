package me.earth.phobot.bot.behaviours;

import me.earth.phobot.bot.Bot;
import me.earth.phobot.modules.combat.Surround;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

/**
 * Handles toggling {@link Surround} while we are standing still.
 */
public class Surrounding extends Behaviour {
    public Surrounding(Bot bot) {
        super(bot, PRIORITY_LAST); // priority does not really matter for this
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (phobot.getPathfinder().isFollowingPath()
                || bot.getJumpDownFromSpawn().isAboveSpawn(player)
                || level.players().stream() // no players near us, no need to surround
                        .filter(p -> p != player)
                        .filter(p -> !bot.getJumpDownFromSpawn().isAboveSpawn(p))
                        .filter(p -> !pingBypass.getFriendManager().contains(p.getUUID()))
                        .noneMatch(p -> p.distanceToSqr(player) < 40.0 * 40.0)) {
            return;
        }

        bot.getModules().getSurround().enable();
    }

}
