package me.earth.phobot.services;

import lombok.Getter;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.commons.event.SafeListener;
import me.earth.pingbypass.commons.event.loop.TickEvent;
import me.earth.pingbypass.commons.event.network.PacketEvent;
import me.earth.pingbypass.commons.event.network.ReceiveListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class BlockDestructionService extends SubscriberImpl {
    private final Map<BlockPos, Progress> positions = new ConcurrentHashMap<>();

    public BlockDestructionService(Minecraft mc) {
        listen(new ReceiveListener.Scheduled.Safe<ClientboundBlockDestructionPacket>(mc) {
            @Override
            public void onSafeEvent(PacketEvent.Receive<ClientboundBlockDestructionPacket> event, LocalPlayer self, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (!level.getBlockState(event.getPacket().getPos()).isAir()) {
                    addProgress(event.getPacket().getPos(), level);
                }
            }
        });

        listen(new ReceiveListener.Scheduled.Safe<ClientboundBlockUpdatePacket>(mc) {
            @Override
            public void onSafeEvent(PacketEvent.Receive<ClientboundBlockUpdatePacket> event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (event.getPacket().getBlockState().isAir() && positions.containsKey(event.getPacket().getPos())) {
                    addProgress(event.getPacket().getPos(), level); // mined, reset timeStamp
                }
            }
        });

        listen(new SafeListener<TickEvent>(mc) {
            @Override
            public void onEvent(TickEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                positions.entrySet().removeIf(entry -> entry.getValue().isInvalid());
            }
        });

        ResetUtil.onRespawnOrWorldChange(this, mc, positions::clear);
    }

    private void addProgress(BlockPos pos, ClientLevel level) {
        List<Player> players = new ArrayList<>();
        for (Player player : level.players()) {
            if (player.getEyePosition().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 7.0 * 7.0) {
                players.add(player);
            }
        }

        positions.put(pos, new Progress(pos, TimeUtil.getMillis(), players));
    }

    public record Progress(BlockPos pos, long timeStamp, List<Player> players) {
        public boolean isInvalid() {
            players.removeIf(player -> player.isRemoved() || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) >= 7.0 * 7.0);
            return players.isEmpty();
        }
    }

}
