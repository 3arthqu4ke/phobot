package me.earth.phobot.modules.movement;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.event.MoveEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Slf4j
public class FastFall extends PhobotModule {
    private final Setting<Double> speed = precise("Speed", 3.1, 0.1, 10.0, "The speed you are going to fall with.");

    public FastFall(Phobot phobot, Speed speed) {
        super(phobot, "FastFall", Categories.MOVEMENT, "Makes you fall fast.");
        listen(new SafeListener<MoveEvent>(mc, -1000) {
            @Override
            public void onEvent(MoveEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (!phobot.getPathfinder().isFollowingPath()
                        && !mc.options.keyJump.isDown()
                        && !mc.options.keyShift.isDown()
                        && canFastFall(player, event.getVec(), level, speed.isEnabled())) {
                    event.setVec(getFastFallVec(event.getVec()));
                }
            }
        });
    }

    public Vec3 getFastFallVec(Vec3 delta) {
        // cancel movement?
        return new Vec3(delta.x() * 0.01, Math.min(-speed.getValue(), delta.y ), delta.z() * 0.01);
    }

    public boolean canFastFall(Player player, Vec3 delta, ClientLevel level, boolean speedIsOn) {
        double nearestBlockBelow;
        // TODO: phobot.getLagbackService?
        return !speedIsOn
                && !phobot.getMovementService().getMovement().shouldNotUseMovementHacks(player)
                && player.fallDistance < 0.5
                && player.getDeltaMovement().y < 0
                && player.onGround()
                && !isOnGroundAfterMove(player, delta, level)
                && player.getY() - (nearestBlockBelow = getNearestBlockBelow(player, level)) > 0.625
                && player.getY() - nearestBlockBelow <= speed.getValue();
    }

    private boolean isOnGroundAfterMove(Player player, Vec3 delta, ClientLevel level) {
        MovementPlayer movementPlayer = new MovementPlayer(level);
        movementPlayer.copyPosition(player);
        movementPlayer.setDeltaMovement(delta);
        Vec3 collideVec = movementPlayer.collide(delta = movementPlayer.maybeBackOffFromEdge(movementPlayer.getDeltaMovement(), MoverType.SELF));
        return delta.y != collideVec.y && delta.y < 0.0;
    }

    private double getNearestBlockBelow(Player player, Level level) {
        double playerY = player.getY();
        double speed = this.speed.getValue();
        for (double y = playerY; y > playerY - speed; y -= 0.001) {
            BlockPos pos = BlockPos.containing(player.getX(), y, player.getZ());
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty()) {
                if (state.getBlock() instanceof SlabBlock) {
                    return -1;
                }

                return y;
            }
        }

        return -1;
    }

}
