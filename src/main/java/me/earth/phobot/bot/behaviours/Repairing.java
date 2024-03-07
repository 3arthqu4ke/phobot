package me.earth.phobot.bot.behaviours;

import me.earth.phobot.bot.Bot;
import me.earth.phobot.modules.misc.Repair;
import me.earth.phobot.services.StealingDetectionService;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

/**
 * Manages {@link RunningAway} for {@link Repair} so that our experience does not get stolen.
 */
public class Repairing extends Behaviour {
    private final StealingDetectionService stealingDetectionService = new StealingDetectionService();

    public Repairing(Bot bot) {
        super(bot, PRIORITY_MINE_AND_REPAIR);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        bot.getRunningAway().getRunningAwayRequests().remove(this);
        Repair.Criticality criticality = bot.getModules().getRepair().getCriticality(player);
        if (criticality.criticality() > 0 && stealingDetectionService.couldDropsGetStolen(player, level)) {
            bot.getRunningAway().getRunningAwayRequests().add(this);
        }
    }

}
