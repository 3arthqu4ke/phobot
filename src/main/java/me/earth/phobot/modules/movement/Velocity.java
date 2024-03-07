package me.earth.phobot.modules.movement;

import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.modules.client.anticheat.MovementAntiCheat;
import me.earth.phobot.util.ResetUtil;
import me.earth.pingbypass.api.event.CancellingListener;
import me.earth.pingbypass.api.event.event.CancellableEvent;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
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
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

public class Velocity extends PhobotModule {
    private final Setting<Boolean> lag = bool("Lag", true, "Turn off for a bit when lagging.");
    private boolean grimCancelled;

    public Velocity(Phobot phobot) {
        super(phobot, "Velocity", Categories.MOVEMENT, "Prevents knockback.");
        listen(new Listener<LocalPlayerUpdateEvent>() {
            @Override
            public void onEvent(LocalPlayerUpdateEvent event) {
                if (isLagging()) {
                    return;
                }

                if (phobot.getAntiCheat().getMovement().getValue() == MovementAntiCheat.Grim) {
                    if (grimCancelled) {
                        LocalPlayer player = event.getPlayer();
                        player.connection.send(
                            new ServerboundMovePlayerPacket.PosRot(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.onGround()));
                        player.connection.send(
                            new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, BlockPos.containing(player.position()), Direction.DOWN));

                        grimCancelled = false;
                    }
                } else {
                    grimCancelled = false;
                }
            }
        });

        listen(new AsyncReceiveListener<ClientboundSetEntityMotionPacket>(mc) {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundSetEntityMotionPacket> e, LocalPlayer p, ClientLevel ignore, MultiPlayerGameMode ignored) {
                if (isLagging()) {
                    return;
                }

                if (e.getPacket().getId() == p.getId()) {
                    e.setCancelled(true);
                    if (phobot.getAntiCheat().getMovement().getValue() == MovementAntiCheat.Grim) {
                        grimCancelled = true;
                    }
                }
            }
        });

        listen(new Listener<PacketEvent.Receive<ClientboundExplodePacket>>() {
            @Override
            public void onEvent(PacketEvent.Receive<ClientboundExplodePacket> event) {
                if (isLagging()) {
                    return;
                }

                IClientBoundExplodePacket explodePacket = ((IClientBoundExplodePacket) event.getPacket());
                explodePacket.setKnockbackX(0.0f);
                explodePacket.setKnockbackY(0.0f);
                explodePacket.setKnockbackZ(0.0f);
                if (phobot.getAntiCheat().getMovement().getValue() == MovementAntiCheat.Grim) {
                    grimCancelled = true;
                }
            }
        });

        listen(new CancellingListener.WithSetting<>(PushOutOfBlocks.class, bool("Blocks", true, "Prevents you from getting pushed out of blocks.")));
        listen(new CancellingListener.WithSetting<>(Velocity.EntityPush.class, bool("Entities", true, "Prevents you from getting pushed by entities.")));
        ResetUtil.onRespawnOrWorldChange(this, mc , () -> grimCancelled = false);
    }

    private boolean isLagging() {
        return lag.getValue() && phobot.getAntiCheat().getMovement().getValue() == MovementAntiCheat.Grim && !phobot.getLagbackService().passed(250L);
    }

    public static final class PushOutOfBlocks extends CancellableEvent {}
    public static final class EntityPush extends CancellableEvent {}

}
