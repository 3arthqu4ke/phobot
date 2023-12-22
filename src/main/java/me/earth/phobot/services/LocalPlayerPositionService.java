package me.earth.phobot.services;

import lombok.AccessLevel;
import lombok.Getter;
import me.earth.phobot.event.ChangeWorldEvent;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.player.DamageCalculatorPlayer;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.commons.event.SafeListener;
import me.earth.pingbypass.commons.event.network.PacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

@Getter
@SuppressWarnings("NonAtomicOperationOnVolatileField") // is volatile only for the reads
public class LocalPlayerPositionService extends PlayerPositionService {
    private static final int THRESHOLD = 400;
    private final Deque<PlayerPosition> tickPositions = new ArrayDeque<>(THRESHOLD);
    private final Minecraft mc;
    @Getter(AccessLevel.NONE)
    private DamageCalculatorPlayer playerOnLastPosition = null;

    public LocalPlayerPositionService(Minecraft mc) {
        super(THRESHOLD);
        this.mc = mc;
        listen(new Listener<PostMotionPlayerUpdateEvent>() {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event) {
                addToPositions(position, tickPositions);
            }
        });
        
        listen(new Listener<ChangeWorldEvent>() {
            @Override
            public void onEvent(ChangeWorldEvent event) {
                position = new PlayerPosition();
                playerOnLastPosition = null;
                tickPositions.clear();
            }
        });

        listen(new Listener<PacketEvent.PostSend<ServerboundMovePlayerPacket.StatusOnly>>(Integer.MAX_VALUE) {
            @Override
            public void onEvent(PacketEvent.PostSend<ServerboundMovePlayerPacket.StatusOnly> event) {
                position = new PlayerPosition(position, event.getPacket().isOnGround());
                addToPositions(position, positions);
            }
        });

        listen(new Listener<PacketEvent.PostSend<ServerboundMovePlayerPacket.Rot>>(Integer.MAX_VALUE) {
            @Override
            public void onEvent(PacketEvent.PostSend<ServerboundMovePlayerPacket.Rot> event) {
                position = new PlayerPosition(position, event.getPacket().getXRot(position.getXRot()), event.getPacket().getYRot(position.getYRot()), event.getPacket().isOnGround());
                addToPositions(position, positions);
            }
        });

        listen(new Listener<PacketEvent.PostSend<ServerboundMovePlayerPacket.Pos>>(Integer.MAX_VALUE) {
            @Override
            public void onEvent(PacketEvent.PostSend<ServerboundMovePlayerPacket.Pos> event) {
                position = new PlayerPosition(position,
                        event.getPacket().getX(position.getX()),
                        event.getPacket().getY(position.getY()),
                        event.getPacket().getZ(position.getZ()),
                        event.getPacket().isOnGround());
                addToPositions(position, positions);
            }
        });

        listen(new Listener<PacketEvent.PostSend<ServerboundMovePlayerPacket.PosRot>>(Integer.MAX_VALUE) {
            @Override
            public void onEvent(PacketEvent.PostSend<ServerboundMovePlayerPacket.PosRot> event) {
                position = new PlayerPosition(event.getPacket().getX(position.getX()),
                                                event.getPacket().getY(position.getY()),
                                                event.getPacket().getZ(position.getZ()),
                                                event.getPacket().getXRot(position.getXRot()),
                                                event.getPacket().getYRot(position.getYRot()),
                                                event.getPacket().isOnGround());
                addToPositions(position, positions);
            }
        });

        ResetUtil.onRespawnOrWorldChange(this, mc, () -> playerOnLastPosition = null);
    }

    @Override
    protected void addToPositions(PlayerPosition position, Deque<PlayerPosition> positions) {
        if (positions == this.positions) {
            LocalPlayer player = mc.player;
            DamageCalculatorPlayer playerOnLastPosition = this.playerOnLastPosition;
            if (player != null && (playerOnLastPosition == null || playerOnLastPosition.getPlayer() != player)) {
                playerOnLastPosition = new DamageCalculatorPlayer(player);
                this.playerOnLastPosition = playerOnLastPosition;
            }

            if (playerOnLastPosition != null) {
                playerOnLastPosition.yRotO = playerOnLastPosition.getYRot();
                playerOnLastPosition.xRotO = playerOnLastPosition.getXRot();
                position.applyTo(playerOnLastPosition);
            }
        }

        super.addToPositions(position, positions);
    }

    public Player getPlayerOnLastPosition(Player fallback) {
        var result = this.playerOnLastPosition;
        if (result != null) {
            Vec3 pos = result.position();
            // TODO: there is still some uncertainty whether this pose is the server pose or not...
            result.setPose(fallback.getPose());
            // result.refreshDimensions(); no need to, this is done by Entity.onSyncedDataUpdated, but this might change results position, so:
            result.setPos(pos);
            return result;
        }

        return fallback;
    }

}
