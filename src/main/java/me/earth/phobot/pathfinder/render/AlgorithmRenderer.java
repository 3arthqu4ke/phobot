package me.earth.phobot.pathfinder.render;

import lombok.AccessLevel;
import lombok.Getter;
import me.earth.phobot.event.RenderEvent;
import me.earth.phobot.pathfinder.algorithm.PathfindingNode;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.movement.MovementNode;
import me.earth.phobot.pathfinder.movement.MovementPathfindingAlgorithm;
import me.earth.phobot.util.render.Renderer;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;

import java.awt.*;

/**
 * Renders {@link RenderableAlgorithm}s.
 *
 * @param <T> the type of PathfindingNode used by the Algorithm to render.
 */
public class AlgorithmRenderer<T extends PathfindingNode<T>> extends SubscriberImpl {
    private static final int MAX_NODES = Integer.parseInt(System.getProperty("phobot.algorithm.render.max.nodes", "1000"));

    @Getter(AccessLevel.PROTECTED)
    private final RenderableAlgorithm<T> algorithm;

    public AlgorithmRenderer(RenderableAlgorithm<T> algorithm) {
        this.algorithm = algorithm;
        listen(new Listener<RenderEvent>() {
            @Override
            public void onEvent(RenderEvent event) {
                render(event);
            }
        });
    }

    public void render(RenderEvent event) {
        RenderableAlgorithm<T> algorithm = this.getAlgorithm();
        if (algorithm == null) {
            return;
        }

        T current = algorithm.getCurrent();
        if (current != null) {
            Renderer.startLines(1.5f, true);
            event.getLineColor().set(Color.RED);
            event.getBoxColor().set(Color.RED);
            event.getAabb().set(current.getRenderX() - 0.125, current.getRenderY() - 0.125, current.getRenderZ() - 0.125,
                                current.getRenderX() + 0.125, current.getRenderY() + 0.125, current.getRenderZ() + 0.125);
            Renderer.drawAABBOutline(event);
            if (algorithm instanceof MovementPathfindingAlgorithm movementPathfindingAlgorithm && !((MovementNode) current).isGoal()) {
                MeshNode mesh = movementPathfindingAlgorithm.getPath().getPath().get(((MovementNode) current).targetNodeIndex());
                event.getLineColor().set(Color.CYAN);
                event.getAabb().set(mesh.getRenderX() - 0.125, mesh.getRenderY() - 0.125, mesh.getRenderZ() - 0.125,
                                    mesh.getRenderX() + 0.125, mesh.getRenderY() + 0.125, mesh.getRenderZ() + 0.125);
                Renderer.drawAABBOutline(event);
                event.getLineColor().set(Color.RED);
            }

            T goal = algorithm.getGoal();
            if (goal != null) {
                event.getAabb().set(goal.getRenderX() - 0.125, goal.getRenderY() - 0.125, goal.getRenderZ() - 0.125,
                                    goal.getRenderX() + 0.125, goal.getRenderY() + 0.125, goal.getRenderZ() + 0.125);
                Renderer.drawAABBOutline(event);
            }

            for (int i = 0; i < MAX_NODES; i++) { // just to ensure this does not become infinite for any reason
                T previous = algorithm.getCameFrom(current);
                if (previous != null) {
                    event.getTo().set(current.getRenderX(), current.getRenderY(), current.getRenderZ());
                    event.getFrom().set(previous.getRenderX(), previous.getRenderY(), previous.getRenderZ());
                    Renderer.drawLine(event);
                    current = previous;
                } else {
                    Renderer.end(true);
                    return;
                }
            }

            Renderer.end(true);
        }
    }

}
