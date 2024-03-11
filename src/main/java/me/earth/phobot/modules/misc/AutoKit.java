package me.earth.phobot.modules.misc;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.services.TaskService;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.event.network.PostListener;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

public class AutoKit extends PhobotModule {
    // Xello12121 has multiple stacks of obsidian, we could equip it first then kill then select 3arthqu4ke
    private final Setting<String> kitName = string("Kit", "3arthqu4ke", "The name of the kit you want to equip.");
    private final Setting<Boolean> sendKillCommand = bool("Kill", true, "Kills you once after getting a kit so that you can pick up additional items and fill your XCarry. Has a cooldown.");
    private final StopWatch.ForSingleThread killTimer = new StopWatch.ForSingleThread();
    private boolean expectingPos = false;

    public AutoKit(Phobot phobot) {
        super(phobot, "AutoKit", Categories.MISC, "Automatically equips a kit.");
        listen(new Listener<PacketEvent.PostSend<ServerboundClientCommandPacket>>() {
            @Override
            public void onEvent(PacketEvent.PostSend<ServerboundClientCommandPacket> event) {
                if (mc.isSingleplayer()) {
                    expectingPos = false;
                } else if (event.getPacket().getAction() == ServerboundClientCommandPacket.Action.PERFORM_RESPAWN) {
                    // we cannot request the kit right away, we need to wait until our position has been set
                    expectingPos = true;
                }
            }
        });

        listen(new PostListener.Safe.Direct<ClientboundPlayerPositionPacket>(mc) {
            @Override
            public void onSafePacket(ClientboundPlayerPositionPacket packet, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (mc.isSingleplayer()) {
                    expectingPos = false;
                } else if (expectingPos) {
                    sendKitCommand(player, true);
                }
            }
        });

        listen(new PostListener.Safe.Direct<ClientboundLoginPacket>(mc) {
            @Override
            public void onSafePacket(ClientboundLoginPacket packet, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (mc.isSingleplayer()) {
                    expectingPos = false;
                } else if (player != null) {
                    sendKitCommand(player, true);
                }
            }
        });
    }

    private void sendKitCommand(LocalPlayer player, boolean kill) {
        player.connection.sendCommand("kit " + kitName.getValue());
        expectingPos = false;
        if (kill && phobot.getServerService().isCurrentServerCC() && sendKillCommand.getValue() && killTimer.passed(10_000L)) {
            TaskService taskService = new TaskService(mc);
            taskService.addTaskToBeExecutedIn(1_000L, () -> {
                if (killTimer.passed(10_000L)) {
                    player.connection.sendCommand("kill");
                    player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                    killTimer.reset();
                }

                phobot.getUnloadingEventBus().unsubscribe(taskService);
            });

            phobot.getUnloadingEventBus().subscribe(taskService);
        }
    }

    @Override
    protected void onEnable() {
        this.expectingPos = false;
        LocalPlayer player = mc.player;
        if (player != null) {
            sendKitCommand(player, false);
        }
    }

}
