package me.earth.phobot.modules.misc;

import me.earth.phobot.modules.combat.KillAura;
import me.earth.phobot.modules.combat.autocrystal.AutoCrystal;
import me.earth.phobot.util.math.RotationUtil;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.module.impl.ModuleImpl;
import me.earth.pingbypass.commons.event.SafeListener;
import me.earth.pingbypass.commons.event.loop.GameloopEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class Rotate extends ModuleImpl {
    public Rotate(PingBypass pingBypass, AutoCrystal autoCrystal, KillAura killAura) {
        super(pingBypass, "Rotate", Categories.MISC, "Makes you rotate.");
        listen(new SafeListener<GameloopEvent>(mc) {
            @Override
            public void onEvent(GameloopEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (mc.options.getCameraType().isFirstPerson()) {
                    Entity target = autoCrystal.targetedPlayer();
                    if (target == null) {
                        KillAura.Target killauraTarget = killAura.getTarget();
                        if (killauraTarget != null) {
                            target = killauraTarget.entity();
                        }
                    }

                    if (target != null) {
                        double x = Mth.lerp(mc.getFrameTime(), target.xo, target.getX());
                        double y = Mth.lerp(mc.getFrameTime(), target.yo, target.getY()) + target.getEyeHeight();
                        double z = Mth.lerp(mc.getFrameTime(), target.zo, target.getZ());
                        float[] rotations = RotationUtil.getLerpRotations(mc, player, x, y, z);
                        player.setYRot(rotations[0]);
                        player.setXRot(rotations[1]);
                    }
                }
            }
        });
    }

}
