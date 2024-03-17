package me.earth.phobot.bot.behaviours;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.modules.client.anticheat.MovementAntiCheat;
import me.earth.phobot.modules.combat.KillAura;
import me.earth.phobot.modules.combat.autocrystal.AutoCrystal;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.InventoryUtil;
import me.earth.phobot.util.time.StopWatch;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * If we did not find an {@link AutoCrystal} target we need to sword enemies with {@link KillAura}.
 */
@Slf4j
public class Swording extends Behaviour {
    private final StopWatch.ForSingleThread timer = new StopWatch.ForSingleThread();

    public Swording(Bot bot) {
        super(bot, PRIORITY_SWORD);
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        boolean stoppedBecauseTimer = false;
        if (phobot.getPathfinder().isFollowingPath()
                || !bot.getAura().getValue()
                || bot.getJumpDownFromSpawn().isAboveSpawn(player)
                || bot.getModules().getAutoCrystal().getTarget() != null
                || !bot.getModules().getKillAura().isEnabled()
                || phobot.getInventoryService().isLockedIntoTotem()
                || !bot.getSurroundService().isSurrounded()
                || !bot.getModules().getBomber().getLastBombing().passed(3_000L) // Bombing is cooler than Swording! TODO: also when we are about to bomb?!
                || (stoppedBecauseTimer = !timer.passed(3_500L))) { // <- eating Golden Apple + Chorus Fruit + Some ms leniency
            boolean finalStoppedBecauseTimer = stoppedBecauseTimer;
            phobot.getInventoryService().use(ctx -> {
                // On Grim AntiCheat the NoSlowDown for Offhand is terrible, as it requires us to switch items
                // So since this is the only thing that might switch Golden Apples into our Offhand we need to switch back
                if (!finalStoppedBecauseTimer
                        && phobot.getAntiCheat().getMovement().getValue() == MovementAntiCheat.Grim
                        // && player.getUsedItemHand() == InteractionHand.OFF_HAND TODO: why???
                        && player.getItemInHand(InteractionHand.OFF_HAND).is(Items.ENCHANTED_GOLDEN_APPLE)
                        && !InventoryUtil.isHoldingWeapon(player)) {
                    switchGoldenApplesFromOffToMainHand(player);
                }
            });

            return;
        }
        // TODO: fix AutoCrystal now fighting with AutoEat for Offhand, in this case AutoCrystal should make an exception
        KillAura.Target target = bot.getModules().getKillAura().computeTarget(player, level);
        if (target != null) {
            phobot.getInventoryService().use(ctx -> {
                var switchResult = ctx.switchTo(InventoryUtil::isWeapon, InventoryContext.PREFER_MAINHAND | InventoryContext.SKIP_OFFHAND);
                if (switchResult != null) {
                    timer.reset();
                }
            });
        }
    }

    private void switchGoldenApplesFromOffToMainHand(LocalPlayer player) {
        player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
        player.setItemInHand(InteractionHand.OFF_HAND, player.getItemInHand(InteractionHand.MAIN_HAND));
        player.setItemInHand(InteractionHand.MAIN_HAND, offhand);
        player.stopUsingItem();
        player.startUsingItem(InteractionHand.MAIN_HAND);
    }

}
