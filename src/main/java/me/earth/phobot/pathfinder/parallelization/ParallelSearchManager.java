package me.earth.phobot.pathfinder.parallelization;

import lombok.Synchronized;
import lombok.experimental.StandardException;
import me.earth.phobot.pathfinder.util.CancellableFuture;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages {@link ParallelPathSearch}es by multiple providers.
 * At a time only one {@link ParallelPathSearch} can be executed.
 * If a provider with a higher priority is already doing a MultiPathSearch, providers with a lower priority cant register their searches.
 */
public class ParallelSearchManager {
    private final Object lock = new Object();
    public volatile Search<?> search;

    /**
     * Starts a new {@link ParallelPathSearch} if there is none in progress yet.
     * In case confusion about java generics exists, you can define the type of the search like this:
     * <p>{@code multiPathSearchManager.<Hole>applyForPathSearch(this, holePathSearch -> ... }
     *
     * @param provider the provider for this search.
     * @param configuration configures the MultiPathSearch to add.
     */
    public <T> @Nullable CancellableFuture<CancellableSearch.Result<T>> applyForPathSearch(HasPriority provider, Consumer<ParallelPathSearch<T>> configuration) {
        return applyForPathSearch(provider, ParallelPathSearch::new, configuration);
    }

    /**
     * @return {@code true} if a {@link ParallelPathSearch} is in progress for this holder.
     */
    public boolean isSearching() {
        return search != null;
    }

    /**
     * @param provider the provider to check.
     * @return {@code true} if a provider with a higher or the same priority has already registered a MultiPathSearch.
     */
    public boolean isAtLeastEquallyImportantTo(HasPriority provider) {
        Search<?> pathSearch = this.search;
        return pathSearch != null && (pathSearch.provider.getPriority() == provider.getPriority() || pathSearch.provider.isMoreImportantThan(provider));
    }

    /**
     * @param provider the provider to check.
     * @return {@code true} if a provider with a higher priority has already registered a MultiPathSearch.
     */
    public boolean isMoreImportantSearchThan(HasPriority provider) {
        Search<?> pathSearch = this.search;
        return pathSearch != null && pathSearch.provider.isMoreImportantThan(provider);
    }

    /**
     * Cancels the currently ongoing {@link ParallelPathSearch} if it exists.
     * @see #cancel(HasPriority)
     */
    @Synchronized("lock")
    public void cancel() {
        cancel(null);
    }

    /**
     * Cancels the currently ongoing {@link ParallelPathSearch} if it exists.
     * @param provider if not {@code null} the search will only be cancelled if it was issued by this provider.
     */
    @Synchronized("lock")
    public void cancel(@Nullable HasPriority provider) {
        Search<?> pathSearch = this.search;
        if (pathSearch != null && (provider == null || pathSearch.provider.equals(provider))) {
            pathSearch.search.setCancelled(true);
            this.search = null;
        }
    }

    /**
     * Starts a new {@link CancellableSearch} if there is none in progress yet.
     * In case confusion about java generics exists, you can define the type of the search like this:
     * <p>{@code multiPathSearchManager.<Hole, (type of search)<Hole>>applyForPathSearch(this, factory, holePathSearch -> ... }
     *
     * @param provider the provider for this search.
     * @param searchFactory the factory supplying the {@link CancellableSearch}
     * @param configuration configures the MultiPathSearch to add.
     */
    @Synchronized("lock")
    public <T, S extends CancellableSearch<T>> @Nullable CancellableFuture<CancellableSearch.Result<T>> applyForPathSearch(HasPriority provider,
                                                                                                                           Supplier<S> searchFactory,
                                                                                                                           Consumer<S> configuration) {
        if (isMoreImportantSearchThan(provider)) {
            return null;
        }

        Search<?> previousSearch = this.search;
        if (previousSearch != null) {
            previousSearch.search.getFuture().completeExceptionally(
                    new CancelledDueToBetterProviderException("Better provider " + provider + " has cancelled your (" + previousSearch.provider + ") search."));
            previousSearch.search.setCancelled(true);
        }

        S multiPathSearch = searchFactory.get();
        synchronized (multiPathSearch.getFuture()) { // synchronize in case a future completes before this is even done, in that case some futures might run for longer
            Search<T> search = new Search<>(multiPathSearch, provider);
            configuration.accept(multiPathSearch);
            multiPathSearch.allFuturesAdded();
            this.search = search;
            multiPathSearch.getFuture().whenComplete((r,t) -> {
                synchronized (lock) {
                    if (this.search == search) {
                        this.search = null;
                    }
                }
            });
        }

        return multiPathSearch.getFuture();
    }

    public record Search<T>(CancellableSearch<T> search, HasPriority provider) { }

    @StandardException
    public static class CancelledDueToBetterProviderException extends Exception { }

}
