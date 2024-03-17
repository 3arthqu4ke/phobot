package me.earth.phobot.util.world;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface BlockStateLevel extends CollisionGetter {
    Map<BlockPos, BlockState> getMap();

    ClientLevel getLevel();

    @Override
    default BlockState getBlockState(BlockPos pos) {
        BlockState state = getMap().get(pos);
        if (state == null) {
            state = getLevel().getBlockState(pos);
        }

        return state;
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class BlockStateLevelImpl implements BlockStateLevel {
        private final ClientLevel level;
        private final Map<BlockPos, BlockState> map;
        private final BlockGetter chunkForCollisions;

        public BlockStateLevelImpl(ClientLevel level) {
            this(level, new HashMap<>());
        }

        public BlockStateLevelImpl(ClientLevel level, Map<BlockPos, BlockState> map) {
            this.level = level;
            this.map = map;
            this.chunkForCollisions = new BlockStateLevelImpl(level, this.map, level) {
                @Override
                public @Nullable BlockGetter getChunkForCollisions(int x, int z) {
                    return BlockStateLevelImpl.this.level.getChunkForCollisions(x, z);
                }
            };
        }

        @Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos blockPos) {
            return level.getBlockEntity(blockPos);
        }

        @Override
        public FluidState getFluidState(BlockPos blockPos) {
            return level.getFluidState(blockPos);
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return level.getMinBuildHeight();
        }

        @Override
        public WorldBorder getWorldBorder() {
            return level.getWorldBorder();
        }

        @Nullable
        @Override
        public BlockGetter getChunkForCollisions(int x, int z) {
            return chunkForCollisions;
        }

        @Override
        public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB bb) {
            return level.getEntityCollisions(entity, bb);
        }
    }

    /**
     * IMPORTANT: If you want to override {@link #getBlockState(BlockPos)} or other methods, do it by overriding {@link #getImpl(ClientLevel)}!
     */
    class Delegating extends UnusableLevels.UnusableBlockStateLevel implements BlockStateLevel {
        public Delegating(ClientLevel level) {
            super(level);
        }

        @Override
        public int getHeight() {
            return this.dimensionType().height();
        }

        @Override
        public int getMinBuildHeight() {
            return this.dimensionType().minY();
        }

        @Override
        public int getMaxBuildHeight() {
            return this.getMinBuildHeight() + this.getHeight();
        }

        @Override
        public int getSectionsCount() {
            return this.getMaxSection() - this.getMinSection();
        }

        @Override
        public int getMinSection() {
            return SectionPos.blockToSectionCoord(this.getMinBuildHeight());
        }

        @Override
        public int getMaxSection() {
            return SectionPos.blockToSectionCoord(this.getMaxBuildHeight() - 1) + 1;
        }

        @Override
        public boolean isEmptyBlock(BlockPos blockPos) {
            return this.getBlockState(blockPos).isAir();
        }
    }

    class Empty implements BlockStateLevel {
        private final Map<BlockPos, BlockState> map = new HashMap<>();
        private final WorldBorder worldBorder = new WorldBorder();

        @Override
        public WorldBorder getWorldBorder() {
            return worldBorder;
        }

        @Nullable
        @Override
        public BlockGetter getChunkForCollisions(int x, int z) {
            return null;
        }

        @Override
        public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB aABB) {
            return new ArrayList<>(0);
        }

        @Override
        public Map<BlockPos, BlockState> getMap() {
            return map;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return map.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public ClientLevel getLevel() {
            throw new UnsupportedOperationException("Empty CustomBlockStateLevel getLevel has been called!");
        }

        @Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public int getMinBuildHeight() {
            return 0;
        }
    }

}
