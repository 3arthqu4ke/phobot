package me.earth.phobot.modules.misc;

import lombok.Getter;
import me.earth.phobot.modules.PhobotNameSpacedModule;
import me.earth.phobot.util.math.MathUtil;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.event.CancellableEvent;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.gui.hud.DisplaysHudInfo;
import me.earth.pingbypass.api.module.impl.Categories;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Reach extends PhobotNameSpacedModule implements DisplaysHudInfo {
    public Reach(PingBypass pingBypass) {
        super(pingBypass, "Reach", Categories.RENDER, "Allows you to reach further.");
        listen(new Listener<Reach.Event>() {
            @Override
            public void onEvent(Reach.Event event) {
                event.setCancelled(true);
            }
        });
    }

    @Override
    public String getHudInfo() {
        HitResult result = mc.hitResult;
        LocalPlayer p = mc.player;
        if (result instanceof BlockHitResult bhr && p != null) {
            return MathUtil.round(p.getEyePosition().distanceTo(new Vec3(bhr.getBlockPos().getX() + 0.5, bhr.getBlockPos().getY() + 0.5, bhr.getBlockPos().getZ() + 0.5)), 2) + "";
        } else if (result instanceof EntityHitResult ehr && p != null) {
            return MathUtil.round(Math.sqrt(ehr.getEntity().getBoundingBox().distanceToSqr(p.getEyePosition())), 2) + "";
        }

        return null;
    }

    @Getter
    public static class Event extends CancellableEvent {
        private final float range = 6.0f;
    }

}
