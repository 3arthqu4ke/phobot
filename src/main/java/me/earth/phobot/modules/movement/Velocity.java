package me.earth.phobot.modules.movement;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.modules.client.anticheat.MovementAntiCheat;
import me.earth.phobot.util.world.PredictionUtil;
import me.earth.pingbypass.api.event.CancellingListener;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.event.CancellableEvent;
import me.earth.pingbypass.api.event.loop.LocalPlayerUpdateEvent;
import me.earth.pingbypass.api.event.network.AsyncReceiveListener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.mixins.network.s2c.IClientBoundExplodePacket;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.player.Player;

import static net.minecraft.core.Direction.DOWN;
import static net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK;

@Slf4j
public class Velocity extends PhobotModule {
    private final Setting<Position> position = constant("Grim-Position", Position.Last, "The position to send when up against Grim AC");
    private final Setting<Boolean> lag = bool("Lag", true, "Turn off for a bit when lagging.");

    public Velocity(Phobot phobot) {
        super(phobot, "Velocity", Categories.MOVEMENT, "Prevents knockback.");
        listen(new SafeListener<LocalPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(LocalPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (isLagging()) {
                    return;
                }

                if (phobot.getAntiCheat().getMovement().getValue() == MovementAntiCheat.Grim) {
                    sendInvalidationPackets(player, level);
                }
            }
        });

        listen(new AsyncReceiveListener<ClientboundSetEntityMotionPacket>(mc) {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundSetEntityMotionPacket> e, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (isLagging()) {
                    return;
                }

                if (e.getPacket().getId() == player.getId()) {
                    e.setCancelled(true);
                }
            }
        });

        listen(new SafeListener<PacketEvent.Receive<ClientboundExplodePacket>>(mc) {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundExplodePacket> event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (isLagging()) {
                    return;
                }

                IClientBoundExplodePacket explodePacket = ((IClientBoundExplodePacket) event.getPacket());
                explodePacket.setKnockbackX(0.0f);
                explodePacket.setKnockbackY(0.0f);
                explodePacket.setKnockbackZ(0.0f);
            }
        });

        listen(new CancellingListener.WithSetting<>(PushOutOfBlocks.class, bool("Blocks", true, "Prevents you from getting pushed out of blocks.")));
        listen(new CancellingListener.WithSetting<>(Velocity.EntityPush.class, bool("Entities", true, "Prevents you from getting pushed by entities.")));
    }

    private boolean isLagging() {
        return lag.getValue() && phobot.getAntiCheat().getMovement().getValue() == MovementAntiCheat.Grim && !phobot.getLagbackService().passed(250L);
    }

    private void sendInvalidationPackets(LocalPlayer p, ClientLevel level) {
        PredictionUtil.predict(level, seq -> {
            if (position.getValue() == Position.Current) { // TODO: I think Current is actually worse? Get confirmation and remove it entirely?
                p.connection.send(new ServerboundMovePlayerPacket.PosRot(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot(), p.onGround()));
            } else {
                Player l = phobot.getLocalPlayerPositionService().getPlayerOnLastPosition(p);
                p.connection.send(new ServerboundMovePlayerPacket.PosRot(l.getX(), l.getY(), l.getZ(), l.getYRot(), l.getXRot(), l.onGround()));
            }

            p.connection.send(new ServerboundPlayerActionPacket(STOP_DESTROY_BLOCK, BlockPos.containing(p.position()), DOWN, seq));
        });
    }

    public enum Position {
        Current,
        Last
    }

    public static final class PushOutOfBlocks extends CancellableEvent {}
    public static final class EntityPush extends CancellableEvent {}

}
