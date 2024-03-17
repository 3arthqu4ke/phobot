package me.earth.phobot.modules.movement;

import me.earth.phobot.Phobot;
import me.earth.phobot.event.MoveEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.movement.Movement;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.loop.TickEvent;
import me.earth.pingbypass.api.event.network.ReceiveListener;
import me.earth.pingbypass.api.module.impl.Categories;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.phys.Vec3;

public class Speed extends PhobotModule {
    private Movement.State state = new Movement.State();

    public Speed(Phobot phobot) {
        super(phobot, "Speed", Categories.MOVEMENT, "Move around faster using BunnyHops.");
        listen(new SafeListener<MoveEvent>(mc) {
            @Override
            public void onEvent(MoveEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (phobot.getPathfinder().isFollowingPath()) {
                    return;
                }

                if (player.fallDistance <= 5.0 && (player.xxa != 0.0f || player.zza != 0.0f)) {
                    state = phobot.getMovementService().getMovement().move(player, mc.level, state, getDirectionAndYMovement(player));
                    if (!state.isReset()) {
                        player.setDeltaMovement(player.getDeltaMovement().x, state.getDelta().y, player.getDeltaMovement().z);
                        event.setVec(new Vec3(state.getDelta().x, state.getDelta().y, state.getDelta().z));
                    }
                } else {
                    state.reset();
                }
            }
        });

        listen(new SafeListener<TickEvent>(mc) {
            @Override
            public void onEvent(TickEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (phobot.getPathfinder().isFollowingPath()) {
                    return;
                }

                if (!mc.options.keyUp.isDown() && !mc.options.keyDown.isDown() && !mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
                    player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
                }

                state.setDistance(Math.sqrt((player.getX() - player.xo) * (player.getX() - player.xo) + (player.getZ() - player.zo) * (player.getZ() - player.zo)));
            }
        });

        listen(new ReceiveListener.Direct<ClientboundPlayerPositionPacket>() {
            @Override
            public void onPacket(ClientboundPlayerPositionPacket packet) {
                mc.submit(state::reset);
            }
        });
    }

    @Override
    protected void onEnable() {
        this.state.reset();
        LocalPlayer player = mc.player;
        if (player != null) {
            state.setDistance(Math.sqrt((player.getX() - player.xo) * (player.getX() - player.xo) + (player.getZ() - player.zo) * (player.getZ() - player.zo)));
        }
    }

    public Vec3 getDirectionAndYMovement(LocalPlayer player) {
        double forward = player.input.forwardImpulse;
        double strafe = player.input.leftImpulse;
        float yaw = player.yRotO + (player.getYRot() - player.yRotO) * mc.getFrameTime();
        if (forward == 0.0 && strafe == 0.0) {
            return new Vec3(0.0, player.getDeltaMovement().y, 0.0);
        } else {
            if (forward != 0.0) {
                if (strafe > 0.0) {
                    yaw += ((forward > 0.0) ? -45 : 45);
                } else if (strafe < 0.0) {
                    yaw += ((forward > 0.0) ? 45 : -45);
                }

                strafe = 0.0;
                if (forward > 0.0) {
                    forward = 1.0;
                } else if (forward < 0.0) {
                    forward = -1.0;
                }
            }

            return new Vec3(forward * -Math.sin(Math.toRadians(yaw)) + strafe * Math.cos(Math.toRadians(yaw)),
                    // TODO: this special handling of y movement is so weird and causes so many weird cases like in MovementPathfindingAlgorithm or the Pathfinder?!
                    player.getDeltaMovement().y,
                    forward * Math.cos(Math.toRadians(yaw)) - strafe * -Math.sin(Math.toRadians(yaw)));
        }
    }

}
