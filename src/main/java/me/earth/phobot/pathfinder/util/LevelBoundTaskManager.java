package me.earth.phobot.pathfinder.util;

import lombok.Synchronized;
import me.earth.phobot.event.ChangeWorldEvent;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages tasks (represented by {@link CancellableFuture}s) related to a certain {@link Level}.
 * If a {@link ChangeWorldEvent} occurs all currently running tasks will be cancelled.
 */
public class LevelBoundTaskManager extends SubscriberImpl {
    private final Set<CancellableFuture<?>> futures = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public LevelBoundTaskManager() {
        listen(new Listener<ChangeWorldEvent>(Integer.MAX_VALUE) {
            @Override
            public void onEvent(ChangeWorldEvent event) {
                cancelAll();
            }
        });

        // TODO: respawn event too?
    }

    @Synchronized
    public void addFuture(CancellableFuture<?> future) {
        future.whenComplete((r,t) -> removeFuture(future));
        futures.add(future);
    }

    @Synchronized
    public void removeFuture(CancellableFuture<?> future) {
        futures.remove(future);
    }

    @Synchronized
    public void cancelAll() {
        futures.removeIf(future -> {
            future.cancel(true);
            return true;
        });
    }

}
