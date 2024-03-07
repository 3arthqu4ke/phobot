package me.earth.phobot.bot.behaviours;

import me.earth.phobot.bot.Bot;
import me.earth.phobot.modules.combat.AutoTrap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

/**
 * Handles the toggling of {@link AutoTrap}.
 */
public class Trapping extends Behaviour {
    public Trapping(Bot bot) {
        super(bot, PRIORITY_LAST); // priority does not really matter for this
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (phobot.getPathfinder().isFollowingPath() || bot.getJumpDownFromSpawn().isAboveSpawn(player)) {
            bot.getModules().getAutoTrap().disable();
            return;
        }
        // TODO: In AutoTrap, dont trap moving targets!
        // TODO: AutoTrap do not trap us!
        phobot.getInventoryService().use(ctx -> {
            if (ctx.getCount(stack -> stack.is(Items.OBSIDIAN)) > 8) {
                bot.getModules().getAutoTrap().enable();
            } else {
                bot.getModules().getAutoTrap().disable();
            }
        });
    }

}
