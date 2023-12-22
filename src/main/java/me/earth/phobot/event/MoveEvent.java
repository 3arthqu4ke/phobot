package me.earth.phobot.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.world.phys.Vec3;

@Data
@AllArgsConstructor
public class MoveEvent {
    private Vec3 vec;

}
