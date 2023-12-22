package me.earth.phobot.damagecalc;

import lombok.RequiredArgsConstructor;
import me.earth.phobot.mixins.IVoxelShape;
import me.earth.phobot.util.mutables.MutAABB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@FunctionalInterface
public interface Raytracer {
    BlockHitResult clip(Level level, MutAABB mutAABB, ClipContext context);

    static Raytracer level() {
        return (level, mutAABB, context) -> level.clip(context);
    }

    static Raytracer cc() {
        return new RaytracerForCC();
    }

    /**
     * In FFA CC has a much simpler world, consisting only of Obsidian, Ender Chests, Anvils, Bedrock and Air.
     * No fluids etc., we can use this to make Raytracing much less resource intensive.
     */
    @RequiredArgsConstructor
    class RaytracerForCC implements Raytracer {
        private static final AABB FULL_BLOCK_BB = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        private static final AABB ENDER_CHEST_BB = new AABB(0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375);

        @Override
        public BlockHitResult clip(Level level, MutAABB mutAABB, ClipContext context) {
            Vec3 diff = context.getTo().subtract(context.getFrom());
            if (diff.lengthSqr() < Shapes.EPSILON) {
                return BlockHitResult.miss(context.getTo(), Direction.getNearest(diff.x, diff.y, diff.z), BlockPos.containing(context.getTo()));
            }

            Vec3 step = context.getFrom().add(diff.scale(0.001));
            return BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (clipContext, pos) -> {
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    return null;
                }

                VoxelShape shape = clipContext.getBlockShape(state, level, pos);
                return this.clipShape(mutAABB, shape, clipContext.getFrom(), clipContext.getTo(), diff, step, pos, state);
            }, c -> BlockHitResult.miss(context.getTo(), Direction.getNearest(diff.x, diff.y, diff.z), BlockPos.containing(context.getTo())));
        }

        private BlockHitResult clipShape(MutAABB bb, VoxelShape shape, Vec3 from, Vec3 to, Vec3 diff, Vec3 step, BlockPos pos, BlockState state) {
            if (shape.isEmpty()) {
                return null;
            }

            IVoxelShape access = ((IVoxelShape) shape);
            if (access.getShape().isFullWide(
                    access.invokeFindIndex(Direction.Axis.X, step.x - pos.getX()),
                    access.invokeFindIndex(Direction.Axis.Y, step.y - pos.getY()),
                    access.invokeFindIndex(Direction.Axis.Z, step.z - pos.getZ()))) {
                return new BlockHitResult(step, Direction.getNearest(diff.x, diff.y, diff.z).getOpposite(), pos, true);
            }

            if (shape == Shapes.block()) {
                bb.set(FULL_BLOCK_BB);
                return bb.singleClip(from, to, pos);
            } else if (state.is(Blocks.ENDER_CHEST)) {
                bb.set(ENDER_CHEST_BB);
                return bb.singleClip(from, to, pos);
            }

            return AABB.clip(shape.toAabbs(), from, to, pos);
        }
    }

}
