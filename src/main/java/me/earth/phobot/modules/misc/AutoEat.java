package me.earth.phobot.modules.misc;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.event.PreKeybindHandleEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.modules.combat.KillAura;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.InventoryUtil;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.module.impl.Categories;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

@Slf4j
public class AutoEat extends PhobotModule {
    public AutoEat(Phobot phobot, KillAura killAura) {
        super(phobot, "AutoEat", Categories.MISC, "Automatically eats Golden Apples.");
        // TODO: calculate from ping?
        var ticks = number("Ticks", 2, 0, 30, "Abort eating this many ticks before finishing a Golden Apple if it is redundant. Value probably depends a bit on your latency.");
        listen(new SafeListener<PreKeybindHandleEvent>(mc) {
            @Override
            public void onEvent(PreKeybindHandleEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (event.isCancelled() || mc.options.keyUse.isDown() && isHoldingFood(player)) {
                    return;
                }

                if (player.getUseItemRemainingTicks() <= ticks.getValue()
                        && player.getUseItem().is(Items.ENCHANTED_GOLDEN_APPLE)
                        && player.getAbsorptionAmount() == 16.0f
                        && player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                    // no need to waste Golden Apple, we have all the effects that are important
                    gameMode.releaseUsingItem(player);
                }

                if (!player.getUseItem().isEmpty() && player.isUsingItem()) {
                    event.setCancelled(true);
                    return;
                }

                // TODO: optimize eating Gapples with Crapples
                phobot.getInventoryService().use(context -> {
                    Item item = context.find(Items.ENCHANTED_GOLDEN_APPLE);
                    if (item == null) {
                        getPingBypass().getChat().delete("AutoEat");
                        getPingBypass().getChat().sendWithoutLogging(Component.literal("AutoEat could not find Golden Apples!").withStyle(ChatFormatting.RED), "AutoEat");
                    } else {
                        int flags = InventoryContext.PREFER_MAINHAND | InventoryContext.SET_CARRIED_ITEM;
                        if (killAura.isEnabled()
                                && !phobot.getInventoryService().isLockedIntoTotem()
                                && InventoryUtil.isHoldingWeapon(player)) {
                            flags = InventoryContext.DEFAULT_SWAP_SWITCH;
                        }

                        InventoryContext.SwitchResult switchResult = context.switchTo(Items.ENCHANTED_GOLDEN_APPLE, flags);
                        if (switchResult != null) {
                            // sometimes we start eating twice, probably because the server sends finished eating back after we started the next Gapple?
                            gameMode.useItem(player, switchResult.hand());
                        }

                        event.setCancelled(true);
                    }
                });
            }
        });
    }

    private boolean isHoldingFood(LocalPlayer player) {
        return player.getItemInHand(InteractionHand.OFF_HAND).getItem().isEdible()
                || player.getItemInHand(InteractionHand.MAIN_HAND).getItem().isEdible();
    }

}
