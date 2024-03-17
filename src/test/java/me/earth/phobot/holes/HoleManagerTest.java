package me.earth.phobot.holes;

import lombok.SneakyThrows;
import me.earth.phobot.BlockableEventLoopImpl;
import me.earth.phobot.Phobot;
import me.earth.phobot.TestPhobot;
import me.earth.phobot.TestUtil;
import me.earth.phobot.invalidation.ChunkWorker;
import me.earth.phobot.modules.render.Holes;
import me.earth.phobot.util.mutables.MutPos;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class HoleManagerTest {
    @Test
    @SneakyThrows
    public void testAllHoleTypes() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            BlockStateLevel.Delegating level = TestUtil.setupLevelFromJson(clientLevel, "worlds/AllHoles.json");
            HoleManager manager = setupHoleManager();
            var holeTask = new HoleTask(manager.getMap(), new BlockableEventLoopImpl(), level, new MutPos(), manager, new ChunkWorker(), -5, 5, -1, 3, -5, 5);
            holeTask.run();

            Set<Hole> holes = new HashSet<>();

            Hole bedrock1x1 = manager.getMap().get(new BlockPos(-1, 2, 2));
            assertNotNull(bedrock1x1);
            assertTrue(bedrock1x1.is1x1());
            assertTrue(bedrock1x1.isSafe());
            assertEquals(new Vec3(-0.5, 2.0, 2.5), bedrock1x1.getCenter());
            holes.add(bedrock1x1);

            Set<BlockPos> airParts = bedrock1x1.getAirParts();
            assertEquals(1, airParts.size());
            assertTrue(airParts.contains(new BlockPos(-1, 2, 2)));

            Hole bedrock2x1 = manager.getMap().get(new BlockPos(-2, 2, 0));
            assertNotNull(bedrock2x1);
            assertTrue(bedrock2x1.is2x1());
            // assertTrue(bedrock2x1.isSafe()); TODO: track safety for 2x1s and 2x2s
            assertEquals(new Vec3(-1.5, 2.0, 1.0), bedrock2x1.getCenter());
            holes.add(bedrock2x1);

            airParts = bedrock2x1.getAirParts();
            assertEquals(2, airParts.size());
            assertTrue(airParts.contains(new BlockPos(-2, 2, 0)));
            assertTrue(airParts.contains(new BlockPos(-2, 2, 1)));

            Hole bedrock2x12 = manager.getMap().get(new BlockPos(3, 2, 3));
            assertNotNull(bedrock2x12);
            assertTrue(bedrock2x12.is2x1());
            // assertTrue(bedrock2x12.isSafe());  TODO: track safety for 2x1s and 2x2s
            assertEquals(new Vec3(4.0, 2.0, 3.5), bedrock2x12.getCenter());
            holes.add(bedrock2x12);

            airParts = bedrock2x12.getAirParts();
            assertEquals(2, airParts.size());
            assertTrue(airParts.contains(new BlockPos(3, 2, 3)));
            assertTrue(airParts.contains(new BlockPos(4, 2, 3)));

            Hole bedrock2x2 = manager.getMap().get(new BlockPos(0, 2, -3));
            assertNotNull(bedrock2x2);
            assertTrue(bedrock2x2.is2x2());
            // assertTrue(bedrock2x2.isSafe()); TODO: track safety for 2x1s and 2x2s
            assertEquals(new Vec3(1.0, 2.0, -2.0), bedrock2x2.getCenter());
            holes.add(bedrock2x2);

            airParts = bedrock2x2.getAirParts();
            assertEquals(4, airParts.size());
            assertTrue(airParts.contains(new BlockPos(1, 2, -2)));
            assertTrue(airParts.contains(new BlockPos(0, 2, -2)));
            assertTrue(airParts.contains(new BlockPos(1, 2, -3)));
            assertTrue(airParts.contains(new BlockPos(0, 2, -3)));


            Hole obby1x1 = manager.getMap().get(new BlockPos(2, 1, 2));
            assertNotNull(obby1x1);
            assertTrue(obby1x1.is1x1());
            assertFalse(obby1x1.isSafe());
            assertEquals(new Vec3(2.5, 1.0, 2.5), obby1x1.getCenter());
            holes.add(obby1x1);

            Hole obby2x1 = manager.getMap().get(new BlockPos(3, 1, 0));
            assertNotNull(obby2x1);
            assertTrue(obby2x1.is2x1());
            assertFalse(obby2x1.isSafe());
            assertEquals(new Vec3(3.5, 1.0, 1.0), obby2x1.getCenter());
            holes.add(obby2x1);

            Hole obby2x12 = manager.getMap().get(new BlockPos(0, 1, 3));
            assertNotNull(obby2x12);
            assertTrue(obby2x12.is2x1());
            assertFalse(obby2x12.isSafe());
            assertEquals(new Vec3(1.0, 1.0, 3.5), obby2x12.getCenter());
            holes.add(obby2x12);

            Hole obby2x2 = manager.getMap().get(new BlockPos(0, 1, 0));
            assertNotNull(obby2x2);
            assertTrue(obby2x2.is2x2());
            assertFalse(obby2x2.isSafe());
            assertEquals(new Vec3(1.0, 1.0, 1.0), obby2x2.getCenter());
            holes.add(obby2x2);

            assertEquals(8, holes.size());
        }
    }

    public static HoleManager setupHoleManager() {
        Phobot phobot = TestPhobot.createNewTestPhobot();
        Holes holes = new Holes(phobot.getPingBypass(), phobot.getExecutorService());
        holes.getCalcAsync().setValue(false);
        return new HoleManager(holes, new HashMap<>());
    }

}
