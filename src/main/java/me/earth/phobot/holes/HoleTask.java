package me.earth.phobot.holes;

import me.earth.phobot.invalidation.AbstractInvalidationTask;
import me.earth.phobot.invalidation.ChunkWorker;
import me.earth.phobot.util.mutables.MutPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

class HoleTask extends AbstractInvalidationTask<Hole, HoleManager> implements HoleBlocks, HoleOffsets {
    public HoleTask(BlockableEventLoop<Runnable> mc, LevelChunk chunk, ChunkWorker worker, int h, int minH, HoleManager manager) {
        super(mc, chunk, worker, h, minH, manager);
    }

    public HoleTask(Map<BlockPos, Hole> map, BlockableEventLoop<Runnable> scheduler, Level level, MutPos pos, HoleManager manager,
                    @Nullable ChunkWorker chunk, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        super(map, scheduler, level, pos, manager, chunk, minX, maxX, minY, maxY, minZ, maxZ);
    }

    @Override
    protected void handleOverwritten(BlockPos pos, Hole overwritten, Hole by) {
        if (overwritten.isAirPart(pos)) {
            overwritten.invalidate();
        }
    }

    @Override
    public void calc(MutPos pos) {
        Objects.requireNonNull(getChunk());
        Boolean safe = checkAirAndFloor(pos, true, false);
        if (safe == null) {
            return;
        }

        boolean _2x1 = false;
        boolean _2x2;
        // -> x a
        pos.setX(pos.getX() - 1);
        BlockState state = Objects.requireNonNull(getLevel()).getBlockState(pos);
        if (state.getBlock() != Blocks.BEDROCK) {
            if (!unsafeBlocks().contains(state.getBlock())) {
                return;
            }

            safe = false;
        }
        // x a
        //   x <-
        pos.setX(pos.getX() + 1);
        pos.setZ(pos.getZ() - 1);
        state = getLevel().getBlockState(pos);
        if (state.getBlock() != Blocks.BEDROCK) {
            if (!unsafeBlocks().contains(state.getBlock())) {
                return;
            }

            safe = false;
        }
        //   x a x <-
        //     x
        pos.setX(pos.getX() + 1);
        pos.setZ(pos.getZ() + 1);
        state = getLevel().getBlockState(pos);
        if (state.getBlock() != Blocks.BEDROCK) {
            if (state.getCollisionShape(getLevel(), pos).isEmpty()) {
                safe = checkAirAndFloor(pos, safe, true);
                if (safe == null) {
                    return;
                }

                // TODO: track safety for 2x1 holes!
                _2x1 = true;
                //   x a a
                //     x x <-
                pos.setZ(pos.getZ() - 1);
                state = getLevel().getBlockState(pos);
                //noinspection DuplicatedCode
                if (!noBlastBlocks().contains(state.getBlock())) {
                    return;
                }

                pos.setZ(pos.getZ() + 1);
                pos.setX(pos.getX() + 1);
                //   x a a x <-
                //     x x
                state = getLevel().getBlockState(pos);
                if (!noBlastBlocks().contains(state.getBlock())) {
                    return;
                }

                //  Go back here v
                //           x a a x
                //             x x
                pos.setX(pos.getX() - 1);
            } else if (!unsafeBlocks().contains(state.getBlock())) {
                return;
            }

            safe = false;
        }
        //  -> x
        //   x a x
        //     x
        pos.setX(pos.getX() - 1);
        pos.setZ(pos.getZ() + 1);
        state = getLevel().getBlockState(pos);
        if (state.getBlock() != Blocks.BEDROCK) {
            if (state.getCollisionShape(getLevel(), pos).isEmpty()) {
                safe = checkAirAndFloor(pos, safe, true);
                if (safe == null) {
                    return;
                }

                _2x2 = _2x1;
                //
                // -> x a
                //    x a x
                //      x
                pos.setX(pos.getX() - 1);
                //noinspection DuplicatedCode
                state = getLevel().getBlockState(pos);
                if (!noBlastBlocks().contains(state.getBlock())) {
                    return;
                }

                //   -> x
                //    x a
                //    x a x
                //      x
                pos.setZ(pos.getZ() + 1);
                pos.setX(pos.getX() + 1);
                state = getLevel().getBlockState(pos);
                if (!noBlastBlocks().contains(state.getBlock())) {
                    return;
                }

                //      x                   x
                //    x a x <-    or      x a a <-
                //    x a x               x a a x
                //      x                   x x
                pos.setX(pos.getX() + 1);
                pos.setZ(pos.getZ() - 1);
                state = getLevel().getBlockState(pos);
                if (!noBlastBlocks().contains(state.getBlock())) {
                    if (state.getCollisionShape(getLevel(), pos).isEmpty() && _2x2) {
                        safe = checkAirAndFloor(pos, safe, true);
                        if (safe == null) {
                            return;
                        }

                        //      x x <-
                        //    x a a
                        //    x a a x
                        //      x x
                        pos.setZ(pos.getZ() + 1);
                        state = getLevel().getBlockState(pos);
                        if (!noBlastBlocks().contains(state.getBlock())) {
                            return;
                        }

                        //      x x
                        //    x a a x <-
                        //    x a a x
                        //      x x
                        pos.setZ(pos.getZ() - 1);
                        pos.setX(pos.getX() + 1);
                        state = getLevel().getBlockState(pos);
                        if (!noBlastBlocks().contains(state.getBlock())) {
                            return;
                        }

                        Hole hole = new Hole(getChunk(), pos.getX() - 2, pos.getY(), pos.getZ() - 1, pos.getX(),
                                pos.getZ() + 1, false, true, false);
                        putHole(pos, hole, OFFSETS_2x2);
                    }

                    return;
                }

                if (_2x1) {
                    return;
                }

                //      x
                //    x a x <- reminder, we are here
                //    x a x
                //      x
                Hole hole = new Hole(getChunk(), pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX(),
                        pos.getZ() + 1, true, false, false);
                putHole(pos, hole, OFFSETS_2x1_Z);
                return;
            } else if (!unsafeBlocks().contains(state.getBlock())) {
                return;
            }

            safe = false;
        }

        if (_2x1) {
            //     x x <-
            //   x a a x
            //     x x
            pos.setX(pos.getX() + 1);
            state = getLevel().getBlockState(pos);
            if (noBlastBlocks().contains(state.getBlock())) {
                Hole hole = new Hole(getChunk(), pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 1,
                        pos.getZ(), true, false, false);
                putHole(pos, hole, OFFSETS_2x1_X);
            }

            return;
        }
        //     x <- reminder, we are here rn
        //   x a x
        //     x
        Hole hole = new Hole(getChunk(), pos.getX(), pos.getY(), pos.getZ() - 1, pos.getX() + 1, pos.getZ(),
                false, false, safe);
        putHole(pos, hole, OFFSETS_1x1);
    }

