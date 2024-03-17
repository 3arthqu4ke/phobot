package me.earth.phobot.event;

import lombok.Data;
import net.minecraft.world.phys.AABB;

@Data
public class CollideEvent {
    private final AABB originalBB;
    private AABB bB;

    public CollideEvent(AABB originalBB) {
        this.originalBB = originalBB;
        this.bB = originalBB;
    }

}
