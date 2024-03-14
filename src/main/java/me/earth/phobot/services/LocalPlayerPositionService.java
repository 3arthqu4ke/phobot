package me.earth.phobot.services;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.event.ChangeWorldEvent;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.mixins.entity.ILocalPlayer;
import me.earth.phobot.util.ResetUtil;
import me.earth.phobot.util.player.DamageCalculatorPlayer;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.api.setting.impl.types.BoolBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@Getter
@SuppressWarnings("NonAtomicOperationOnVolatileField") // is volatile only for the reads
public class LocalPlayerPositionService extends PlayerPositionService {
    private static final int THRESHOLD = 400;
    private final Deque<PlayerPosition> tickPositions = new ArrayDeque<>(THRESHOLD);
    private final Minecraft mc;
    /**
     * When we call {@link MultiPlayerGameMode#useItem(Player, InteractionHand)} a {@link ServerboundMovePlayerPacket.PosRot} will be sent.
     * This packet will not update the latest position/rotation of the player, so if we were spoofing the position/rotation before, and we spoof to the same value afterward,
     * our server position will not be updated, but on the server we will have the position/rotation from when {@code useItem} was called.
     */
    private final Setting<Boolean> fixLast = new BoolBuilder()
            .withName("FixPosition")
            .withValue(true)
            .withDescription("Fixes the last position/rotation sent to the server e.g. when an item is used.")
            .build();

    @Getter(AccessLevel.NONE)
    private DamageCalculatorPlayer playerOnLastPosition = null;

    public LocalPlayerPositionService(Minecraft mc) {
        super(THRESHOLD);
        this.mc = mc;
        listen(new Listener<PostMotionPlayerUpdateEvent>(Integer.MAX_VALUE) {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event) {
                LocalPlayerPositionService.super.addToPositions(position, tickPositions);
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
                fixLastPosition(position, false, false);
            }
        });

        listen(new Listener<PacketEvent.PostSend<ServerboundMovePlayerPacket.Rot>>(Integer.MAX_VALUE) {
            @Override
            public void onEvent(PacketEvent.PostSend<ServerboundMovePlayerPacket.Rot> event) {
                position = new PlayerPosition(
                        position,
                        event.getPacket().getXRot(position.getXRot()),
                        event.getPacket().getYRot(position.getYRot()),
                        event.getPacket().isOnGround()
                );

                addToPositions(position, positions);
                fixLastPosition(position, false, true);
            }
        });

        listen(new Listener<PacketEvent.PostSend<ServerboundMovePlayerPacket.Pos>>(Integer.MAX_VALUE) {
            @Override
            public void onEvent(PacketEvent.PostSend<ServerboundMovePlayerPacket.Pos> event) {
                position = new PlayerPosition(
                        position,
                        event.getPacket().getX(position.getX()),
                        event.getPacket().getY(position.getY()),
                        event.getPacket().getZ(position.getZ()),
                        event.getPacket().isOnGround()
                );
                addToPositions(position, positions);
                fixLastPosition(position, true, false);
            }
        });

        listen(new Listener<PacketEvent.PostSend<ServerboundMovePlayerPacket.PosRot>>(Integer.MAX_VALUE) {
            @Override
            public void onEvent(PacketEvent.PostSend<ServerboundMovePlayerPacket.PosRot> event) {
                position = new PlayerPosition(
                        event.getPacket().getX(position.getX()),
                        event.getPacket().getY(position.getY()),
                        event.getPacket().getZ(position.getZ()),
                        event.getPacket().getXRot(position.getXRot()),
                        event.getPacket().getYRot(position.getYRot()),
                        event.getPacket().isOnGround()
                );
                addToPositions(position, positions);
                fixLastPosition(position, true, true);
            }
        });

        ResetUtil.onRespawnOrWorldChange(this, mc, () -> playerOnLastPosition = null);
    }

    @Override
    protected synchronized void addToPositions(PlayerPosition position, Deque<PlayerPosition> positions) {
        if (positions == this.positions) {
            LocalPlayer player = mc.player;
            DamageCalculatorPlayer playerOnLastPosition = this.playerOnLastPosition;
            if (player != null) {
                // TODO: if (player.isPassenger()) -> we send y == -999, fix!!!

                if (playerOnLastPosition == null || playerOnLastPosition.getPlayer() != player) {
                    playerOnLastPosition = new DamageCalculatorPlayer(player);
                    this.playerOnLastPosition = playerOnLastPosition;
                }
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
            if (fallback != null) {
                result.setPose(fallback.getPose());
            }

            // result.refreshDimensions(); no need to, this is done by Entity.onSyncedDataUpdated, but this might change results position, so:
            result.setPos(pos);
            return result;
        }

        return fallback;
    }

    private synchronized void fixLastPosition(PlayerPosition position, boolean hasPosition, boolean hasRotation) {
        ILocalPlayer player;
        if (fixLast.getValue() && (player = (ILocalPlayer) mc.player) != null) {
            if (hasPosition && !((LocalPlayer) player).isPassenger()) {
                player.setXLast(position.getX());
                player.setYLast1(position.getY());
                player.setZLast(position.getZ());
                player.setPositionReminder(0);
            }

            if (hasRotation) {
                player.setYRotLast(position.getYRot());
                player.setXRotLast(position.getXRot());
            }

            player.setLastOnGround(position.isOnGround());
        }
    }

}
