package me.earth.phobot.modules.render;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.earth.phobot.Phobot;
import me.earth.phobot.modules.PhobotModule;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.module.impl.Categories;

public class Fullbright extends PhobotModule {
    public Fullbright(Phobot phobot) {
        super(phobot, "Fullbright", Categories.RENDER, "Makes the game as bright as possible");
        listen(new Listener<LightTextureEvent>() {
            @Override
            public void onEvent(LightTextureEvent event) {
                event.setColor(0xFFFFFFFF);
            }
        });
    }

    @Data
    @AllArgsConstructor
    public static class LightTextureEvent {
        private int color;
    }

}
