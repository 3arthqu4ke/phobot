package me.earth.phobot.modules.movement;

import me.earth.phobot.event.MoveEvent;
import me.earth.phobot.modules.PhobotNameSpacedModule;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.module.impl.Categories;
import net.minecraft.world.phys.Vec3;

public class Stop extends PhobotNameSpacedModule {
    public Stop(PingBypass pingBypass) {
        super(pingBypass, "Stop", Categories.MOVEMENT, "Stops all movement.");
        listen(new Listener<MoveEvent>(-10_000) {
            @Override
            public void onEvent(MoveEvent event) {
                event.setVec(Vec3.ZERO);
            }
        });
    }

}
