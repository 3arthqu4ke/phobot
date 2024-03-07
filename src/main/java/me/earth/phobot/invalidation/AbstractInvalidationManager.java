package me.earth.phobot.invalidation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.event.ChangeWorldEvent;
import me.earth.phobot.event.UnloadChunkEvent;
import me.earth.phobot.util.mutables.MutPos;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.event.network.PostListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

// TODO: block changes at chunk borders while a chunk is being calculated could be problematic?
/**
 * Instead of scanning the world periodically we can create a model of it when a chunk is loaded.
 * Then, when a block inside that chunk changes, we just need to check blocks around that changed block.
 * This can e.g. be useful to create a more efficient HoleESP. Usual HoleESP implementations check the world for holes
 * in a certain interval, but with this we only need to check it once, when a chunk is loaded and then
 * only if blocks change.
 *
 * @param <T> the type of object maintained by this manager, e.g. holes.
 * @param <C> the type of the InvalidationConfig used by this manager.
 */
@Slf4j
public abstract class AbstractInvalidationManager<T extends CanBeInvalidated, C extends InvalidationConfig> extends AbstractBlockChangeListener<Runnable> {
    protected final MutPos mutPos = new MutPos();
    @Getter
    protected final Map<BlockPos, T> map;
    @Getter
    protected final C config;

    public AbstractInvalidationManager(C config, Map<BlockPos, T> map) {
        super(config.getMinecraft());
        this.config = config;
        this.map = map;
        listen(new Listener<UnloadChunkEvent>() {
            @Override
            public void onEvent(UnloadChunkEvent event) {
                runTask(() -> {
                    getChunkWorker(event.chunk()).incrementVersion();
                    removeInvalids(event.chunk());
                });
            }
        });

        // Rn, we check the entire chunk instantly, that's a lot. Maybe we should just check parts of the chunk?
        listen(new PostListener.Safe<ClientboundLevelChunkWithLightPacket>(mc) {
            @Override
            public void onSafeEvent(PacketEvent.PostReceive<ClientboundLevelChunkWithLightPacket> event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                ClientboundLevelChunkWithLightPacket packet = event.getPacket();
                LevelChunk levelChunk = level.getChunk(packet.getX(), packet.getZ());
                ChunkWorker chunk = getChunkWorker(levelChunk);
                chunk.setWorking(true);
                chunk.incrementVersion();
                AbstractInvalidationTask<?, ?> invalidationTask = getChunkTask(levelChunk, chunk);
                runChunkTaskWithRetry(levelChunk, chunk, invalidationTask);
            }
        });

        listen(new Listener<ChangeWorldEvent>() {
            @Override
            public void onEvent(ChangeWorldEvent tickEvent) {
                reset();
            }
        });
    }

    @Override
    protected void onBlockStateChange(BlockPos pos, BlockState state, LevelChunk chunk) {
        this.onBlockStateChange(pos, state, getChunkWorker(chunk), chunk);
    }

    @Override
    protected void handle(List<Runnable> callbacks) {
        callbacks.forEach(Runnable::run);
    }

    // sometimes async AbstractInvalidationTasks fail, see AbstractInvalidationTask.execute, so we retry on the mainthread
    protected void runChunkTaskWithRetry(LevelChunk levelChunk, ChunkWorker chunk, AbstractInvalidationTask<?, ?> chunkTask) {
        runTask(() -> {
            try {
                removeInvalids(levelChunk);
                chunkTask.run();
                mc.submit(() -> chunk.setWorking(false));
            } catch (Exception e) {
                if (!mc.isSameThread()) {
                    log.info("Retrying failed chunk task " + chunkTask + " on main thread.");
                    chunkTask.reset();
                    mc.submit(() -> {
                        chunkTask.reset(); // just to be sure about volatility stuff, idk, calling this twice should not be expensive
                        chunkTask.run();
                        chunk.setWorking(false);
                        log.info("Task " + chunkTask + " scheduled for retry has completed successfully!");
                    });
                } else {
                    mc.submit(() -> chunk.setWorking(false));
                }

                throw e;
            }
        });
    }

    protected void onBlockStateChange(BlockPos pos, BlockState state, ChunkWorker worker, LevelChunk chunk) {
        addInvalidateTask(chunk, pos, state, worker);
        if (callbacks == null) {
            addPostWorkingTask(pos, state, worker, chunk);
        } else {
            callbacks.add(() -> addPostWorkingTask(pos, state, worker, chunk));
        }
    }

    protected void addInvalidateTask(LevelChunk chunk, BlockPos pos, BlockState state, ChunkWorker worker) {
        worker.addTask(() -> {
            mutPos.setX(pos.getX());
            mutPos.setY(pos.getY());
            mutPos.setZ(pos.getZ());
            invalidate(chunk, mutPos, state, worker);
        });
    }

    protected abstract AbstractInvalidationTask<?, ?> getChunkTask(LevelChunk levelChunk, ChunkWorker chunk);

    protected abstract ChunkWorker getChunkWorker(LevelChunk chunk);

    protected abstract void addPostWorkingTask(BlockPos pos, BlockState state, ChunkWorker worker, LevelChunk chunk);

    protected abstract void invalidate(LevelChunk chunk, MutPos pos, BlockState state, ChunkWorker worker);

    // TODO: with the XZMap this can happen much more efficiently!
    protected void removeInvalids(LevelChunk chunk) {
        getMap().entrySet().removeIf(Predicate.not(e -> e.getValue().isValid()));
    }

    protected void reset() {
        getMap().clear();
    }

    private void runTask(Runnable task) {
        if (config.isAsync()) {
            config.getExecutor().submit(task);
        } else {
            task.run();
        }
    }

}
