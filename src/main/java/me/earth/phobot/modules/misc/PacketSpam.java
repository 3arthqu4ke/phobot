package me.earth.phobot.modules.misc;

import me.earth.phobot.util.math.MathUtil;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.module.impl.ModuleImpl;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.commons.event.SafeListener;
import me.earth.pingbypass.commons.event.loop.GameloopEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;

public class PacketSpam extends ModuleImpl {
    private final Setting<Mode> mode = constant("Mode", Mode.Swing, "The type of packets to send.");
    private final Setting<Integer> delay = number("Delay", 1000, 0, 10_000, "Delay in milliseconds with which we send the batches of packets.");
    private final Setting<Integer> packets = number("Packets", 1, 1, 100_000, "The amount of packets to send every time.");
    private final StopWatch.ForSingleThread timer = new StopWatch.ForSingleThread();

    public PacketSpam(PingBypass pb) {
        super(pb, "PacketSpam", Categories.MISC, "For finding out AntiCheat limits.");
        listen(new SafeListener<GameloopEvent>(mc) {
            @Override
            public void onEvent(GameloopEvent event, LocalPlayer player, ClientLevel clientLevel, MultiPlayerGameMode multiPlayerGameMode) {
                pb.getChat().send(Component.literal("Sending packets in " + MathUtil.round(Math.max(0.0, (delay.getValue() - timer.getPassedTime()) / 1000.0), 1) + "s."), "PacketSpam");
                if (timer.passed(delay.getValue())) {
                    for (int i = 0; i < packets.getValue(); i++) {
                        //noinspection SwitchStatementWithTooFewBranches
                        switch (mode.getValue()) {
                            case Swing -> player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                        }
                    }

                    timer.reset();
                }
            }
        });
    }

    public enum Mode {
        Swing
    }

}
