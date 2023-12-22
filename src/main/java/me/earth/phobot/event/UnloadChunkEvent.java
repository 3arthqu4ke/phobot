package me.earth.phobot.event;

import net.minecraft.world.level.chunk.LevelChunk;

public record UnloadChunkEvent(LevelChunk chunk) { }
