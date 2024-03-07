package me.earth.phobot.modules.render;

import me.earth.phobot.event.RenderEvent;
import me.earth.phobot.holes.Hole;
import me.earth.phobot.holes.HoleManager;
import me.earth.phobot.util.render.Renderer;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.loop.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.awt.*;
import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;

public class HolesRenderListener extends SubscriberImpl {
    private static int RENDER_ID = 1;
    private final PriorityQueue<Hole> holes;

    public HolesRenderListener(HoleManager holeManager, Holes module) {
        Minecraft mc = module.getMinecraft();
        // sorted by negative distance, because we want to remove the furthest hole in case there is too many holes to render and PriorityQueue only offers poll to remove the furthest
        this.holes = new PriorityQueue<>(Comparator.comparingDouble(hole -> -hole.getDistanceSqr(Objects.requireNonNull(mc.player))));
        listen(new Listener<RenderEvent>() {
            @Override
            public void onEvent(RenderEvent event) {
                for (Hole hole : holes) {
                    Color color = hole.is1x1() ? (hole.isSafe() ? Color.GREEN : Color.RED) : hole.is2x1() ? Color.MAGENTA : Color.CYAN;
                    event.getAabb().set(hole.getX(), hole.getY(), hole.getZ(), hole.getMaxX(), hole.getY() + (!hole.is1x1() ? 0 : 1), hole.getMaxZ());
                    event.setBoxColor(color, 0.2f);
                    Renderer.renderBoxWithOutlineAndSides(event, 1.0f, true);
                }
            }
        });

        listen(new Listener<TickEvent>() {
            @Override
            public void onEvent(TickEvent event) {
                holes.clear();
                LocalPlayer player = mc.player;
                if (player != null && module.getRender().getValue()) {
                    RENDER_ID++;
                    double rangeSq = module.getRange().getValue() * module.getRange().getValue();
                    for (Hole hole : holeManager.getMap().values()) {
                        if (hole.isValid() && hole.getVisitId() != RENDER_ID && hole.getDistanceSqr(player) <= rangeSq) {
                            hole.setVisitId(RENDER_ID);
                            holes.add(hole);
                            if (holes.size() > module.getHoles().getValue()) {
                                holes.poll();
                            }
                        }
                    }
                }
            }
        });
    }

}
