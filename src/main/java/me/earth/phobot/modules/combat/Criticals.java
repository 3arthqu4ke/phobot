package me.earth.phobot.modules.combat;

import me.earth.phobot.ducks.IServerboundInteractPacket;
import me.earth.phobot.mixins.entity.ILocalPlayer;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.module.impl.ModuleImpl;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.commons.event.SafeListener;
import me.earth.pingbypass.commons.event.network.PacketEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

// TODO: fix the onGround packets being weird with Speedmine?!
public class Criticals extends ModuleImpl {
    private final Setting<Mode> twoPackets = constant("Mode", Mode.Many, "How many packets to use.");
    private final StopWatch.ForSingleThread timer = new StopWatch.ForSingleThread();

    public Criticals(PingBypass pingBypass) {
        super(pingBypass, "Criticals", Categories.COMBAT, "Always deals criticals when attacking entities.");
        listen(new SafeListener<PacketEvent.Send<ServerboundInteractPacket>>(mc) {
            @Override
            public void onEvent(PacketEvent.Send<ServerboundInteractPacket> event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (((IServerboundInteractPacket) event.getPacket()).getPhobot$entity() instanceof LivingEntity
                        && ((IServerboundInteractPacket) event.getPacket()).isPhobot$attack()
                        && mc.isSameThread()
                        && player.onGround()
                        && !player.isSpectator()
                        && !player.hasEffect(MobEffects.BLINDNESS)
                        && !player.isPassenger()
                        && !player.isInWater()
                        && !player.onClimbable()
                        && timer.passed(400L)
                        /*&& !player.isInLava() seems like this does not get checked */) {
                    if (twoPackets.getValue() == Mode.Few) {
                        // this makes Speedmine take longer!!!
                        player.connection.send(new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY() + 0.0625, player.getZ(), false));
                        player.connection.send(new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY(), player.getZ(), false));
                    } else {
                        player.connection.send(new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY() + 0.05, player.getZ(), false));
                        player.connection.send(new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY(), player.getZ(), false));
                        player.connection.send(new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY() + 0.03, player.getZ(), false));
                        player.connection.send(new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY(), player.getZ(), false));
                    }
                    // TODO: service that listens to packets send and then fixes lastX, Y, Z, YRot, XRot and onGround?
                    ((ILocalPlayer) player).setLastOnGround(false);
                    timer.reset();
                }
            }
        });
    }

    public enum Mode {
        Few,
        Many
    }

}
