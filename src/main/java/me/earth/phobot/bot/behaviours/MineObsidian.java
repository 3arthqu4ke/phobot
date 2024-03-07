package me.earth.phobot.bot.behaviours;

import me.earth.phobot.bot.Bot;
import me.earth.phobot.modules.misc.AutoEchest;
import me.earth.phobot.services.StealingDetectionService;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

/**
 * Uses {@link AutoEchest} to mine Obsidian if we are low on it.
 */
public class MineObsidian extends Behaviour {
    // TODO: currently this can be heavily abused, not last due to the bad stealing detection!
    private final StealingDetectionService stealingDetectionService = new StealingDetectionService();

    public MineObsidian(Bot bot) {
        super(bot, PRIORITY_MINE_AND_REPAIR);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        bot.getRunningAway().getRunningAwayRequests().remove(this);
        phobot.getInventoryService().use(ctx -> {
            int count = ctx.getCount(stack -> stack.is(Items.OBSIDIAN));
            int enderChests = ctx.getCount(stack -> stack.is(Items.ENDER_CHEST));
            if (count < 24 && enderChests > 0) {
                if (stealingDetectionService.couldDropsGetStolen(player, level)) {
                    bot.getRunningAway().getRunningAwayRequests().add(this);
                    bot.getModules().getAutoEchest().disable();
                } else {
                    bot.getModules().getAutoEchest().enable();
                }
            } else if (count >= 64 || enderChests <= 0) {
                bot.getModules().getAutoEchest().disable();
            }
        });
    }

}
