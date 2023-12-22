package me.earth.phobot.mixins;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VoxelShape.class)
public interface IVoxelShape {
    @Accessor("shape")
    DiscreteVoxelShape getShape();

    @Invoker("findIndex")
    int invokeFindIndex(Direction.Axis axis, double limit);

}
