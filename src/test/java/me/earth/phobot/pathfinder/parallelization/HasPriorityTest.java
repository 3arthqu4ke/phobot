package me.earth.phobot.pathfinder.parallelization;

import org.junit.jupiter.api.Test;

import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

public class HasPriorityTest {
    @Test
    public void testPriorityComparison() {
        HasPriority highPriority = () -> 10;
        HasPriority mediumPriority = () -> 5;
        HasPriority lowPriority = () -> 1;

        assertFalse(highPriority.isMoreImportantThan(highPriority));
        assertTrue(highPriority.isMoreImportantThan(mediumPriority));
        assertTrue(highPriority.isMoreImportantThan(lowPriority));

        assertFalse(mediumPriority.isMoreImportantThan(highPriority));
        assertFalse(mediumPriority.isMoreImportantThan(mediumPriority));
        assertTrue(mediumPriority.isMoreImportantThan(lowPriority));

        assertFalse(lowPriority.isMoreImportantThan(highPriority));
        assertFalse(lowPriority.isMoreImportantThan(mediumPriority));
        assertFalse(lowPriority.isMoreImportantThan(lowPriority));

        PriorityQueue<HasPriority> queue = new PriorityQueue<>();
        queue.add(mediumPriority);
        queue.add(lowPriority);
        queue.add(highPriority);

        assertEquals(highPriority, queue.poll());
        assertEquals(mediumPriority, queue.poll());
        assertEquals(lowPriority, queue.poll());
    }

}
