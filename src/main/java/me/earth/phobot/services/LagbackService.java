package me.earth.phobot.services;

import lombok.Getter;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.event.network.ReceiveListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

@Getter
public class LagbackService extends SubscriberImpl {
    private final StopWatch.ForMultipleThreads stopWatch = new StopWatch.ForMultipleThreads();
    private volatile long timeSinceLastLag = 0L;

    public LagbackService() {
        listen(new ReceiveListener<ClientboundPlayerPositionPacket>() {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundPlayerPositionPacket> event) {
                timeSinceLastLag = stopWatch.getPassedTime();
                stopWatch.reset();
            }
        });
    }

    public boolean passed(long ms) {
        return stopWatch.passed(ms);
    }

}
