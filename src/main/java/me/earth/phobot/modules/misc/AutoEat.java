package me.earth.phobot.modules.misc;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.event.PreKeybindHandleEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.commons.event.SafeListener;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

@Slf4j
public class AutoEat extends PhobotModule {
    public AutoEat(Phobot phobot) {
        super(phobot, "AutoEat", Categories.MISC, "Automatically eats Golden Apples.");
        Setting<Integer> ticks = number("Ticks", 2, 0, 30, "Abort eating this many ticks before finishing a Golden Apple if it is redundant. Value probably depends a bit on your latency.");
        listen(new SafeListener<PreKeybindHandleEvent>(mc) {
            @Override
            public void onEvent(PreKeybindHandleEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (player.getUseItemRemainingTicks() <= ticks.getValue()
                        && player.getUseItem().is(Items.ENCHANTED_GOLDEN_APPLE)
                        && player.getAbsorptionAmount() == 16.0f
                        && player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                    // no need to waste Golden Apple, we have all the effects that are important
                    gameMode.releaseUsingItem(player);
                }

                phobot.getInventoryService().use(context -> {
                    Item item = context.find(Items.ENCHANTED_GOLDEN_APPLE);
                    if (item == null) {
                        getPingBypass().getChat().delete("AutoEat");
                        getPingBypass().getChat().send(Component.literal("AutoEat could not find Golden Apples!").withStyle(ChatFormatting.RED), "AutoEat");
                    } else {
                        InventoryContext.SwitchResult switchResult = context.switchTo(Items.ENCHANTED_GOLDEN_APPLE, InventoryContext.PREFER_MAINHAND | InventoryContext.SET_CARRIED_ITEM);
                        if (switchResult != null) {
                            gameMode.useItem(player, switchResult.hand());
                        }

                        event.setCancelled(true);
                    }
                });
            }
        });
    }

}
