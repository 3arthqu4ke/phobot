package me.earth.phobot.modules.client;

import lombok.RequiredArgsConstructor;
import me.earth.phobot.event.RenderEvent;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManager;
import me.earth.phobot.util.render.Renderer;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.util.Set;

@RequiredArgsConstructor
public class GraphDebugRenderer extends Listener<RenderEvent> {
    private final NavigationMeshManager navigationMeshManager;
    private final Pathfinding module;
    private final Minecraft mc;

    @Override
    public void onEvent(RenderEvent event) {
        if (module.getRender().getValue()) {
            assert mc.player != null;
            for (int x = (int) (mc.player.getX() - 10); x <= mc.player.getX() + 10; x++) {
                for (int z = (int) (mc.player.getZ() - 10); z <= mc.player.getZ() + 10; z++) {
                    Set<MeshNode> nodes = navigationMeshManager.getXZMap().get(x, z);
                    if (nodes != null) {
                        for (MeshNode node : nodes) {
                            if (node.isValid() && node.distanceSq(mc.player.position()) <= 100) {
                                for (int i = 0; i < MeshNode.OFFSETS.length; i++) {
                                    double xOff = MeshNode.OFFSETS[i].getX();
                                    double zOff = MeshNode.OFFSETS[i].getZ();

                                    MeshNode adjacent = node.getAdjacent()[i];
                                    Color color = Color.GREEN;
                                    if (adjacent == null) {
                                        continue;
                                    } else if (!adjacent.isValid()) {
                                        color = Color.RED;
                                    }

                                    // TODO: maybe make line point somewhere?
                                    event.getLineColor().set(color);
                                    event.getAabb().set(
                                            node.getX() + 0.5 + xOff / 3.9,
                                            node.getY(),
                                            node.getZ() + 0.5 + zOff / 3.9,
                                            node.getX() + 0.5 + xOff / 3.9 + 0.01,
                                            node.getY(),
                                            node.getZ()  + 0.5 + zOff / 3.9 + 0.01
                                    );

                                    Renderer.startLines(1.5f, true);
                                    Renderer.drawAABBOutline(event);
                                    Renderer.end(true);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
