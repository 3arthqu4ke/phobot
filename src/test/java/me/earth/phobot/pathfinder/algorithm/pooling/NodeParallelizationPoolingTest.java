package me.earth.phobot.pathfinder.algorithm.pooling;

import lombok.SneakyThrows;
import me.earth.phobot.pathfinder.util.Cancellation;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class NodeParallelizationPoolingTest {
    @Test
    @SneakyThrows
    public void testNodeParallelizationPooling() {
        NodeParallelizationPooling nodeParallelizationPooling = new NodeParallelizationPooling(4);
        Set<NodeParallelizationPooling.PoolReference> references = Collections.newSetFromMap(new ConcurrentHashMap<>());
        for (int i = 0; i < 4; i++) {
            references.add(nodeParallelizationPooling.requestIndex(new Cancellation()));
        }

        assertTrue(nodeParallelizationPooling.openIndices.isEmpty());
        assertEquals(4, nodeParallelizationPooling.indicesInUse.size());

        references.stream().filter(ref -> ref.getIndex() == 2).forEach(NodeParallelizationPooling.PoolReference::close);
        assertFalse(nodeParallelizationPooling.openIndices.isEmpty());
        assertTrue(nodeParallelizationPooling.openIndices.contains(2));
        assertEquals(3, nodeParallelizationPooling.indicesInUse.size());

        try (NodeParallelizationPooling.PoolReference ref = nodeParallelizationPooling.requestIndex(new Cancellation())) {
            assert ref != null;
            assertEquals(2, ref.getIndex());
            assertEquals(5, ref.getAlgorithmId());
        }

        Cancellation cancellation = new Cancellation();
        cancellation.setCancelled(true);
        try (NodeParallelizationPooling.PoolReference ref = nodeParallelizationPooling.requestIndex(cancellation)) {
            assertNull(ref);
        }
    }

}
