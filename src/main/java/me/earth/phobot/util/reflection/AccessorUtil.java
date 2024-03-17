package me.earth.phobot.util.reflection;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import me.earth.phobot.mixins.entity.IEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * TODO: just apply mixins in tests?
 * TODO: generic solution? Annotations are RUNTIME retention! Might be hard to find the correct method though?
 * The idea of this is that it gives you reflection based fallbacks for {@link Accessor} and {@link Invoker}.
 * This can be useful if Mixins have not been applied in the context you want to use them, like unit tests.
 */
@UtilityClass
public class AccessorUtil {
    public static IEntity getAsIEntity(Entity entity) {
        if (entity instanceof IEntity accessor) {
            return accessor;
        }

        return new IEntity() {
            @Override
            @SneakyThrows
            public boolean getOnGroundNoBlocks() {
                return ReflectionUtil.getFieldValue(entity, Entity.class, "onGroundNoBlocks");
            }

            @Override
            @SneakyThrows
            public void setOnGroundNoBlocks(boolean onGroundNoBlocks) {
                ReflectionUtil.setFieldValue(entity, onGroundNoBlocks, Entity.class, "onGroundNoBlocks");
            }

            @Override
            public Vec3 getStuckSpeedMultiplier() {
                return ReflectionUtil.getFieldValue(entity, Entity.class, "stuckSpeedMultiplier");
            }

            @Override
            public void setStuckSpeedMultiplier(Vec3 stuckSpeedMultiplier) {
                ReflectionUtil.setFieldValue(entity, stuckSpeedMultiplier, Entity.class, "stuckSpeedMultiplier");
            }
        };
    }

}
