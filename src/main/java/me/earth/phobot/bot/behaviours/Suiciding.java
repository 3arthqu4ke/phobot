package me.earth.phobot.bot.behaviours;

import me.earth.phobot.bot.Bot;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.time.StopWatch;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Comparator;

/**
 * Kills the player if he does not have enough gear.
 */
public class Suiciding extends Behaviour {
    private final StopWatch.ForSingleThread timer = new StopWatch.ForSingleThread();
    private boolean suicidingDecisionMade = false;

    public Suiciding(Bot bot) {
        super(bot, PRIORITY_SUICIDE);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        bot.getRunningAway().getRunningAwayRequests().remove(this);
        if (bot.getJumpDownFromSpawn().isAboveSpawn(player) || bot.isDueling() || !bot.getSuicide().getValue()) {
            bot.getModules().getSuicide().disable();
            suicidingDecisionMade = false;
            return;
        }

        // TODO: also kill after long fight!
        phobot.getInventoryService().use(ctx -> {
            if (doesNotHaveEnough(Items.END_CRYSTAL, 64, ctx)
                || doesNotHaveEnough(Items.EXPERIENCE_BOTTLE, 8, ctx)
                || doesNotHaveEnough(Items.ENCHANTED_GOLDEN_APPLE, 8, ctx)
                || doesNotHaveEnough(Items.NETHERITE_SWORD, 1, ctx)
                || doesNotHaveEnough(Items.NETHERITE_PICKAXE, 1, ctx)
                || doesNotHaveEnough(Items.NETHERITE_HELMET, 1, ctx)
                || doesNotHaveEnough(Items.NETHERITE_CHESTPLATE, 1, ctx)
                || doesNotHaveEnough(Items.NETHERITE_LEGGINGS, 1, ctx)
                || doesNotHaveEnough(Items.NETHERITE_BOOTS, 1, ctx)
                || doesNotHaveEnough(Items.CHORUS_FRUIT, 1, ctx)
                || doesNotHaveEnough(Items.ENDER_CHEST, 1, ctx) && doesNotHaveEnough(Items.OBSIDIAN, 1, ctx)) {
                Player closestEnemy = level.players().stream()
                        .filter(p -> p != player && !pingBypass.getFriendManager().contains(p.getUUID()))
                        .min(Comparator.comparingDouble(player::distanceToSqr))
                        .orElse(null);
                if (closestEnemy == null || closestEnemy.distanceToSqr(player) > 36.0) {
                    if (!suicidingDecisionMade) {
                        timer.reset();
                    } else if (timer.passed(1000L)) { // wait for a bit before actually suiciding, we might not be able to find an item temporarily due to lag.
                        bot.getModules().getSuicide().enable();
                    }

                    suicidingDecisionMade = true;
                } else {
                    bot.getRunningAway().getRunningAwayRequests().add(this);
                }
            } else {
                bot.getModules().getSuicide().disable();
                suicidingDecisionMade = false;
            }
        });
    }

    private boolean doesNotHaveEnough(Item item, int min, InventoryContext ctx) {
        return ctx.getCount(stack -> stack.is(item)) < min;
    }

}
