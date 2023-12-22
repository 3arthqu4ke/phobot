package me.earth.phobot.modules.client.anticheat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public enum NCP implements StrictDirection {
    INSTANCE;

    @Override
    public @Nullable Direction getStrictDirection(BlockPos pos, Player player, ClientLevel level) {
        BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        if (checkInside(pos, eyePos)) {
            return Direction.DOWN;
        } else {
            List<Direction> interactableDirections = getInteractableDirections(pos, eyePos, true);
            for (Direction direction : interactableDirections) {
                if (isDirectionBlocked(pos, level, interactableDirections, direction, true)) {
                    continue;
                }

                return direction;
            }
        }

        return null;
    }

    @Override
    public boolean strictDirectionCheck(BlockPos pos, Direction direction, ClientLevel level, Player player) {
        // see fr.neatmonster.nocheatplus.checks.generic.pos.AbstractBlockDirectionCheck.isInteractable
        BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        if (checkInside(pos, eyePos)) {
            return true;
        }

        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        boolean fullBounds = shape == Shapes.block();
        List<Direction> interactableDirections = getInteractableDirections(pos, eyePos, fullBounds);
        if (!interactableDirections.contains(direction)) {
            return false;
        }

        return !isDirectionBlocked(pos, level, interactableDirections, direction, fullBounds);
    }

    private boolean checkInside(BlockPos pos, BlockPos eyePos) {
        return eyePos.getX() == pos.getX() && eyePos.getY() == pos.getY() && eyePos.getZ() == pos.getZ();
    }

    private List<Direction> getInteractableDirections(BlockPos pos, BlockPos eyePos, boolean fullBounds) {
        int locX = eyePos.getX();
        int locY = eyePos.getY();
        int locZ = eyePos.getZ();
        int blockX = pos.getX();
        int blockY = pos.getY();
        int blockZ = pos.getZ();
        return getDirections(locX - blockX, locY - blockY, locZ - blockZ, fullBounds);
    }

    private List<Direction> getDirections(int xDiff, int yDiff, int zDiff, boolean fullBounds) {
        List<Direction> faces = new ArrayList<>(6);
        if (!fullBounds) {
            if (xDiff == 0) {
                faces.add(Direction.EAST);
                faces.add(Direction.WEST);
            }

            if (zDiff == 0) {
                faces.add(Direction.SOUTH);
                faces.add(Direction.NORTH);
            }
        }

        if (yDiff == 0) {
            faces.add(Direction.UP);
            faces.add(Direction.DOWN);
        } else {
            faces.add(yDiff > 0 ? Direction.UP : Direction.DOWN);
        }

        if (xDiff != 0) {
            faces.add(xDiff > 0 ? Direction.EAST : Direction.WEST);
        }

        if (zDiff != 0) {
            faces.add(zDiff > 0 ? Direction.SOUTH : Direction.NORTH);
        }

        return faces;
    }

    private boolean isDirectionBlocked(BlockPos pos, ClientLevel level, List<Direction> interactable, Direction direction, boolean hasFullBounds) {
        if (hasFullBounds) {
            BlockPos relative = pos.relative(direction);
            BlockState state = level.getBlockState(relative);
            // TODO: actual passable!
            return state.getShape(level, pos) == Shapes.block() && !state.getCollisionShape(level, relative).isEmpty(); // fullBounds && !isPassable
        } else {
            for (Direction dir : interactable) {
                BlockPos relative = pos.relative(dir);
                BlockState state = level.getBlockState(relative);
                boolean fullBounds = state.getShape(level, pos) == Shapes.block();
                if (!fullBounds || !state.getCollisionShape(level, relative).isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

}
