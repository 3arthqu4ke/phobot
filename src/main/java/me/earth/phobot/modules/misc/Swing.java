package me.earth.phobot.modules.misc;

import me.earth.phobot.ducks.IServerboundSwingPacket;
import me.earth.phobot.modules.PhobotNameSpacedModule;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.event.network.SendListener;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;

public class Swing extends PhobotNameSpacedModule {
    private final Setting<Mode> mode = constant("Mode", Mode.None, "None will not swing, except for mining, Override will never swing.");

    public Swing(PingBypass pingBypass) {
        super(pingBypass, "Swing", Categories.MISC, "Controls when you swing your hand on the server side.");
        listen(new SendListener<ServerboundSwingPacket>() {
            @Override
            public void onEvent(PacketEvent.Send<ServerboundSwingPacket> event) {
                if (mode.getValue() == Mode.None && !((IServerboundSwingPacket) event.getPacket()).phobot$isUncancellable()) {
                    event.setCancelled(true);
                }
            }
        });
    }

    public static ServerboundSwingPacket uncancellable(InteractionHand hand) { // we need to swing for mining
        ServerboundSwingPacket packet = new ServerboundSwingPacket(hand);
        ((IServerboundSwingPacket) packet).phobot$setUncancellable(true);
        return packet;
    }

    public enum Mode {
        None,
        Override
    }

}
