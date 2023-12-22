package me.earth.phobot.pathfinder.process;

import lombok.Synchronized;
import me.earth.phobot.event.ChangeWorldEvent;
import me.earth.phobot.event.UnloadChunkEvent;
import me.earth.phobot.invalidation.AbstractBlockChangeListener;
import me.earth.phobot.pathfinder.Path;
import me.earth.phobot.pathfinder.algorithm.PathfindingNode;
import me.earth.phobot.util.collections.XZMap;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PathPool<T extends PathfindingNode<T>> extends AbstractBlockChangeListener<Runnable> {
    private final Map<BlockPos, Set<Path<T>>> map = new ConcurrentHashMap<>();
    private final XZMap<Set<Path<T>>> chunkMap = new XZMap<>(new ConcurrentHashMap<>());

    public PathPool(Minecraft mc) {
        super(mc);
        listen(new Listener<UnloadChunkEvent>() {
            @Override
            public void onEvent(UnloadChunkEvent event) {
                removeChunk(event.chunk().getPos().x, event.chunk().getPos().z);
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
        if (callbacks == null) {
            onBlockStateChange(pos, state);
        } else {
            callbacks.add(() -> onBlockStateChange(pos, state));
        }
    }

    @Override
    protected void handle(List<Runnable> callbacks) {
        callbacks.forEach(Runnable::run);
    }

    private void onBlockStateChange(BlockPos pos, BlockState state) {

    }

    public void addPath(Path<T> path) {
        mc.submit(() -> addPath0(path));
    }

    @Synchronized
    private void addPath0(Path<T> path) {
        map.computeIfAbsent(path.getStart(), v -> ConcurrentHashMap.newKeySet()).add(path);
        map.computeIfAbsent(path.getGoal(), v -> ConcurrentHashMap.newKeySet()).add(path);
        for (BlockPos air : path.getAffectedPositions()) {
            map.computeIfAbsent(air, v -> ConcurrentHashMap.newKeySet()).add(path);
        }

        if (PathfindingNode.class.isAssignableFrom(path.getType())) {
            for (Object node : path.getPath()) {
                if (node instanceof PathfindingNode<?> pathfindingNode) {
                    chunkMap.computeIfAbsent(pathfindingNode.getChunkX(), pathfindingNode.getChunkZ(), v -> ConcurrentHashMap.newKeySet()).add(path);
                }
            }
        }
    }

    @Synchronized
    public void removePath(Path<T> path) {
        path.invalidate();
        remove(map, path.getStart(), path);
        remove(map, path.getGoal(), path);
        for (BlockPos air : path.getAffectedPositions()) {
            remove(map, air, path);
        }

        if (PathfindingNode.class.isAssignableFrom(path.getType())) {
            for (Object node : path.getPath()) {
                if (node instanceof PathfindingNode<?> pathfindingNode) {
                    remove(chunkMap, chunkMap.getKey(pathfindingNode.getChunkX(), pathfindingNode.getChunkZ()), path);
                }
            }
        }
    }

    @Synchronized
    public void reset() {
        map.clear();
        chunkMap.clear();
    }

    @Synchronized
    private void removeChunk(int chunkX, int chunkZ) {
        var set = chunkMap.remove(chunkMap.getKey(chunkX, chunkZ));
        if (set != null) {
            for (Path<T> path : set) {
                removePath(path);
            }
        }
    }

    private <K> void remove(Map<K, Set<Path<T>>> map, K key, Path<T> path) {
        var set = map.get(key);
        if (set != null) {
            set.remove(path);
            if (set.isEmpty()) {
                map.remove(key, set);
            }
        }
    }

}
