package me.earth.phobot.modules.combat;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.combat.autocrystal.AutoCrystal;
import me.earth.phobot.modules.combat.autocrystal.Calculation;
import me.earth.phobot.modules.combat.autocrystal.CrystalPlacingModule;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.command.CommandSource;
import me.earth.pingbypass.api.command.impl.module.HasCustomModuleCommand;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.loop.TickEvent;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import static me.earth.pingbypass.api.command.CommandSource.literal;

public class Suicide extends CrystalPlacingModule implements HasCustomModuleCommand {
    private final Setting<Boolean> armor = boolBuilder("Armor", true).withDescription("Throws away your armor and totems.").build();
    private final Setting<Mode> mode = enumBuilder("Mode", Mode.All).withDescription("The mode to use.").build();
    private final StopWatch.ForSingleThread commandTimer = new StopWatch.ForSingleThread();
    private final StopWatch.ForSingleThread throwTimer = new StopWatch.ForSingleThread();

    public Suicide(Phobot phobot, SurroundService surroundService) {
        super(phobot, surroundService, "Suicide", Categories.COMBAT, "Kills you.");
        registerAfter(mode, getByName("Bind").orElseThrow());
        registerAfter(armor, getByName("Mode").orElseThrow());
        ResetUtil.disableOnRespawnAndWorldChange(this, mc);
        listen(new SafeListener<TickEvent>(mc) {
            @Override
            public void onEvent(TickEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                sendCommand();
                if (mode.getValue() != Mode.Command && armor.getValue() && throwTimer.passed(phobot.getAntiCheat().getInventoryDelay().getValue())) {
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
        commandTimer.setTimeStamp(0L);
        sendCommand();
    }

    private void sendCommand() {
        if (mode.getValue() != Mode.AutoCrystal && commandTimer.passed(4_000L)) {
            LocalPlayer player = mc.player;
            if (player != null) {
                player.connection.sendCommand("kill");
                commandTimer.reset();
            }
        }
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

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("SyncWithAutoCrystal").executes(ctx -> {
            AutoCrystal autoCrystal = getPingBypass().getModuleManager().getByClass(AutoCrystal.class).orElse(null);
            if (autoCrystal != null) {
                for (Setting setting : autoCrystal) {
                    if ("Enabled".equalsIgnoreCase(setting.getName()) || "Bind".equalsIgnoreCase(setting.getName())) {
                        continue;
                    }

                    syncSetting(setting, setting.getType());
                }

                getPingBypass().getChat().send(Component.literal("Synced Suicide with AutoCrystal successfully."), "SuicideSync");
            }
        }));
    }

    private <T> void syncSetting(Setting<T> autoCrystalSetting, Class<T> type) {
        getSetting(autoCrystalSetting.getName(), type).ifPresent(ourSetting -> ourSetting.setValue(autoCrystalSetting.getValue()));
    }

    public enum Mode {
        Command,
        AutoCrystal,
        All
    }

}
