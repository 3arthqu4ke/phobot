package me.earth.phobot.modules;


import lombok.Getter;
import me.earth.phobot.Phobot;
import me.earth.phobot.services.BlockPlacer;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.api.traits.Nameable;

/**
 * A {@link BlockPlacingModule} which uses packet swapping.
 */
@Getter
public abstract class SwappingBlockPlacingModule extends BlockPlacingModule implements BlockPlacer.ActionListener {
    private final Setting<Integer> swap = number("Swap", -1, -1, 2000, "Delay between swap (packet) switches in ms. Off if -1.");
    private final StopWatch.ForMultipleThreads swapTimer = new StopWatch.ForMultipleThreads();

    public SwappingBlockPlacingModule(Phobot phobot, BlockPlacer blockPlacer, String name, Nameable category, String description, int priority) {
        super(phobot, blockPlacer, name, category, description, priority);
    }

    @Override
    public void onActionExecutedSuccessfully(BlockPlacer.Action action) {
        if (!action.isUsingSetCarried()) {
            swapTimer.reset();
        }
    }

    @Override
    public boolean isUsingSetCarriedItem() {
        int swap = this.swap.getValue();
        return swap == -1 || swap != 0 && !swapTimer.passed(swap);
    }

}

