package me.earth.phobot.services;

import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.commons.event.network.PacketEvent;
import me.earth.pingbypass.commons.event.network.ReceiveListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class BlockUpdateService extends SubscriberImpl {
    private final Map<BlockPos, Entry> map = new ConcurrentHashMap<>();

    public BlockUpdateService() {
        listen(new Listener<PacketEvent.PostSend<ServerboundUseItemOnPacket>>(Integer.MIN_VALUE) {
            @Override
            public void onEvent(PacketEvent.PostSend<ServerboundUseItemOnPacket> event) {
                ServerboundUseItemOnPacket packet = event.getPacket();
                addBlockPos(packet.getHitResult().getBlockPos(), packet, true, false, null);
                addBlockPos(packet.getHitResult().getBlockPos().relative(packet.getHitResult().getDirection()), packet, true, false, null);
            }
        });

        listen(new Listener<PacketEvent.PostSend<ServerboundPlayerActionPacket>>() {
            @Override
            public void onEvent(PacketEvent.PostSend<ServerboundPlayerActionPacket> event) {
                if (event.getPacket().getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                    addBlockPos(event.getPacket().getPos(), event.getPacket(), false, false, null);
                }
            }
        });

        // also gets send by SignBlockEntity, failed break and falling block entity, no way to detect
        listen(new ReceiveListener<ClientboundBlockUpdatePacket>() {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundBlockUpdatePacket> event) {
                synchronized (map) {
                    Entry entry = map.get(event.getPacket().getPos());
                    if (entry != null) {
                        if (!popFromMap(entry.immediate)) {
                            popFromMap(entry.nextTick);
                        }
                    }
                }
            }
        });
    }

    public void addBlockPos(BlockPos pos, Packet<?> packet, boolean immediate, boolean force, @Nullable Consumer<Long> callback) {
        synchronized (map) {
            Entry entry = map.computeIfAbsent(pos, v -> new Entry(new LinkedHashMap<>(), new LinkedHashMap<>(), TimeUtil.getMillis()));
            if (immediate) {
                putOrPutIfAbsent(entry.immediate, packet, v -> new TimeStamp(TimeUtil.getMillis(), callback), force);
            } else {
                putOrPutIfAbsent(entry.nextTick, packet, v -> new TimeStamp(TimeUtil.getMillis(), callback), force);
            }
        }
    }

    private boolean popFromMap(Map<Packet<?>, TimeStamp> map) {
        Iterator<Map.Entry<Packet<?>, TimeStamp>> itr = map.entrySet().iterator();
        if (itr.hasNext()) {
            Map.Entry<Packet<?>, TimeStamp> entry = itr.next();
            Consumer<Long> callback = entry.getValue().callback();
            if (callback != null) {
                callback.accept(entry.getValue().timeStamp());
            }

            itr.remove();
            return true;
        }

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    private <K,V> V putOrPutIfAbsent(Map<K,V> map, K key, Function<? super K, ? extends V> mappingFunction, boolean put) {
        return put ? map.put(key, mappingFunction.apply(key)) : map.computeIfAbsent(key, mappingFunction);
    }

    private record Entry(Map<Packet<?>, TimeStamp> immediate, Map<Packet<?>, TimeStamp> nextTick, long time) { }

    private record TimeStamp(long timeStamp, @Nullable Consumer<Long> callback) { }

}
