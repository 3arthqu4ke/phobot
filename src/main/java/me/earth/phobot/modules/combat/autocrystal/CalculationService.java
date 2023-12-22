package me.earth.phobot.modules.combat.autocrystal;

import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.util.time.StopWatch;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class CalculationService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicReference<Object> current = new AtomicReference<>();
    private final StopWatch.ForMultipleThreads timer = new StopWatch.ForMultipleThreads();
    private final CrystalPlacingModule module;

    public void calculate(ClientLevel level, LocalPlayer player, int delay, boolean multiThread) {
        calculate(level, player, delay, multiThread, calculation -> {});
    }

    @Synchronized
    public void calculate(ClientLevel level, LocalPlayer player, int delay, boolean multiThread, Consumer<Calculation> configuration) {
        if (current.get() == null && timer.passed(delay)) {
            Calculation calculation = module.createCalculation(multiThread ? module.getPhobot().getThreadSafeLevelService().getLevel() : level, player);
            configuration.accept(calculation);
            Runnable runnable = () -> {
                try {
                    calculation.run();
                } catch (Exception e) {
                    log.error("Error during calculation", e);
                } finally {
                    current.set(null);
                }
            };

            current.set(runnable);
            timer.reset();
            run(runnable, multiThread);
        }
    }

    private void run(Runnable runnable, boolean multiThread) {
        if (multiThread) {
            executor.submit(runnable);
        } else {
            runnable.run();
        }
    }

}
