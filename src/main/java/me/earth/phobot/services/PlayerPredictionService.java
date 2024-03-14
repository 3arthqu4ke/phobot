package me.earth.phobot.services;

import me.earth.phobot.ducks.IAbstractClientPlayer;
import me.earth.phobot.event.LerpToEvent;
import me.earth.phobot.modules.client.anticheat.AntiCheat;
import me.earth.phobot.movement.Movement;
import me.earth.phobot.util.player.PredictionPlayer;
import me.earth.phobot.util.time.TimeUtil;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.loop.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class PlayerPredictionService extends SubscriberImpl {
    public static final double DISTANCE_SQUARED = Mth.square(6.0 + 12.0 + 2.0); // range + crystal range + lenience

    public PlayerPredictionService(AntiCheat antiCheat, Minecraft mc, MovementService movementService) {
        listen(new SafeListener<TickEvent>(mc) {
            @Override
            public void onEvent(TickEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                for (AbstractClientPlayer clientPlayer : level.players()) {
                    if (clientPlayer != player && clientPlayer instanceof IAbstractClientPlayer access) {
                        for (PredictionPlayer pp : access.phobot$getPredictions()) {
                            if (pp != null && TimeUtil.isTimeStampOlderThan(pp.getLastPrediction(), 150L)) {
                                pp.setDeltaMovement(Vec3.ZERO);
                                pp.setPos(access.getLerpX(), access.getLerpY(), access.getLerpZ());
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
        });

        listen(new SafeListener<LerpToEvent>(mc) {
            @Override
            public void onEvent(LerpToEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (event.getEntity() instanceof AbstractClientPlayer absClientPlayer
                        && event.getEntity() instanceof IAbstractClientPlayer access
                        && !(event.getEntity() instanceof LocalPlayer)
                        && event.getEntity().distanceToSqr(player) <= DISTANCE_SQUARED) {
                    double xBefore = access.getLerpX();
                    double yBefore = access.getLerpY();
                    double zBefore = access.getLerpZ();
                    Vec3 move = new Vec3(event.getX() - xBefore, event.getY() - yBefore, event.getZ() - zBefore).scale(1.0 / antiCheat.getUpdates().getValue());
                    PredictionPlayer previous = null;
                    PredictionPlayer[] predictionPlayers = access.phobot$getPredictions();
                    boolean reset = Double.compare(move.lengthSqr(), 0.0) == 0 || move.lengthSqr() > Mth.square(antiCheat.getLagbackThreshold().getValue() * antiCheat.getUpdates().getValue());
                    boolean resetPos = TimeUtil.isTimeStampOlderThan(event.getLastLerp(), 100L);
                    Movement physics = movementService.getMovement();
                    for (int i = 0; i < predictionPlayers.length; i++) {
                        PredictionPlayer pp = predictionPlayers[i];
                        if (pp == null) {
                            pp = predictionPlayers[i] = new PredictionPlayer(absClientPlayer);
                        }

                        if (reset) {
                            if (resetPos) {
                                pp.setPos(event.getX(), event.getY(), event.getZ());
                            } else {
                                return;
                            }
                        } else {
                            pp.setLastPrediction(TimeUtil.getMillis());
                            if (previous == null) {
                                pp.setPos(event.getX(), event.getY(), event.getZ());
                                pp.setDeltaMovement(move);
                                pp.setOnGround(true);
                            } else {
                                pp.copyPosition(previous);
                                pp.setDeltaMovement(previous.getDeltaMovement());
                                pp.setOnGround(previous.onGround());
                            }

                            // TODO: MovementPlayer.travel might be more accurate?
                            pp.setDeltaMovement(move.x, pp.onGround() ? -physics.getGravity() * physics.getDrag() : (pp.getDeltaMovement().y - physics.getGravity()) * physics.getDrag(), move.z);
                            pp.move(MoverType.SELF, pp.getDeltaMovement());
                        }

                        previous = pp;
                    }
                }
            }
        });
    }

}
