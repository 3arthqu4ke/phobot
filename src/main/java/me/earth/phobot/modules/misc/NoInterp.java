package me.earth.phobot.modules.misc;

import me.earth.phobot.event.LerpToEvent;
import me.earth.phobot.modules.PhobotNameSpacedModule;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.module.impl.Categories;

public class NoInterp extends PhobotNameSpacedModule {
    public NoInterp(PingBypass pingBypass) {
        super(pingBypass, "NoInterp", Categories.MISC, "Removes client side interpolation: Entities will always assume their server position.");
        listen(new Listener<LerpToEvent>(Integer.MIN_VALUE) {
            @Override
            public void onEvent(LerpToEvent event) {
                event.getEntity().setPos(event.getX(), event.getY(), event.getZ());
                event.getEntity().setXRot(event.getXRot());
                event.getEntity().setYRot(event.getYRot());
            }
        });
    }

}
