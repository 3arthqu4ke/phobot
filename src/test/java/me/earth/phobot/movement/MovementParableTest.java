package me.earth.phobot.movement;

import lombok.SneakyThrows;
import me.earth.phobot.TestUtil;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManagerTest;
import me.earth.phobot.util.player.MovementPlayer;
import me.earth.phobot.util.world.BlockStateLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MovementParableTest {
    @Test
    @SneakyThrows
    public void testMovementParable() {
        try (ClientLevel clientLevel = TestUtil.createClientLevel()) {
            BlockStateLevel.Delegating level = NavigationMeshManagerTest.setupLevel(clientLevel);
            MovementPlayer player = new MovementPlayer(level);
            BunnyHop movement = new BunnyHopCC();
            player.setMovement(movement);
            player.setPos(Vec3.ZERO);
            MovementParable parable = MovementParable.calculate(player, level);
            assertEquals("MovementParable(horizontalValues=[0.287, 0.574, 0.861, 1.148, 1.4349999999999998, 1.7219999999999998, 2.009, 2.296, 2.5829999999999997," +
                             " 2.8699999999999997, 3.1569999999999996, 3.4439999999999995, 3.7309999999999994, 4.018, 4.305, 4.592, 4.879]," +
                             " verticalValues=[0.4000000059604645, 0.7198720180039406, 0.9596480858042122, 1.1193602462158032, 1.1990405232791046, 1.1987271947950418, " +
                             "1.1184459901150652, 0.958228894868915, 0.718107881892688, 0.39811491123395437, -0.0017180698431268904, -0.4813591268526949, " +
                             "-1.0407763380820758, -1.6799377945866738, -2.398811600184864, -3.197365871452887, -4.075568737719749], maxHorizontal=4.879," +
                             " maxVertical=1.1990405232791046, size=17)", parable.toString());
            assertEquals(parable.getHorizontalValues()[0], movement.getBaseSpeed());
            assertEquals(parable.getVerticalValues()[0], movement.getJumpY());
            assertFalse(parable.canReach(player.position(), 0.0, 10_000, 0.0)); // cant jump that far up
            assertTrue(parable.canReach(player.position(), 0.0, -10_000, 0.0)); // can jump down tho
            assertTrue(parable.canReach(player.position(), 1.0, 1.0, 0.0));
            assertTrue(parable.canReach(player.position(), 2.0, 1.0, 0.0));
            assertFalse(parable.canReach(player.position(), 10.0, 1.0, 0.0));
            assertFalse(parable.canReach(264.51939469454373, 72.0, 353.49056240770767, 100.0, 72.0, 353.0));

            player.setPos(10.5, 1.0, 10.5);
            assertTrue(parable.canReach(player.position(), 8.5, 1, 8.5));

            assertFalse(parable.isOnParable(0.0, 0.0, 0.0, 0.287, 0.0, 0.0, 0.5, 0.1));
            assertTrue(parable.isOnParable(0.0, 0.0, 0.0, 0.287, 0.0, 0.0, 0.5, 0.5));
            for (int i = 0; i < parable.getSize(); i++) {
                assertTrue(parable.canReach(0.0, 0.0, 0.0, 0.0, parable.getVerticalValues()[i], parable.getHorizontalValues()[i]));
                assertTrue(parable.isOnParable(0.0, 0.0, 0.0, 0.0, parable.getVerticalValues()[i], parable.getHorizontalValues()[i], 0.0, 0.0));
            }

            // TODO: more tests for isOnParable
        }
    }

}
