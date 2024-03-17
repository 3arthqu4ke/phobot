package me.earth.phobot.pathfinder.render;

import me.earth.phobot.event.RenderEvent;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.setting.Setting;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages multiple {@link AlgorithmRenderer}s.
 */
public class AlgorithmRendererManager extends SubscriberImpl {
    private final List<AlgorithmRenderer<?>> renderers = new CopyOnWriteArrayList<>();
    private final Setting<Boolean> render;

    public AlgorithmRendererManager(Setting<Boolean> render) {
        this.render = render;
        listen(new Listener<RenderEvent>() {
            @Override
            public void onEvent(RenderEvent event) {
                onRender(event);
            }
        });
    }

    protected void onRender(RenderEvent event) {
        if (render.getValue()) {
            for (AlgorithmRenderer<?> algorithmRenderer : renderers) {
                algorithmRenderer.render(event);
            }
        }
    }

    public void add(AlgorithmRenderer<?> renderer) {
        renderers.add(renderer);
    }

    public void remove(AlgorithmRenderer<?> renderer) {
        renderers.remove(renderer);
    }

}