    private Boolean checkAirAndFloor(MutPos pos, boolean safe, boolean alreadyCheckedFirst) {
        if (alreadyCheckedFirst) {
            pos.setY(pos.getY() + 1);
        }

        if (checkAir(pos) || checkAir(pos) || !alreadyCheckedFirst && checkAir(pos)) {
            return null;
        }

        pos.setY(pos.getY() - 4);
        assert getLevel() != null;
        BlockState state = getLevel().getBlockState(pos);
        if (state.getBlock() != Blocks.BEDROCK) {
            if (!unsafeBlocks().contains(state.getBlock())) {
                return null;
            }

            pos.setY(pos.getY() + 1);
            return Boolean.FALSE;
        }

        pos.setY(pos.getY() + 1);
        return safe;
    }

    private boolean checkAir(MutPos pos) {
        if (!Objects.requireNonNull(getLevel()).getBlockState(pos).getCollisionShape(getLevel(), pos).isEmpty()) {
            return true;
        }

        pos.setY(pos.getY() + 1);
        return false;
    }

    private void putHole(MutPos pos, Hole hole, Vec3i[] offsets) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        hole.setMap(getManager().getMap());
        for (Vec3i offset : offsets) {
            // this was a bug in 3arthh4ck, we just moved the pos, but the offsets are not incremental
            pos.set(x + offset.getX(), y + offset.getY(), z + offset.getZ());
            BlockPos immutable = pos.immutable();
            if (hole.getPositions().add(immutable)) {
                Hole before = getMap().put(immutable, hole);
                if (before != null && before.isAirPart(immutable)) {
                    before.invalidate();
                }
            }
        }
    }

}
