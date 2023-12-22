package me.earth.phobot.util.math;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MathUtilTest {
    @Test
    public void testGetAngle() {
        Vec3 x = new Vec3(1.0, 0.0, 0.0);
        Vec3 z = new Vec3(0.0, 0.0, 1.0);
        assertEquals(90.0, MathUtil.getAngle(x, z));
        assertEquals(180.0, MathUtil.getAngle(x, new Vec3(-1.0, 0.0, 0.0)));
        assertEquals(0.0, MathUtil.getAngle(x, x));
    }

}
