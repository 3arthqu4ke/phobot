package me.earth.phobot.modules.movement;

import me.earth.phobot.Phobot;
import me.earth.phobot.event.StepHeightEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.loop.TickEvent;
import me.earth.pingbypass.api.input.Bind;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;

public class Step extends PhobotModule {
    private final Setting<Boolean> hold = bool("Hold", false, "Disables this module when u let go of the bind.");

    public Step(Phobot phobot) {
        super(phobot, "Step", Categories.MOVEMENT, "Allows you to step up blocks.");
        Setting<Bind> bind = getSetting("Bind", Bind.class).orElseThrow();
        listen(new Listener<StepHeightEvent>() {
            @Override
            public void onEvent(StepHeightEvent event) {
                if (phobot.getMovementService().getMovement().canStep(event.getPlayer())) {
                    event.setHeight(phobot.getMovementService().getMovement().getStepHeight());
                }
            }
        });
        // TODO: use phobot KeyBoardEvents?
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
