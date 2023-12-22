package me.earth.phobot.mixins.network;

import me.earth.phobot.ducks.IServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerboundSwingPacket.class)
public abstract class MixinServerboundSwingPacket implements IServerboundSwingPacket {
    @Unique
    private boolean phobot$uncancellable;

    @Override
    public void phobot$setUncancellable(boolean uncancellable) {
        phobot$uncancellable = uncancellable;
    }

    @Override
    public boolean phobot$isUncancellable() {
        return phobot$uncancellable;
    }

}
