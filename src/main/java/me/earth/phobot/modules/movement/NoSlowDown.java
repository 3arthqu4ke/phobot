package me.earth.phobot.modules.movement;

import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.event.CancellableEvent;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.module.impl.ModuleImpl;
import me.earth.pingbypass.commons.event.CancellingListener;

public class NoSlowDown extends ModuleImpl {
    public NoSlowDown(PingBypass pingBypass) {
        super(pingBypass, "NoSlowDown", Categories.MOVEMENT, "Prevents slowdown from using items.");
        listen(new CancellingListener<>(UseItemEvent.class));
    }

    public static final class UseItemEvent extends CancellableEvent {}

}
