package me.earth.phobot.modules;


import me.earth.phobot.modules.client.anticheat.StrictDirection;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.util.entity.EntityUtil;
import me.earth.phobot.util.math.PositionUtil;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface ChecksBlockPlacingValidity {
    BlockPlacer getBlockPlacer();

    default boolean isBlockedByEntity(BlockPos pos, VoxelShape shapeToPlace, Player player, ClientLevel level, Predicate<Entity> consumesBlockingEntities) {
        return isBlockedByEntity(pos, shapeToPlace, player, level, consumesBlockingEntities, BlockPlacer::setCrystal);
    }

    default boolean isBlockedByEntity(BlockPos pos, VoxelShape shapeToPlace, Player player, ClientLevel level, Predicate<Entity> consumesBlockingEntities,
                                      BiConsumer<BlockPlacer, EndCrystal> setCrystal) {
        return isBlockedByEntity(pos, shapeToPlace, player, level, consumesBlockingEntities, setCrystal, true);
    }

    default boolean isBlockedByEntity(BlockPos pos, VoxelShape shapeToPlace, Player player, ClientLevel level, Predicate<Entity> consumesBlockingEntities,
                                      BiConsumer<BlockPlacer, EndCrystal> setCrystal, boolean breakCrystals) {
        VoxelShape moved = shapeToPlace.move(pos.getX(), pos.getY(), pos.getZ());
        boolean blockedByCrystal = false;
        for (Entity entity : level.getEntities(null, moved.bounds())) {
            if (entity.isRemoved()
                    || !entity.blocksBuilding
                    || !Shapes.joinIsNotEmpty(moved, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)
                    || consumesBlockingEntities.test(entity)) {
                continue;
            }

            if (breakCrystals && entity instanceof EndCrystal) {
                blockedByCrystal = true;
            } else {
                return true;
            }
        }

        if (blockedByCrystal && getBlockPlacer().getCrystal() == null) {
            float lowestDamage = Float.MAX_VALUE;
            EndCrystal bestCrystal = null;
            for (EndCrystal crystal : EntityUtil.getCrystalsInRange(player, level)) {
                float damage = getBlockPlacer().getDamageCalculator().getDamage(player, level, crystal);
                if (isDamageAcceptable(player, damage) && damage < lowestDamage) {
                    lowestDamage = damage;
                    bestCrystal = crystal;
                }
            }

            if (bestCrystal == null) {
                return true;
            }

            setCrystal.accept(getBlockPlacer(), bestCrystal);
        }

        return false;
    }

    default boolean isDamageAcceptable(Player player, float damage) {
        return damage < EntityUtil.getHealth(player);
    }

    default @Nullable Direction getDirection(ClientLevel level, Player player, BlockPos pos, Set<BlockPlacer.Action> dependencies) {
        for (Direction direction : PositionUtil.DIRECTIONS) {
            BlockPos offset = pos.relative(direction);
            if (isValidPlacePos(offset, level, player) && strictDirectionCheck(offset, direction.getOpposite(), level, player)) {
                return direction;
            }
        }

        for (Direction direction : PositionUtil.DIRECTIONS) {
            BlockPos offset = pos.relative(direction);
            if (hasActionCreatingPlacePos(offset, direction, player, level, dependencies)) {
                return direction;
            }
        }

        return null;
    }

    default boolean isValidPlacePos(BlockPos pos, ClientLevel level, Player player) {
        if (!level.getWorldBorder().isWithinBounds(pos) || isOutsideRange(pos, player)) {
            return false;
        }

        BlockState offsetState = level.getBlockState(pos);
        return !offsetState.isAir() && offsetState.getFluidState().isEmpty();
    }

    default boolean isOutsideRange(BlockPos pos, Player player) {
        return player.position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5 - player.getEyeHeight(), pos.getZ() + 0.5)
                > ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE;
    }

    default boolean hasActionCreatingPlacePos(BlockPos pos, @Nullable Direction direction, Player player, ClientLevel level, @Nullable Set<BlockPlacer.Action> dependencies) {
        for (BlockPlacer.Action action : getBlockPlacer().getActions()) {
            if (action.getPos().equals(pos)) {
                if (direction != null && getBlockPlacer().getAntiCheat().getStrictDirection().getValue() != StrictDirection.Type.Vanilla) {
                    if (action.getItem() instanceof BlockItem blockItem) {
                        BlockStateLevel.Delegating customBlockStateLevel = new BlockStateLevel.Delegating(level);
                        customBlockStateLevel.getMap().put(action.getPos(), blockItem.getBlock().defaultBlockState());
                        if (!strictDirectionCheck(action.getPos(), direction.getOpposite(), customBlockStateLevel, player)) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }

                if (dependencies != null) {
                    dependencies.add(action);
                }

                return true;
            }
        }

        return false;
    }

    default boolean strictDirectionCheck(BlockPos pos, Direction direction, ClientLevel level, Player player) {
        return getBlockPlacer().getAntiCheat().getStrictDirectionCheck().strictDirectionCheck(pos, direction, level, player);
    }

}
