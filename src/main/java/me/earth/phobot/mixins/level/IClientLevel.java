package me.earth.phobot.mixins.level;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientLevel.class)
public interface IClientLevel {
    @Accessor("entityStorage")
    TransientEntitySectionManager<Entity> getEntityStorage();

    @Accessor("blockStatePredictionHandler")
    BlockStatePredictionHandler getBlockStatePredictionHandler();

    @Accessor("connection")
    ClientPacketListener getConnection();

}
