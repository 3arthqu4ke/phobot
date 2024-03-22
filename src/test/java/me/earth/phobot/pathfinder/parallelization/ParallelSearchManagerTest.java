package me.earth.phobot.pathfinder.parallelization;

import me.earth.phobot.pathfinder.util.CancellableFuture;
import me.earth.phobot.pathfinder.util.Cancellation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ParallelSearchManagerTest {
    @Test
    public void testParallelSearchManager() {
        ParallelSearchManager searchManager = new ParallelSearchManager();
        var future = searchManager.<String>applyForPathSearch(() -> 0, search -> search.addFuture("test", new CancellableFuture<>(new Cancellation())));
        assertTrue(searchManager.isSearching());
        assertTrue(searchManager.isAtLeastEquallyImportantTo(() -> 0));
        assertTrue(searchManager.isAtLeastEquallyImportantTo(() -> -1));
        assertFalse(searchManager.isAtLeastEquallyImportantTo(() -> 1));
        assertTrue(searchManager.isMoreImportantSearchThan(() -> -1));

        AtomicBoolean called = new AtomicBoolean();
        future = searchManager.applyForPathSearch(() -> -1, search -> {
            search.addFuture("test2", new CancellableFuture<>(new Cancellation()));
            called.set(true);
        });
        assertNull(future);

        HasPriority provider = () -> 1;
        future = searchManager.applyForPathSearch(provider, search -> {
            search.addFuture("test3", new CancellableFuture<>(new Cancellation()));
            called.set(true);
        });

        assertNotNull(future);
        assertTrue(called.get());

        assertTrue(searchManager.isSearching());
        searchManager.cancel(() -> 0);
        assertTrue(searchManager.isSearching());
        searchManager.cancel(provider);
        assertFalse(searchManager.isSearching());
    }

}
