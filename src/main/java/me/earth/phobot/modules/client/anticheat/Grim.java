package me.earth.phobot.modules.client.anticheat;

import me.earth.phobot.util.math.PositionUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public enum Grim implements StrictDirection {
    INSTANCE;

    private static final double MIN_EYE_HEIGHT = 0.4;
    private static final double MAX_EYE_HEIGHT = 1.62;
    private static final double MOVEMENT_THRESHOLD = 0.0002;

    @Override
    public @Nullable Direction getStrictDirection(BlockPos pos, Player player, ClientLevel level) {
        for (Direction direction : PositionUtil.DIRECTIONS) {
            if (strictDirectionCheck(pos, direction, level, player)) {
                return direction;
            }
        }

        return null;
    }

    @Override
    public boolean strictDirectionCheck(BlockPos pos, Direction direction, ClientLevel level, Player player) {
        // see ac.grim.grimac.checks.impl.scaffolding.PositionPlace
        // TODO: skip if we are placing Blocks.SCAFFOLDING
        AABB combined = getCombinedBox(pos, level);
        AABB eyePositions = new AABB(player.getX(), player.getY() + MIN_EYE_HEIGHT, player.getZ(), player.getX(), player.getY() + MAX_EYE_HEIGHT, player.getZ()).inflate(MOVEMENT_THRESHOLD);
        if (isIntersected(eyePositions, combined)) {
            return true;
        }

        return !switch (direction) {
            case NORTH ->
                    eyePositions.minZ > combined.minZ;
            case SOUTH ->
                    eyePositions.maxZ < combined.maxZ;
            case EAST ->
                    eyePositions.maxX < combined.maxX;
            case WEST ->
                    eyePositions.minX > combined.minX;
            case UP ->
                    eyePositions.maxY < combined.maxY;
            case DOWN ->
                    eyePositions.minY > combined.minY;
        };
    }

    private AABB getCombinedBox(BlockPos pos, Level level) {
        // TODO: Grim HitboxData, not needed for CrystalPvP.cc FFA, but later
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ());
        AABB combined = new AABB(pos);
        for (AABB box : shape.toAabbs()) {
            double minX = Math.max(box.minX, combined.minX);
            double minY = Math.max(box.minY, combined.minY);
            double minZ = Math.max(box.minZ, combined.minZ);
            double maxX = Math.min(box.maxX, combined.maxX);
            double maxY = Math.min(box.maxY, combined.maxY);
            double maxZ = Math.min(box.maxZ, combined.maxZ);
            combined = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }

        // TODO: weirdBoxes, buggyBoxes
        return combined;
    }

    private boolean isIntersected(AABB bb, AABB other) {
        return other.maxX - Shapes.EPSILON > bb.minX
                && other.minX + Shapes.EPSILON < bb.maxX
                && other.maxY - Shapes.EPSILON > bb.minY
                && other.minY + Shapes.EPSILON < bb.maxY
                && other.maxZ - Shapes.EPSILON > bb.minZ
                && other.minZ + Shapes.EPSILON < bb.maxZ;
    }

}
