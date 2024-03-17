package me.earth.phobot.services.inventory;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.earth.phobot.mixins.network.IMultiPlayerGameMode;
import me.earth.pingbypass.api.input.Keys;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class InventoryContext {
    public static final int DEFAULT_SWAP_SWITCH = 0;
    public static final int PREFER_MAINHAND = 1;
    public static final int SWITCH_BACK = 2;
    public static final int SET_CARRIED_ITEM = 4;
    public static final int SKIP_OFFHAND = 8;

    private final List<Switch> switches = new ArrayList<>();
    private final InventoryService inventoryService;
    private final LocalPlayer player;
    private final MultiPlayerGameMode gameMode;
    private HotbarSwitch hotbarSwitch;

    public @Nullable SwitchResult switchTo(Item item, int flags) {
        return switchTo(stack -> stack.is(item), flags);
    }

    public @Nullable SwitchResult switchTo(Predicate<ItemStack> check, int flags) {
        if (!(player.containerMenu instanceof InventoryMenu)) {
            return null;
        }

        if ((flags & SKIP_OFFHAND) == 0 && check.test(player.inventoryMenu.getSlot(InventoryMenu.SHIELD_SLOT).getItem())) {
            return SwitchResult.get(player.inventoryMenu.getSlot(InventoryMenu.SHIELD_SLOT));
        } else if (check.test(getSelectedSlot().getItem())) {
            return SwitchResult.get(getSelectedSlot());
        }

        Slot slot = find(s -> check.test(s.getItem()) ? s : null);
        return switchTo(slot, flags);
    }

    public @Nullable SwitchResult switchTo(@Nullable Slot slot, int flags) {
        if (slot != null) {
            if ((flags & SKIP_OFFHAND) == 0 && slot.index == InventoryMenu.SHIELD_SLOT) {
                return SwitchResult.get(player.inventoryMenu.getSlot(InventoryMenu.SHIELD_SLOT));
            } else if (slot.getContainerSlot() == player.getInventory().selected) {
                return SwitchResult.get(getSelectedSlot());
            }

            Slot targetSlot = player.inventoryMenu.getSlot(InventoryMenu.SHIELD_SLOT);
            boolean slotIsHotbar = isHotbar(slot);
            if ((flags & PREFER_MAINHAND) != 0 || (flags & SET_CARRIED_ITEM) != 0 && slotIsHotbar) {
                targetSlot = getSelectedSlot();
            }

            // TODO: prefer SwapSwitch if we are mining!
            Switch action = (flags & SET_CARRIED_ITEM) != 0 && slotIsHotbar && isHotbar(targetSlot)
                    ? new HotbarSwitch(targetSlot, slot, (flags & SWITCH_BACK) != 0)
                    : new SwapSwitch(slot, targetSlot, (flags & SWITCH_BACK) != 0);

            action.execute(this);
            if (action instanceof HotbarSwitch hs) {
                if (this.hotbarSwitch == null) {
                    this.hotbarSwitch = hs;
                }
            } else {
                switches.add(action);
            }

            /* TODO: if (isHotbar(slot) && (... || anticheat flagged too many swaps)) -> HotbarSwitch*/
            return SwitchResult.get(targetSlot, action);
        }

        return null;
    }

    public void moveWithTwoClicks(Slot from, Slot to) {
        if (player.containerMenu instanceof InventoryMenu) {
            gameMode.handleInventoryMouseClick(player.containerMenu.containerId, from.index, Keys.MOUSE_1, ClickType.PICKUP, player);
            gameMode.handleInventoryMouseClick(player.containerMenu.containerId, to.index, Keys.MOUSE_1, ClickType.PICKUP, player);
            closeScreen();
        }
    }

    public void click(Slot slot) {
        if (player.containerMenu instanceof InventoryMenu) {
            gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slot.index, Keys.MOUSE_1, ClickType.PICKUP, player);
            closeScreen();
        }
    }

    public void drop(Slot slot) {
        gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, slot.index, 0, ClickType.THROW, player);
        closeScreen();
    }

    public Slot getOffhand() {
        return player.inventoryMenu.getSlot(InventoryMenu.SHIELD_SLOT);
    }

    public static InteractionHand getHand(Slot slot) {
        return slot.index == InventoryMenu.SHIELD_SLOT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    public boolean has(Item... items) {
        return find(items) != null;
    }

    @SuppressWarnings({"unchecked", "RedundantCast"})
    public Item find(Item... items) {
        return (Item) find(Arrays.stream(items).map(this::findItemFunction).toArray(Function[]::new));
    }

    @SuppressWarnings({"unchecked", "RedundantCast"})
    public Block findBlock(Block... blocks) {
        return (Block) find(Arrays.stream(blocks).map(this::findBlockFunction).toArray(Function[]::new));
    }

    @SafeVarargs
    public final <T> @Nullable T find(Function<Slot, @Nullable T>... functions) {
        for (var function : functions) {
            T result = function.apply(player.inventoryMenu.getSlot(InventoryMenu.SHIELD_SLOT));
            if (result != null) {
                return result;
            }

            result = function.apply(getSelectedSlot());
            if (result != null) {
                return result;
            }

            for (int i = InventoryMenu.USE_ROW_SLOT_START; i < InventoryMenu.USE_ROW_SLOT_END; i++) {
                result = function.apply(player.inventoryMenu.getSlot(i));
                if (result != null) {
                    return result;
                }
            }

            for (int i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.INV_SLOT_END; i++) {
                result = function.apply(player.inventoryMenu.getSlot(i));
                if (result != null) {
                    return result;
                }
            }

            for (int i = InventoryMenu.CRAFT_SLOT_START; i < InventoryMenu.CRAFT_SLOT_END; i++) {
                result = function.apply(player.inventoryMenu.getSlot(i));
                if (result != null) {
                    return result;
                }
            }

            // TODO: carried slot
        }

        return null;
    }

    public int getCount(Predicate<ItemStack> check) {
        int count = 0;
        for (Slot slot : player.inventoryMenu.slots) {
            if (check.test(slot.getItem())) {
                count += slot.getItem().getCount();
            }
        }

        return count;
    }

    private int toInventoryMenuSlot(int slot) {
        if (slot == Inventory.SLOT_OFFHAND) {
            return InventoryMenu.SHIELD_SLOT;
        }

        if (slot >= 0 && slot < Inventory.getSelectionSize()/*9*/) {
            return InventoryMenu.USE_ROW_SLOT_START + slot;
        }

        if (slot < 0 || slot > InventoryMenu.USE_ROW_SLOT_END) {
            return Inventory.NOT_FOUND_INDEX;
        }

        return slot;
    }

    public Slot getSelectedSlot() {
        int slotIndex = toInventoryMenuSlot(player.getInventory().selected);
        if (slotIndex < InventoryMenu.USE_ROW_SLOT_START || slotIndex >= InventoryMenu.USE_ROW_SLOT_END) {
            slotIndex = InventoryMenu.USE_ROW_SLOT_START;
        }

        return player.inventoryMenu.getSlot(slotIndex);
    }

    public boolean isSelected(Slot slot) {
        return slot.getContainerSlot() == getSelectedSlot().getContainerSlot();
    }

    public boolean isOffHand(Slot slot) {
        return slot.getContainerSlot() == Inventory.SLOT_OFFHAND;
    }

    void end() {
        if (inventoryService.getSwitchBack().get() /* or was eating!!!*/) {
            for (int i = switches.size() - 1; i >= 0; i--) {
                Switch action = switches.get(i);
                if (!action.switchBack || !action.revert(this)) {
                    break;
                }
            }

            if (hotbarSwitch != null && hotbarSwitch.switchBack) {
                hotbarSwitch.revert(this);
            }
        }
    }

    private void closeScreen() {
        if (inventoryService.getAntiCheat().getCloseInv().getValue() && player.containerMenu == player.inventoryMenu) {
            player.connection.send(new ServerboundContainerClosePacket(player.containerMenu.containerId));
        }
    }

    private Function<Slot, Block> findBlockFunction(Block block) {
        return slot -> slot.getItem().is(block.asItem()) && slot.getItem().getCount() != 0 ? block : null;
    }

    private Function<Slot, Item> findItemFunction(Item item) {
        return slot -> slot.getItem().is(item) && slot.getItem().getCount() != 0 ? item : null;
    }

    private static boolean isHotbar(Slot slot) {
        return slot.getContainerSlot() >= 0 && slot.getContainerSlot() < Inventory.getSelectionSize()/*9*/;
    }

    public record SwitchResult(Slot slot, InteractionHand hand, @Nullable Block block, @Nullable Switch action) {
        public static SwitchResult get(Slot slot) {
            return get(slot, null);
        }

        public static SwitchResult get(Slot slot, @Nullable Switch action) {
            return new SwitchResult(slot, getHand(slot), slot.getItem().getItem() instanceof BlockItem block ? block.getBlock() : null, action);
        }
    }

    @RequiredArgsConstructor
    public abstract static class Switch {
        protected final Slot from;
        protected final Slot to;
        protected final boolean switchBack;

        public abstract boolean execute(InventoryContext context, Slot from, Slot to);

        public boolean execute(InventoryContext context) {
            return execute(context, from, to);
        }

        public boolean revert(InventoryContext context) {
            return execute(context, to, from);
        }
    }

    public static class SwapSwitch extends Switch {
        public SwapSwitch(Slot from, Slot to, boolean silent) {
            super(from, to, silent);
        }

        @Override
        public boolean execute(InventoryContext context, Slot from, Slot to) {
            if (context.player.containerMenu instanceof InventoryMenu && (isHotbar(to) || to.getContainerSlot() == Inventory.SLOT_OFFHAND)) {
                context.gameMode.handleInventoryMouseClick(context.player.containerMenu.containerId, from.index, to.getContainerSlot(), ClickType.SWAP, context.player);
                context.closeScreen();
                return true;
            }

            return false;
        }

        @Override
        public boolean revert(InventoryContext context) {
            return execute(context, from, to);
        }
    }

    public static class HotbarSwitch extends Switch {
        public HotbarSwitch(Slot from, Slot to, boolean silent) {
            super(from, to, silent);
        }

        @Override
        public boolean execute(InventoryContext context, Slot from, Slot to) {
            if (isHotbar(to)) {
                context.player.getInventory().selected = to.getContainerSlot();
                // TODO: via reflection we could actually register an ASM transformer to make GameMode.carriedIndex volatile?!
                ((IMultiPlayerGameMode) context.gameMode).invokeEnsureHasSentCarriedItem();
                return true;
            }

            return false;
        }
    }

}
