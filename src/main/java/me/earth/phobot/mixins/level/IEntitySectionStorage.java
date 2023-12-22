package me.earth.phobot.mixins.level;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntitySectionStorage.class)
public interface IEntitySectionStorage<T extends EntityAccess> {
    @Accessor("entityClass")
    Class<T> getEntityClass();

    @Accessor("intialSectionVisibility")
    Long2ObjectFunction<Visibility> getIntialSectionVisibility();

    @Accessor("sections")
    Long2ObjectMap<EntitySection<T>> getSections();

    @Accessor("sectionIds")
    LongSortedSet getSectionIds();

}
