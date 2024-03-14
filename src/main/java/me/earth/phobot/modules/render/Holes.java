package me.earth.phobot.modules.render;

import lombok.Getter;
import me.earth.phobot.invalidation.ConfigWithMinMaxHeight;
import me.earth.phobot.modules.PhobotNameSpacedModule;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.concurrent.ExecutorService;

public class Holes extends PhobotNameSpacedModule implements ConfigWithMinMaxHeight {
    @Getter
    private final Setting<Boolean> calcAsync =
            bool("Async", true, "Calculates Chunks asynchronously.");
    private final Setting<Integer> maxHeight =
            number("MaxHeight", 60, -64, 320, "Calculates holes in a chunk up to this height.");
    private final Setting<Integer> minHeight =
            number("MinHeight", 0, -64, 320, "Calculates holes in a chunk from height.");
    @Getter
    private final Setting<Boolean> render = bool("Render", false, "If you want to render holes.");
    @Getter
    private final Setting<Double> range = precise("Range", 6.0, 0.0, 100.0, "Only renders holes within this range.");
    @Getter
    private final Setting<Integer> holes = number("Holes", 10, 1, 100, "How many holes to render.");
    @Getter
    private final ExecutorService executor;

    public Holes(PingBypass pingBypass, ExecutorService executor) {
        super(pingBypass, "Holes", Categories.RENDER, "Configures the HoleManager.");
        this.executor = executor;
    }

    @Override
    public Minecraft getMinecraft() {
        return getPingBypass().getMinecraft();
    }

    @Override
    public boolean isAsync() {
        return calcAsync.getValue();
    }

    @Override
    public int getMaxHeight() {
        return maxHeight.getValue();
    }

    @Override
    public int getMinHeight() {
        return minHeight.getValue();
    }

}
