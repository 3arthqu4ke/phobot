package me.earth.phobot.invalidation;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.event.BlockStateChangeEvent;
import me.earth.phobot.mixins.network.IClientboundSectionBlocksUpdatePacket;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.event.network.PrePostSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractBlockChangeListener<T> extends SubscriberImpl {
    protected final Minecraft mc;
    protected List<T> callbacks;

    public AbstractBlockChangeListener(Minecraft mc) {
        this.mc = mc;
        listenToAll(new PrePostSubscriber<>(ClientboundBlockChangedAckPacket.class) {
            @Override
            public void onPreEvent(PacketEvent.Receive<ClientboundBlockChangedAckPacket> event) {
                // unless we got ghost blocked there should be 0 block changes from this
                handleBlockChangePacket(this, 0);
            }
        });

        listenToAll(new PrePostSubscriber<ClientboundBlockUpdatePacket>() {
            @Override
            public void onPreEvent(PacketEvent.Receive<ClientboundBlockUpdatePacket> event) {
                handleBlockChangePacket(this, 1);
            }
        });

        listenToAll(new PrePostSubscriber<ClientboundSectionBlocksUpdatePacket>() {
            @Override
            public void onPreEvent(PacketEvent.Receive<ClientboundSectionBlocksUpdatePacket> event) {
                int changed = ((IClientboundSectionBlocksUpdatePacket) event.getPacket()).getPositions().length;
                handleBlockChangePacket(this, changed);
            }
        });

        listen(new SafeListener<BlockStateChangeEvent>(mc) {
            @Override
            public void onEvent(BlockStateChangeEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                onBlockStateChange(event.pos().immutable(), event.state(), event.chunk());
            }
        });
    }

    protected abstract void onBlockStateChange(BlockPos pos, BlockState state, LevelChunk chunk);

    protected abstract void handle(List<T> callbacks);

    protected void handleBlockChangePacket(PrePostSubscriber<?> subscriber, int amount) {
        List<T> packetCallbacks = new ArrayList<>(amount);
        mc.submit(() -> {
            this.callbacks = packetCallbacks;
        });

        subscriber.addScheduledPostEvent(mc, () -> {
            if (packetCallbacks != this.callbacks) {
                log.error("Callbacks have changed while processing a BlockChange packet!");
                return;
            }

            this.callbacks = null;
            handle(packetCallbacks);
        });
    }

}
