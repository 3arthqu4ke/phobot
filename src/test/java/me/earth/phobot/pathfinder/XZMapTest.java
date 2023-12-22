package me.earth.phobot.pathfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.earth.phobot.util.collections.XZMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XZMapTest {
    @Test
    public void testXZMap() {
        XZMap<Integer> map = new XZMap<>(new Long2ObjectOpenHashMap<>());

        BlockPos pos1 = new BlockPos(Integer.MAX_VALUE, 100, Integer.MAX_VALUE);
        Vec3i vec3i2 = new Vec3i(Integer.MIN_VALUE, 100, Integer.MIN_VALUE);
        BlockPos pos3 = new BlockPos(1, 100, 0);
        BlockPos pos4 = BlockPos.ZERO;

        map.put(pos1, 1);
        map.put(vec3i2, 2);
        map.put(pos3, 3);
        map.put(pos4, 4);
        assertEquals(4, map.put(new BlockPos(0, 5, 0), 4));

        assertEquals(1, map.get(pos1));
        assertEquals(2, map.get(vec3i2));
        assertEquals(3, map.get(pos3));
        assertEquals(4, map.remove(pos4));
    }

}
