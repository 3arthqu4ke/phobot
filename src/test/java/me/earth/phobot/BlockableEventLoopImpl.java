package me.earth.phobot;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
public class BlockableEventLoopImpl extends ReentrantBlockableEventLoop<Runnable> {
    private Thread runningThread = Thread.currentThread();

    public BlockableEventLoopImpl() {
        super("TestReentrantBlockableEventLoop");
    }

    @Override
    protected @NotNull Runnable wrapRunnable(Runnable runnable) {
        return runnable;
    }

    @Override
    protected boolean shouldRun(Runnable runnable) {
        return true;
    }

    @Override
    public void runAllTasks() { // make public
        super.runAllTasks();
    }

}
