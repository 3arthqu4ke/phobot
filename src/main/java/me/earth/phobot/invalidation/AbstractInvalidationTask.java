package me.earth.phobot.invalidation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.util.mutables.MutPos;
import net.minecraft.core.BlockPos;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@RequiredArgsConstructor
public abstract class AbstractInvalidationTask<I extends CanBeInvalidated, M extends AbstractInvalidationManager<I, ?>>
        implements Runnable {
    private final Map<BlockPos, I> map;
    private final BlockableEventLoop<Runnable> scheduler;
    private final Level level;
    private final MutPos pos;
    private final M manager;

    private final @Nullable ChunkWorker chunk;
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final int minZ;
    private final int maxZ;

    public AbstractInvalidationTask(BlockableEventLoop<Runnable> scheduler, LevelChunk chunk, ChunkWorker chunkWorker, int height, int minHeight, M manager) {
        this(new HashMap<>(), scheduler, chunk.getLevel(), new MutPos(), manager, chunkWorker,
                // if two adjacent chunks get loaded after each other we need to check the positions at the borders
                chunk.getPos().x * 16 - 1, chunk.getPos().x * 16 + 16 + 1,
                minHeight, height,
                chunk.getPos().z * 16 - 1, chunk.getPos().z * 16 + 16 + 1);
    }

    @Override
    public void run() {
        try {
            execute();
            submitFinishTask();
        } catch (Exception e) {
            //TODO: fix when multihreading: Caused by: net.minecraft.world.level.chunk.MissingPaletteEntryException: Missing Palette entry for index 12.
            //   at net.minecraft.world.level.chunk.HashMapPalette.valueFor(HashMapPalette.java:64)
            // could this be a volatility issue?
            // For now the retry seems to work well.
            log.warn("Exception occurred in InvalidationTask: " + e.getMessage());
            throw e;
        }
    }

    public void reset() {
        map.clear();
    }

    protected void submitFinishTask() {
        scheduler.submit(this::finishTask);
    }

    protected abstract void calc(MutPos pos);

    protected abstract void handleOverwritten(BlockPos pos, I overwritten, I by);

    public void execute() {
        for (int y = getMinY(); y <= getMaxY(); y++) { // for GraphTasks it is important that we change y only after we have iterated over one layer
            for (int x = getMinX(); x < getMaxX(); x++) {
                for (int z = getMinZ(); z < getMaxZ(); z++) {
                    pos.set(x, y, z);
                    if (!map.containsKey(pos)) {
                        calc(pos);
                    }
                }
            }
        }
    }

    protected void finishTask() {
        map.forEach((pos, value) -> {
            I before = manager.getMap().put(pos, value);
            if (before != null && before != value) {
                handleOverwritten(pos, before, value);
            }
        });
    }

}
