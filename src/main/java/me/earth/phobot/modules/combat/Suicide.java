package me.earth.phobot.modules.combat;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.combat.autocrystal.Calculation;
import me.earth.phobot.modules.combat.autocrystal.CrystalPlacingModule;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.commons.event.SafeListener;
import me.earth.pingbypass.commons.event.loop.TickEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class Suicide extends CrystalPlacingModule {
    private final Setting<Boolean> command = boolBuilder("Command", false).withDescription("Uses a command instead of crystals to kill you.").build();
    private final Setting<Boolean> armor = boolBuilder("Armor", true).withDescription("Throws away your armor and totems.").build();
    private final StopWatch.ForSingleThread throwTimer = new StopWatch.ForSingleThread();

    public Suicide(Phobot phobot, SurroundService surroundService) {
        super(phobot, surroundService, "Suicide", Categories.COMBAT, "Kills you.");
        registerAfter(command, getByName("Bind").orElseThrow());
        registerAfter(armor, getByName("Command").orElseThrow());
        ResetUtil.disableOnRespawnAndWorldChange(this, mc);
        listen(new SafeListener<TickEvent>(mc) {
            @Override
            public void onEvent(TickEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (!sendCommand() && armor.getValue() && throwTimer.passed(phobot.getAntiCheat().getInventoryDelay().getValue())) {
                    phobot.getInventoryService().use(context -> {
                        for (int i = InventoryMenu.ARMOR_SLOT_START; i < InventoryMenu.ARMOR_SLOT_END; i++) {
                            Slot slot = player.inventoryMenu.getSlot(i);
                            if (!slot.getItem().isEmpty()) {
                                context.drop(slot);
                                throwTimer.reset();
                                return;
                            }
                        }

                        if (context.getOffhand().getItem().is(Items.TOTEM_OF_UNDYING)) {
                            context.drop(context.getOffhand());
                            throwTimer.reset();
                            return;
                        }

                        Slot selected = context.getSelectedSlot();
                        if (selected.getItem().is(Items.TOTEM_OF_UNDYING)) {
                            context.drop(selected);
                            throwTimer.reset();
                            //noinspection UnnecessaryReturnStatement
                            return;
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onEnable() {
        sendCommand();
    }

    private boolean sendCommand() {
        if (command.getValue()) {
            LocalPlayer player = mc.player;
            if (player != null) {
                player.connection.sendCommand("kill");
            }

            this.disable();
            return true;
        }

        return false;
    }

    @Override
    public Calculation createCalculation(ClientLevel level, LocalPlayer player) {
        return new Calculation(this, mc, phobot, player, level, phobot.getAntiCheat().getDamageCalculator()) {
            @Override
            protected float getSelfBreakDamage(Entity crystal) {
                return 0.0f;
            }

            @Override
            protected float getSelfPlaceDamage(BlockPos pos, Level level) {
                return 0.0f;
            }

            @Override
            protected double getMaxY() {
                return player.getY() + 2;
            }

            @Override
            protected boolean isValidPlayer(Player enemy, double x, double y, double z) {
                return enemy == player;
            }
        };
    }

}
