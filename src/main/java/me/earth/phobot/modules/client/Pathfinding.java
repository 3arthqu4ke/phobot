package me.earth.phobot.modules.client;

import lombok.Getter;
import me.earth.phobot.invalidation.ConfigWithMinMaxHeight;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.module.impl.ModuleImpl;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.api.setting.impl.types.BoolBuilder;
import net.minecraft.client.Minecraft;

import java.util.concurrent.ExecutorService;

public class Pathfinding extends ModuleImpl implements ConfigWithMinMaxHeight {
    public static final Setting<Boolean> DEBUG = new BoolBuilder().withName("Debug").withDescription("Debugs").withValue(false).build();

    @Getter
    private final Setting<Boolean> calcAsync =
            bool("Async", true, "Calculates Chunks asynchronously.");
    private final Setting<Integer> maxHeight =
            number("MaxHeight", 140, -64, 320, "Calculates pathfinding in a chunk up to this height.");
    private final Setting<Integer> minHeight =
            number("MinHeight", 0, -64, 320, "Calculates pathfinding in a chunk from height.");
    @Getter
    private final Setting<Boolean> render =
            bool("Render", false, "Render the pathfinding graph for debugging.");
    private final ExecutorService executor;

    public Pathfinding(PingBypass pingBypass, ExecutorService executor) {
        super(pingBypass, "Pathfinding", Categories.CLIENT, "Allows you to configure the pathfinding of Phobot.");
        this.executor = executor;
        register(DEBUG);
    }

    @Override
    public int getMaxHeight() {
        return maxHeight.getValue();
    }

    @Override
    public int getMinHeight() {
        return minHeight.getValue();
    }

    @Override
    public Minecraft getMinecraft() {
        return mc;
    }

    @Override
    public ExecutorService getExecutor() {
        return executor;
    }

    @Override
    public boolean isAsync() {
        return calcAsync.getValue();
    }

}
