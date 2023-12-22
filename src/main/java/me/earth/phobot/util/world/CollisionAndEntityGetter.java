package me.earth.phobot.util.world;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class CollisionAndEntityGetter implements CollisionGetter, EntityGetter {
    @Getter
    private final LevelEntityGetterAdapter<Entity> entities;
    private final List<AbstractClientPlayer> players;
    private final ClientLevel level;

    @Override
    public List<Entity> getEntities(@Nullable Entity ignore, AABB aABB, Predicate<? super Entity> predicate) {
        ArrayList<Entity> result = Lists.newArrayList();
        this.getEntities().get(aABB, entity -> {
            if (entity != ignore && predicate.test(entity)) {
                result.add(entity);
            }

            if (entity instanceof EnderDragon) {
                for (EnderDragonPart enderDragonPart : ((EnderDragon) entity).getSubEntities()) {
                    if (entity == ignore || !predicate.test(enderDragonPart)) {
                        continue;
                    }

                    result.add(enderDragonPart);
                }
            }
        });

        return result;
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB aABB, Predicate<? super T> predicate) {
        ArrayList<T> list = Lists.newArrayList();
        this.getEntities(entityTypeTest, aABB, predicate, list);
        return list;
    }

    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB aABB, Predicate<? super T> predicate, List<? super T> list) {
        this.getEntities(entityTypeTest, aABB, predicate, list, Integer.MAX_VALUE);
    }

    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB aABB, Predicate<? super T> predicate, List<? super T> list, int maxSize) {
        this.getEntities().get(entityTypeTest, aABB, entity -> {
            if (predicate.test(entity)) {
                list.add(entity);
                if (list.size() >= maxSize) {
                    return AbortableIterationConsumer.Continuation.ABORT;
                }
            }

            if (entity instanceof EnderDragon enderDragon) {
                for (EnderDragonPart enderDragonPart : enderDragon.getSubEntities()) {
                    T casted = entityTypeTest.tryCast(enderDragonPart);
                    if (casted == null || !predicate.test(casted)) {
                        continue;
                    }

                    list.add(casted);
                    if (list.size() < maxSize) {
                        continue;
                    }

                    return AbortableIterationConsumer.Continuation.ABORT;
                }
            }

            return AbortableIterationConsumer.Continuation.CONTINUE;
        });
    }

    @Override
    public List<AbstractClientPlayer> players() {
        return players;
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB aABB) {
        return EntityGetter.super.getEntityCollisions(entity, aABB);
    }

    @Override
    public boolean isUnobstructed(@Nullable Entity entity, VoxelShape voxelShape) {
        return EntityGetter.super.isUnobstructed(entity, voxelShape);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return level.getWorldBorder();
    }

    @Nullable
    @Override
    public BlockGetter getChunkForCollisions(int x, int z) {
        return level.getChunkForCollisions(x, z);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos blockPos) {
        return level.getBlockEntity(blockPos);
    }

    @Override
    public BlockState getBlockState(BlockPos blockPos) {
        return level.getBlockState(blockPos);
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

}
