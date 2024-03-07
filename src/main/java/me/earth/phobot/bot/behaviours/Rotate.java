package me.earth.phobot.bot.behaviours;

import me.earth.phobot.bot.Bot;
import me.earth.phobot.modules.combat.KillAura;
import me.earth.phobot.modules.combat.autocrystal.AutoCrystal;
import me.earth.phobot.util.math.RotationUtil;
import me.earth.phobot.util.mutables.MutVec3;
import me.earth.pingbypass.api.event.SubscriberImpl;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.loop.GameloopEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Looks at the current bot target.
 */
public class Rotate extends SubscriberImpl {
    public Rotate(Bot bot) {
        AutoCrystal autoCrystal = bot.getModules().getAutoCrystal();
        KillAura killAura = bot.getModules().getKillAura();
        Minecraft mc = bot.getPingBypass().getMinecraft();
        MutVec3 vec3 = new MutVec3();
        listen(new Listener<GameloopEvent>() {
            @Override
            public void onEvent(GameloopEvent event) {
                LocalPlayer player = mc.player;
                if (player != null && mc.options.getCameraType().isFirstPerson() && bot.getRotate().getValue()) {
                    Entity target = bot.getTarget();
                    if (target == null) {
                        target = autoCrystal.getTarget();
                        if (target == null) {
                            KillAura.Target killauraTarget = killAura.getTarget();
                            if (killauraTarget != null) {
                                target = killauraTarget.entity();
                            }
                        }
                    }
                    // TODO: otherwise look in the direction we are moving into?
                    if (target != null) {
                        double x = Mth.lerp(mc.getFrameTime(), target.xo, target.getX());
                        double y = Mth.lerp(mc.getFrameTime(), target.yo, target.getY()) + target.getEyeHeight();
                        double z = Mth.lerp(mc.getFrameTime(), target.zo, target.getZ());
                        float[] rotations = RotationUtil.getLerpRotations(mc, player, x, y, z);
                        player.setYRot(rotations[0]);
                        player.setXRot(rotations[1]);
                    } else {
                        vec3.set(player.getDeltaMovement());
                        vec3.normalize();
                        vec3.scale(player.getEyeHeight());
                        double x = Mth.lerp(mc.getFrameTime(), player.xo + vec3.getX(), player.getX() + vec3.getX());
                        double y = Mth.lerp(mc.getFrameTime(), player.yo + vec3.getY(), player.getY() + vec3.getY());
                        double z = Mth.lerp(mc.getFrameTime(), player.zo + vec3.getZ(), player.getZ() + vec3.getZ());
                        float[] rotations = RotationUtil.getLerpRotations(mc, player, x, y, z);
                        player.setYRot(rotations[0]);
                        player.setXRot(rotations[1]);
                    }
                }
            }
        });
    }

}
