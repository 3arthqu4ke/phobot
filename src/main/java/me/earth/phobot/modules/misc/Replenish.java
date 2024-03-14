package me.earth.phobot.modules.misc;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.loop.TickEvent;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// TODO: replenish 0?
public class Replenish extends PhobotModule {
    public Replenish(Phobot phobot) {
        super(phobot, "Replenish", Categories.MISC, "");
        Setting<Integer> threshold = number("Threshold", 8, 0, 63, "Minimum item count for us to replenish the slot.");
        listen(new SafeListener<TickEvent>(mc) {
            @Override
            public void onEvent(TickEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                phobot.getInventoryService().use(context -> {
                    for (int i = InventoryMenu.USE_ROW_SLOT_START; i <=/* we also want to check offhand*/ InventoryMenu.USE_ROW_SLOT_END; i++) {
                        Slot hotbarSlot = player.inventoryMenu.getSlot(i);
                        ItemStack hotbarStack = hotbarSlot.getItem();
                        if (!hotbarStack.isEmpty() && hotbarStack.getCount() <= threshold.getValue()) {
                            int bestCount = 0;
                            Slot bestSlot = null;
                            for (int j = player.inventoryMenu.slots.size() - 1; j >= 0; j--) { // iterate in reverse order so that we visit the crafting menu last
                                if (j >= InventoryMenu.USE_ROW_SLOT_START) { // skip hotbar
                                    continue;
                                }

                                Slot slot = player.inventoryMenu.slots.get(j);
                                ItemStack stack = slot.getItem();
                                if (stack.is(hotbarStack.getItem()) && stack.getCount() > bestCount) {
                                    bestCount = stack.getCount();
                                    bestSlot = slot;
                                    if (bestCount == 64) {
                                        break;
                                    }
                                }
                            }

                            if (bestSlot != null && bestSlot != hotbarSlot && bestCount > hotbarStack.getCount()) {
                                new InventoryContext.SwapSwitch(bestSlot, hotbarSlot, false).execute(context);
                                return;
                            }
                        }
                    }
                });
            }
        });
    }

}
