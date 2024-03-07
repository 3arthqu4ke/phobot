package me.earth.phobot.services;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.AbstractEventListener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;

@Slf4j
public class TickPredictionService extends SubscriberImpl {
    private final StopWatch.ForMultipleThreads tickTimer = new StopWatch.ForMultipleThreads();

    public TickPredictionService(Minecraft mc) {
        // this seems to get sent very early during a tick
        listenToPacketThatOnlyGetsSendDuringTick(ClientboundSectionBlocksUpdatePacket.class);
        listenToPacketThatOnlyGetsSendDuringTick(ClientboundTeleportEntityPacket.class);
        listenToPacketThatOnlyGetsSendDuringTick(ClientboundRemoveEntitiesPacket.class);
        listenToPacketThatOnlyGetsSendDuringTick(ClientboundMoveEntityPacket.PosRot.class);
        listenToPacketThatOnlyGetsSendDuringTick(ClientboundMoveEntityPacket.Pos.class);
        listenToPacketThatOnlyGetsSendDuringTick(ClientboundMoveEntityPacket.Rot.class);
        listenToPacketThatOnlyGetsSendDuringTick(ClientboundRotateHeadPacket.class);
        listenToPacketThatOnlyGetsSendDuringTick(ClientboundSetHealthPacket.class);
        listenToPacketThatOnlyGetsSendDuringTick(ClientboundSetExperiencePacket.class);
        listenToPacketThatOnlyGetsSendDuringTick(ClientboundBlockEventPacket.class);
        listenToPacketThatOnlyGetsSendDuringTick(ClientboundMapItemDataPacket.class);
        listenToPacketThatOnlyGetsSendDuringTick(ClientboundMapItemDataPacket.class);
    }

    private <T extends Packet<?>> void listenToPacketThatOnlyGetsSendDuringTick(Class<T> packet) {
        listen(new AbstractEventListener.Unsafe<PacketEvent.Receive<T>>(PacketEvent.Receive.class, packet, Integer.MAX_VALUE) {
            @Override
            public void onEvent(PacketEvent.Receive<T> event) {
                if (tickTimer.passed(40)) {
                    tickTimer.reset();
                }
            }
        });
    }

}
