package me.earth.phobot.services;

import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.commons.event.network.PacketEvent;
import me.earth.pingbypass.commons.event.network.ReceiveListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

public class LagbackService extends SubscriberImpl {
    private final StopWatch.ForMultipleThreads stopWatch = new StopWatch.ForMultipleThreads();

    public LagbackService() {
        listen(new ReceiveListener<ClientboundPlayerPositionPacket>() {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundPlayerPositionPacket> event) {
                stopWatch.reset();
            }
        });
    }

    public boolean passed(long ms) {
        return stopWatch.passed(ms);
    }

}
