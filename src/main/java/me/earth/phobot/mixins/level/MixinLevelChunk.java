package me.earth.phobot.mixins.level;

import me.earth.phobot.event.BlockStateChangeEvent;
import me.earth.phobot.invalidation.ChunkWorker;
import me.earth.phobot.invalidation.ChunkWorkerProvider;
import me.earth.pingbypass.PingBypassApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk implements ChunkWorkerProvider {
    @Shadow
    @Final
    Level level;

    @Unique
    private final ChunkWorker phobot$holeChunkWorker = new ChunkWorker();

    @Unique
    private final ChunkWorker phobot$graphChunkWorker = new ChunkWorker();

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void setBlockStateHook(BlockPos pos, BlockState state, boolean bl, CallbackInfoReturnable<BlockState> cir) {
        if (this.level.isClientSide) {
            PingBypassApi.getEventBus().post(new BlockStateChangeEvent(pos, state, LevelChunk.class.cast(this)));
        }
    }

    @Override
    public ChunkWorker phobot$getHoleChunkWorker() {
        return phobot$holeChunkWorker;
    }

    @Override
    public ChunkWorker phobot$getGraphChunkWorker() {
        return phobot$graphChunkWorker;
    }

}
