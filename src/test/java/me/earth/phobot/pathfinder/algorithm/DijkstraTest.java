package me.earth.phobot.pathfinder.algorithm;

import me.earth.phobot.pathfinder.blocks.BlockNode;
import me.earth.phobot.pathfinder.blocks.BlockNodePoolBuilder;
import me.earth.phobot.pathfinder.util.Cancellation;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DijkstraTest {
    @Test
    public void testDijkstra() {
        var builder = new BlockNodePoolBuilder();
        var nodes = builder.build(10, BlockNode::new);
        var start = builder.getCenter();
        BlockNode goal = null;
        for (var node : nodes) {
            node.setToOffsetFromCenter(BlockPos.ZERO);
            if (node.getCurrent().equals(new BlockPos(3, 2, 2))) {
                goal = node;
            }
        }

        assertNotNull(goal);
        Cancellation cancellation = new Cancellation();
        Dijkstra<BlockNode> aStar = new Dijkstra<>(start, goal);
        var result = aStar.run(cancellation);
        assertNotNull(result);
        assertEquals(new BlockNode(new BlockPos(3, 2, 2)), result.get(0));
        assertEquals(new BlockNode(new BlockPos(3, 1, 2)), result.get(1));
        assertEquals(new BlockNode(new BlockPos(3, 0, 2)), result.get(2));
        assertEquals(new BlockNode(new BlockPos(3, 0, 1)), result.get(3));
        assertEquals(new BlockNode(new BlockPos(3, 0, 0)), result.get(4));
        assertEquals(new BlockNode(new BlockPos(2, 0, 0)), result.get(5));
        assertEquals(new BlockNode(new BlockPos(1, 0, 0)), result.get(6));
        assertEquals(new BlockNode(new BlockPos(0, 0, 0)), result.get(7));
        assertFalse(cancellation.isCancelled());
        cancellation.setCancelled(true);
        assertTrue(cancellation.isCancelled());
        aStar = new Dijkstra<>(start, goal);
        result = aStar.run(cancellation);
        assertNull(result);
    }

}
