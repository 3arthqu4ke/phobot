package me.earth.phobot.modules.movement;

import me.earth.phobot.event.MoveEvent;
import me.earth.phobot.modules.PhobotNameSpacedModule;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.loop.TickEvent;
import me.earth.pingbypass.api.input.Bind;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.world.phys.Vec3;

public class Stop extends PhobotNameSpacedModule {
    private final Setting<Boolean> hold = bool("Hold", false, "Disables this module when u let go of the bind.");

    public Stop(PingBypass pingBypass) {
        super(pingBypass, "Stop", Categories.MOVEMENT, "Stops all movement.");
        Setting<Bind> bind = getSetting("Bind", Bind.class).orElseThrow();
        listen(new Listener<MoveEvent>(-10_000) {
            @Override
            public void onEvent(MoveEvent event) {
                event.setVec(Vec3.ZERO);
            }
        });

        listen(new Listener<TickEvent>() {
            @Override
            public void onEvent(TickEvent tickEvent) {
                if (hold.getValue() && !getPingBypass().getKeyBoardAndMouse().isPressed(bind.getValue())) {
                    disable();
                }
            }
        });
    }

}
