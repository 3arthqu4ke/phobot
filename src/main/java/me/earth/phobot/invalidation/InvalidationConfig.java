package me.earth.phobot.invalidation;

import net.minecraft.client.Minecraft;

import java.util.concurrent.ExecutorService;

public interface InvalidationConfig {
    Minecraft getMinecraft();

    ExecutorService getExecutor();

    boolean isAsync();

}
