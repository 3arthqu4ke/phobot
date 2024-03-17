package me.earth.phobot.modules.movement;

import me.earth.phobot.Phobot;
import me.earth.phobot.event.MoveEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.movement.Movement;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.module.impl.Categories;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

public class Strafe extends PhobotModule {
    public Strafe(Phobot phobot, Speed speed) {
        super(phobot, "Strafe", Categories.MOVEMENT, "Makes you accelerate to max speed instantly in all directions.");
        listen(new SafeListener<MoveEvent>(mc, 1_000) {
            @Override
            public void onEvent(MoveEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (!speed.isEnabled() && !phobot.getPathfinder().isFollowingPath() && (player.input.forwardImpulse != 0.0f || player.input.leftImpulse != 0.0f)) {
                    Movement.State state = phobot.getMovementService().getMovement().strafe(player, level, new Movement.State(), speed.getDirectionAndYMovement(player));
                    if (!state.isReset()) {
                        player.setDeltaMovement(state.getDelta());
                        event.setVec(state.getDelta());
                    }
                }
            }
        });
    }

}
