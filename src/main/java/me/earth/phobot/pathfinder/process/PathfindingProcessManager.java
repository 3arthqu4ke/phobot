package me.earth.phobot.pathfinder.process;

import lombok.Synchronized;
import me.earth.phobot.event.ChangeWorldEvent;
import me.earth.phobot.pathfinder.util.CancellableFuture;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;

import java.util.HashSet;
import java.util.Set;

public class PathfindingProcessManager extends SubscriberImpl {
    private final Set<CancellableFuture<?>> futures = new HashSet<>();

    public PathfindingProcessManager() {
        listen(new Listener<ChangeWorldEvent>() {
            @Override
            public void onEvent(ChangeWorldEvent event) {
                cancelAll();
            }
        });
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
