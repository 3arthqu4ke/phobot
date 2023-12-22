package me.earth.phobot.services;

import me.earth.phobot.event.ChangeWorldEvent;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;

import java.util.concurrent.atomic.AtomicInteger;

// TODO: use this for Holes and MeshNodes!
public class WorldVersionService extends SubscriberImpl {
    private final AtomicInteger worldVersion = new AtomicInteger();

    public WorldVersionService() {
        listen(new Listener<ChangeWorldEvent>() {
            @Override
            public void onEvent(ChangeWorldEvent changeWorldEvent) {
                worldVersion.incrementAndGet();
            }
        });
    }

    public int getWorldVersion() {
        return worldVersion.get();
    }

}
