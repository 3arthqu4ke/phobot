package me.earth.phobot.pathfinder.blocks;

import me.earth.phobot.modules.ChecksBlockPlacingValidity;
import me.earth.phobot.modules.client.anticheat.StrictDirection;
import me.earth.phobot.pathfinder.util.Cancellation;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Finds the smallest amount of helping blocks that we need to place in a position.
 */
public class BlockPathfinder {
    private final BlockNode center;

    public BlockPathfinder() {
        BlockNodePoolBuilder builder = new BlockNodePoolBuilder();
        builder.build(10/* this is not radius but manhattan radius basically */, BlockNode::new);
        this.center = Objects.requireNonNull(builder.getCenter(), "PoolBuilder center was null!");
    }

    public @NotNull List<BlockPos> getShortestPath(BlockPos target, Block block, Player player, ClientLevel level, int maxCost, ChecksBlockPlacingValidity module, BlockPlacer blockPlacer) {
        BiPredicate<BlockPos, BlockPos> validityCheck = getDefaultValidityCheck(block, player, level, module, blockPlacer);
        BiPredicate<BlockPos, @Nullable BlockPos> goalCheck = getDefaultGoalCheck(player, block, level, module);
        return getShortestPath(target, maxCost, validityCheck, goalCheck);
    }

    public @NotNull List<BlockPos> getShortestPath(BlockPos target, int maxCost, BiPredicate<BlockPos, BlockPos> validityCheck, BiPredicate<BlockPos, @Nullable BlockPos> goalCheck) {
        center.setToOffsetFromCenter(target);
        BlockPathfinderAlgorithm firstSearchNoHeuristic = new BlockPathfinderAlgorithm(center, validityCheck, goalCheck, target, maxCost);
        var result = firstSearchNoHeuristic.run(Cancellation.UNCANCELLABLE);
        if (result != null) {
            return result.getPath().stream().map(BlockNode::getCurrent).map(BlockPos::immutable).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public  BiPredicate<BlockPos, @Nullable BlockPos> getDefaultGoalCheck(Player player, Block block, ClientLevel level, ChecksBlockPlacingValidity module) {
        BlockStateLevel.Delegating blockStateLevel = new BlockStateLevel.Delegating(level);
        return (testPos, from) -> {
            if (module.isOutsideRange(testPos, player) || from != null && !strictDirectionCheck(from, testPos, block, module, player, blockStateLevel)) {
                return false;
            }

            return module.isValidPlacePos(testPos, level, player) || module.hasActionCreatingPlacePos(testPos, null, player, level, null);
        };
    }

    public BiPredicate<BlockPos, BlockPos> getDefaultValidityCheck(Block block, Player player, ClientLevel level, ChecksBlockPlacingValidity module, BlockPlacer blockPlacer) {
        BlockStateLevel.Delegating blockStateLevel = new BlockStateLevel.Delegating(level);
        return (currentPos, testPos) -> {
            if (!level.getWorldBorder().isWithinBounds(testPos) || module.isOutsideRange(testPos, player)) {
                return false;
            }

            VoxelShape shape = block.defaultBlockState().getCollisionShape(level, testPos, blockPlacer.getCollisionContext());
            if (shape.isEmpty() || !module.isBlockedByEntity(testPos, shape, player, level, entity -> false, ((placer, endCrystal) -> {/*do not set crystal*/}))) {
                return strictDirectionCheck(currentPos, testPos, block, module, player, blockStateLevel);
            }

            return false;
        };
    }

    private boolean strictDirectionCheck(BlockPos currentPos, BlockPos testPos, Block block, ChecksBlockPlacingValidity module, Player player, BlockStateLevel.Delegating level) {
        if (module.getBlockPlacer().getAntiCheat().getStrictDirection().getValue() != StrictDirection.Type.Vanilla) {
            Direction direction = getDirectionBetweenAdjacent(currentPos, testPos);
            level.getMap().clear();
            BlockState state = level.getLevel().getBlockState(testPos);
            if (state.isAir()/* TODO: || isFluid || isGoingToBeReplaced by us?!?!?!*/) {
                state = block.defaultBlockState();
            }

            level.getMap().put(testPos, state); // do the other blocks we placed for the path also play a role during the check?
            return module.getBlockPlacer().getAntiCheat().getStrictDirectionCheck().strictDirectionCheck(testPos, direction, level, player);
        }

        return true;
    }

    private Direction getDirectionBetweenAdjacent(BlockPos pos, BlockPos placeOn) {
        if (pos.getY() > placeOn.getY()) {
            return Direction.UP;
        } else if (pos.getY() < placeOn.getY()) {
            return Direction.DOWN;
        } else if (pos.getX() > placeOn.getX()) {
            return Direction.EAST;
        } else if (pos.getX() < placeOn.getX()) {
            return Direction.WEST;
        } else if (pos.getZ() > placeOn.getZ()) {
            return Direction.SOUTH;
        }

        return Direction.NORTH;
    }

}
