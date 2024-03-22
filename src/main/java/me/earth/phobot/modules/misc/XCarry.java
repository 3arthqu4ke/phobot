package me.earth.phobot.modules.misc;

import me.earth.phobot.Phobot;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.mixins.network.IServerboundContainerClosePacket;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.event.CancellableEvent;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

public class XCarry extends PhobotModule {
    private final StopWatch.ForSingleThread timer = new StopWatch.ForSingleThread();
    private boolean open = false;

    public XCarry(Phobot phobot) {
        super(phobot, "XCarry", Categories.MISC, "Allows you to use crafting slots as inventory.");
        Setting<Boolean> move = bool("Move", false, "Automatically moves crystals and bottles of experience in your crafting slots.");
        Setting<Boolean> carried = bool("Carried", false, "Automatically moves bottles of experience in your carried slot.");
        Setting<Boolean> allowSwapWithHoldItem = bool("AllowSwapWhileDragging", true, "Allows you to swap items in your inventory with number keys while dragging an item.");
        listen(new SafeListener<PacketEvent.Send<ServerboundContainerClosePacket>>(mc) {
            @Override
            public void onEvent(PacketEvent.Send<ServerboundContainerClosePacket> event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (antiCheatRequiresClose()) {
                    return;
                }

                if (((IServerboundContainerClosePacket) event.getPacket()).getContainerId() == player.inventoryMenu.containerId) {
                    event.setCancelled(true);
                    open = true;
                }
            }
        });

        listen(new Listener<SwapEventWithCarried>() {
            @Override
            public void onEvent(SwapEventWithCarried event) {
                if (allowSwapWithHoldItem.getValue()) {
                    event.setCancelled(true);
                }
            }
        });

        listen(new SafeListener<PostMotionPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (antiCheatRequiresClose() || !move.getValue() && !carried.getValue() || !timer.passed(250L)) {
                    return;
                }

                phobot.getInventoryService().use(context -> {
                    timer.reset();
                    int emptyCount = -1;
                    for (int i = InventoryMenu.CRAFT_SLOT_START; i <= InventoryMenu.CRAFT_SLOT_END; i++) {
                        Slot craftingSlot = player.inventoryMenu.getSlot(i);
                        if (!craftingSlot.hasItem()) {
                            if (emptyCount < 0) {
                                emptyCount = 0;
                                for (int j = InventoryMenu.INV_SLOT_START; j < InventoryMenu.INV_SLOT_END; j++) {
                                    Slot invSlot = player.inventoryMenu.getSlot(j);
                                    if (!invSlot.hasItem()) {
                                        emptyCount++;
                                    }
                                }
                            }

                            if (emptyCount < 6) {
                                for (int j = InventoryMenu.INV_SLOT_START; j < InventoryMenu.INV_SLOT_END; j++) {
                                    Slot invSlot = player.inventoryMenu.getSlot(j);
                                    if (invSlot.hasItem() && invSlot.getItem().is(craftingSlot.index % 2 == 0 ? Items.END_CRYSTAL : Items.EXPERIENCE_BOTTLE)) {
                                        context.moveWithTwoClicks(invSlot, craftingSlot);
                                        return;
                                    }
                                }
                            }
                        }
                    }

                    if (carried.getValue() && emptyCount < 6 && player.inventoryMenu.getCarried().isEmpty()) {
                        for (int j = InventoryMenu.INV_SLOT_START; j < InventoryMenu.INV_SLOT_END; j++) {
                            Slot invSlot = player.inventoryMenu.getSlot(j);
                            if (invSlot.hasItem() && invSlot.getItem().is(Items.EXPERIENCE_BOTTLE)) {
                                context.click(invSlot);
                                return;
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onEnable() {
        open = false;
    }

    @Override
    protected void onDisable() {
        LocalPlayer player = mc.player;
        if (player != null && open) {
            player.closeContainer();
        }

        open = false;
    }

    private boolean antiCheatRequiresClose() {
        if (phobot.getAntiCheat().getCloseInv().getValue()) {
            mc.submit(() -> getPingBypass().getChat().sendWithoutLogging(Component.literal("XCarry is enabled, but won't work due to AntiCheat - CloseInventory."), "XCarry"));
            return true;
        }

        return false;
    }

    public static final class SwapEventWithCarried extends CancellableEvent { }

}
