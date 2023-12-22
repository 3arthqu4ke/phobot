package me.earth.phobot.mixins.network;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface IMultiPlayerGameMode {
    @Invoker("ensureHasSentCarriedItem")
    void invokeEnsureHasSentCarriedItem();

}
