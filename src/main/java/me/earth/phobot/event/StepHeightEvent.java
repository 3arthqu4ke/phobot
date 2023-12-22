package me.earth.phobot.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.earth.pingbypass.api.event.event.CancellableEvent;
import net.minecraft.world.entity.LivingEntity;

@Getter
@AllArgsConstructor
public class StepHeightEvent extends CancellableEvent {
    private final LivingEntity entity;
    private float height;

    public void setHeight(float height) {
        setCancelled(true);
        this.height = height;
    }

}
