package me.earth.phobot.mixins.level;

import me.earth.phobot.event.UnloadChunkEvent;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientChunkCache.class)
public abstract class MixinClientChunkCache {
    @Inject(
        method = "drop",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientChunkCache$Storage;replace(ILnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/chunk/LevelChunk;)Lnet/minecraft/world/level/chunk/LevelChunk;",
            shift = At.Shift.AFTER),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void dropHook(ChunkPos chunkPos, CallbackInfo ci, int index, LevelChunk levelChunk) {
        PingBypassApi.getEventBus().post(new UnloadChunkEvent(levelChunk));
    }

}
