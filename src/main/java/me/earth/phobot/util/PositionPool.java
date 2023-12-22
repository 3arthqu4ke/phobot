package me.earth.phobot.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.earth.phobot.util.mutables.MutPos;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Getter
public class PositionPool<T> {
    private final T[] positions;

    public PositionPool(int radius, Function<BlockPos, T> factory, T[] emptyArray) {
        this(new Builder<>(), radius, factory, emptyArray);
    }

    public PositionPool(Builder<T> builder, int radius, Function<BlockPos, T> factory, T[] emptyArray) {
        this.positions = builder.build(radius, factory).toArray(emptyArray);
    }

    @Getter
    @RequiredArgsConstructor
    public static class Pos extends MutPos {
        private final BlockPos pos;

        public void setOffsetFromPoolCenter(BlockPos center) {
            set(pos.getX() + center.getX(), pos.getY() + center.getY(), pos.getZ() + center.getZ());
        }
    }

    public static class Builder<T> {
        public List<T> build(int radius, Function<BlockPos, T> factory) {
            List<T> positions = new ArrayList<>();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (inRange(pos, radius)) {
                            add(factory.apply(pos), positions);
                        }
                    }
                }
            }

            sort(positions);
            return positions;
        }

        protected boolean inRange(BlockPos pos, int radius) {
            return pos.distSqr(BlockPos.ZERO) < radius * radius;
        }

        protected void add(T pos, List<T> positions) {
            positions.add(pos);
            // the map is used by BlockPathfinder
        }

        protected void sort(@SuppressWarnings("unused") List<T> positions) {
            // to be implemented by subclasses
        }
    }

}
