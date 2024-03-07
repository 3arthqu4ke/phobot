package me.earth.phobot.bot.behaviours;

import me.earth.phobot.bot.Bot;
import me.earth.phobot.modules.combat.KillAura;
import me.earth.phobot.modules.combat.autocrystal.AutoCrystal;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.time.StopWatch;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;

/**
 * If we did not find an {@link AutoCrystal} target we need to sword enemies with {@link KillAura}.
 */
public class Swording extends Behaviour {
    private final StopWatch.ForSingleThread timer = new StopWatch.ForSingleThread();

    public Swording(Bot bot) {
        super(bot, PRIORITY_SWORD);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (phobot.getPathfinder().isFollowingPath()
                || bot.getJumpDownFromSpawn().isAboveSpawn(player)
                || bot.getModules().getAutoCrystal().getTarget() != null
                || !bot.getModules().getKillAura().isEnabled()
                || phobot.getInventoryService().isLockedIntoTotem()
                || !timer.passed(1_000L)) {
            return;
        }
        // TODO: fix AutoCrystal now fighting with AutoEat for Offhand, in this case AutoCrystal should make an exception
        KillAura.Target target = bot.getModules().getKillAura().computeTarget(player, level);
        if (target != null) {
            phobot.getInventoryService().use(ctx -> {
                var switchResult = ctx.switchTo(stack -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem,
                                                InventoryContext.PREFER_MAINHAND | InventoryContext.SKIP_OFFHAND);
                if (switchResult != null) {
                    timer.reset();
                }
            });
        }
    }

}
