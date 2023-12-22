package me.earth.phobot.pathfinder;

import me.earth.phobot.event.RenderEvent;
import me.earth.phobot.pathfinder.algorithm.PathfindingNode;
import me.earth.phobot.util.render.Renderer;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;

import java.awt.*;
import java.util.Iterator;

public class PathRenderer extends SubscriberImpl {
    public PathRenderer(Path<? extends PathfindingNode<?>> path, PingBypass pingBypass) {
        listen(new Listener<RenderEvent>() {
            @Override
            public void onEvent(RenderEvent event) {
                if (path.isValid()) {
                    Iterator<? extends PathfindingNode<?>> itr = path.getPath().iterator();
                    if (itr.hasNext()) {
                        Renderer.startLines(1.0f, true);
                        event.getLineColor().set(Color.CYAN);
                        PathfindingNode<?> previous = itr.next();
                        while (itr.hasNext()) {
                            event.getTo().set(previous.getRenderX(), previous.getRenderY(), previous.getRenderZ());
                            previous = itr.next();
                            event.getFrom().set(previous.getRenderX(), previous.getRenderY(), previous.getRenderZ());
                            Renderer.drawLine(event);
                        }

                        Renderer.end(true);
                    }
                } else {
                    pingBypass.getEventBus().unsubscribe(this);
                }
            }
        });
    }

}
