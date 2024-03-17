package me.earth.phobot.pathfinder.parallelization;

import lombok.Synchronized;
import lombok.experimental.StandardException;
import me.earth.phobot.pathfinder.util.CancellableFuture;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

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
     * @param configuration configures the MultiPathSearch to add.
     */
    @Synchronized("lock")
    public <T> @Nullable CancellableFuture<ParallelPathSearch.Result<T>> applyForPathSearch(HasPriority provider, Consumer<ParallelPathSearch<T>> configuration) {
        if (isMoreImportantSearchThan(provider)) {
            return null;
        }

        Search<?> previousSearch = this.search;
        if (previousSearch != null) {
            previousSearch.search.getFuture().completeExceptionally(
                    new CancelledDueToBetterProviderException("Better provider " + provider + " has cancelled your (" + previousSearch.provider + ") search."));
            previousSearch.search.setCancelled(true);
        }

        ParallelPathSearch<T> multiPathSearch = new ParallelPathSearch<>();
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

        return multiPathSearch.getFuture();
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

    public record Search<T>(ParallelPathSearch<T> search, HasPriority provider) { }

    @StandardException
    public static class CancelledDueToBetterProviderException extends Exception { }

}
