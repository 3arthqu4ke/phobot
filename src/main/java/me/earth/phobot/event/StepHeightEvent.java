package me.earth.phobot.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.earth.pingbypass.api.event.event.CancellableEvent;
import net.minecraft.client.player.LocalPlayer;

@Getter
@AllArgsConstructor
public class StepHeightEvent extends CancellableEvent {
    private final LocalPlayer player;
    private float height;

    public void setHeight(float height) {
        setCancelled(true);
        this.height = height;
    }

}
