package me.earth.phobot.movement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.earth.phobot.util.math.MathUtil;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.phobot.util.world.BlockStateLevel;
import me.earth.phobot.util.world.DelegatingClientLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Computes the parable we make if we jump off somewhere with a certain {@link Movement}, e.g. a {@link BunnyHop} one.
 */
@Getter
@ToString
@EqualsAndHashCode
public class MovementParable {
    private final double[] horizontalValues;
    private final double[] verticalValues;
    private final double maxHorizontal;
    private final double maxVertical;
    private final int size;

    public MovementParable(double[] horizontalValues, double[] verticalValues) {
        if (horizontalValues.length != verticalValues.length) {
            throw new IllegalArgumentException("horizontalValues.length (" + horizontalValues.length + ") != verticalValues.length (" + verticalValues.length + ")");
        }

        this.horizontalValues = horizontalValues;
        this.verticalValues = verticalValues;
        this.maxHorizontal = horizontalValues[horizontalValues.length - 1];
        this.maxVertical = Arrays.stream(verticalValues).max().orElse(0.0);
        this.size = horizontalValues.length;
    }

    public boolean canReach(Position position, double targetX, double targetY, double targetZ) {
        return canReach(position.x(), position.y(), position.z(), targetX, targetY, targetZ);
    }

    public boolean canReach(double fromX, double fromY, double fromZ, double targetX, double targetY, double targetZ) {
        double distance = Math.sqrt(MathUtil.distance2dSq(fromX, fromZ, targetX, targetZ));
        if (distance > maxHorizontal || targetY > fromY && targetY - fromY > maxVertical) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            double y = verticalValues[i] + fromY;
            double x = horizontalValues[i];
            if (y >= targetY && x >= distance) { // TODO: handling for nodes that are very close? Could be possible that we cannot jump up that closely?
                return true;
            }
        }

        return false;
    }

    public boolean isOnParable(double fromX, double fromY, double fromZ, double targetX, double targetY, double targetZ, double hLeniency, double vLeniency) {
        double distance = Math.sqrt(MathUtil.distance2dSq(fromX, fromZ, targetX, targetZ));
        if (distance > maxHorizontal || targetY > fromY && targetY - fromY > maxVertical) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            double y = verticalValues[i] + fromY;
            double x = horizontalValues[i];
            if (y >= targetY - vLeniency
                // if i == size - 1, we have reached the part of the parable where it just goes straight down and we can hit any targetY below y
                && (y <= targetY + vLeniency || i == size - 1)
                && x >= distance - hLeniency
                && x <= distance + hLeniency) {
                return true;
            }
        }

        return false;
    }

    public static MovementParable calculate(MovementPlayer potionAndMovementPlayer, ClientLevel level) {
        return calculate(potionAndMovementPlayer, null, level);
    }

    public static MovementParable calculate(MovementPlayer potionAndMovementPlayer, @Nullable Movement.State initialState, ClientLevel level) {
        List<Double> horizontalValues = new ArrayList<>();
        List<Double> verticalValues = new ArrayList<>();
        MovementPlayer player = getMovementPlayer(potionAndMovementPlayer.getMovement(), potionAndMovementPlayer, initialState, level);
        double maxY = 0.0;
        for (int i = 0; i < 256; i++) {
            player.travel();
            horizontalValues.add(player.getX());
            verticalValues.add(player.getY());
            if (player.getY() > maxY) {
                maxY = player.getY();
            }

            if (player.fallDistance > 5.0) {
                break;
            }
        }

        return new MovementParable(horizontalValues.stream().mapToDouble(d -> d).toArray(), verticalValues.stream().mapToDouble(d -> d).toArray());
    }

    private static MovementPlayer getMovementPlayer(Movement movement, MovementPlayer potionPlayer, @Nullable Movement.State initialState, ClientLevel level) {
        DelegatingClientLevel emptyLevel = getDelegatingClientLevel(level);

        Movement.State[] state = new Movement.State[]{ initialState == null ? new Movement.State() : initialState };
        MovementPlayer player = new MovementPlayer(emptyLevel);
        potionPlayer.getActiveEffects().forEach(player::addEffect);
        player.setPos(Vec3.ZERO);
        player.setMovement(movement);
        // ensure we jump on the first update
        player.verticalCollisionBelow = true;
        player.verticalCollision = true;
        player.setOnGround(true);
        if (initialState == null) {
            state[0].stage = 2;
        }

        player.setMoveCallback(delta -> {
            state[0] = movement.move(player, emptyLevel, state[0], new Vec3(1.0, player.getDeltaMovement().y, 0.0));
            if (state[0].isReset()) {
                return delta;
            }

            player.setDeltaMovement(player.getDeltaMovement().x, state[0].getDelta().y, player.getDeltaMovement().z);
            return new Vec3(state[0].getDelta().x, state[0].getDelta().y, state[0].getDelta().z);
        });

        return player;
    }

    private static DelegatingClientLevel getDelegatingClientLevel(ClientLevel level) {
        BlockStateLevel forCollisions = new BlockStateLevel.Empty() {
            @Override
            public int getHeight() {
                return level.getHeight();
            }
        };

        return new BlockStateLevel.Delegating(level) {
            @Override
            public @NotNull BlockStateLevelImpl getImpl(ClientLevel level) {
                return new BlockStateLevelImpl(level) {
                    @Override
                    public @NotNull BlockState getBlockState(BlockPos pos) {
                        return Blocks.AIR.defaultBlockState();
                    }

                    @Override
                    public BlockGetter getChunkForCollisions(int x, int z) {
                        return forCollisions;
                    }

                    @Override
                    public BlockGetter getChunkForCollisions() {
                        return forCollisions;
                    }
                };
            }

            @Override
            public boolean hasChunkAt(BlockPos blockPos) {
                return true;
            }
        };
    }

}
