package me.earth.phobot.util.world;

import lombok.experimental.UtilityClass;
import me.earth.phobot.mixins.level.IEntitySectionStorage;
import me.earth.phobot.mixins.level.ILevelEntityGetterAdapter;
import net.minecraft.world.level.entity.*;

@UtilityClass
final class EntityGetterUtil {
    @SuppressWarnings("unchecked")
    public static <T extends EntityAccess> LevelEntityGetterAdapter<T> copy(LevelEntityGetterAdapter<T> adapter) {
        ILevelEntityGetterAdapter<T> access = (ILevelEntityGetterAdapter<T>) adapter;
        return new LevelEntityGetterAdapter<>(copy(access.getVisibleEntities()), copy(access.getSectionStorage()));
    }

    public static <T extends EntityAccess> EntityLookup<T> copy(EntityLookup<T> lookup) {
        EntityLookup<T> copy = new EntityLookup<>();
        lookup.getAllEntities().forEach(copy::add);
        return copy;
    }

    @SuppressWarnings("unchecked")
    public static <T extends EntityAccess> EntitySectionStorage<T> copy(EntitySectionStorage<T> entitySectionStorage) {
        IEntitySectionStorage<T> access = ((IEntitySectionStorage<T>) entitySectionStorage);
        EntitySectionStorage<T> copy = new EntitySectionStorage<>(access.getEntityClass(), access.getIntialSectionVisibility());
        access.getSectionIds().forEach(l -> ((IEntitySectionStorage<T>) copy).getSectionIds().add(l));
        access.getSections().forEach((l,s) -> ((IEntitySectionStorage<T>) copy).getSections().putIfAbsent(l, copy(access.getEntityClass(), s)));
        return copy;
    }

    public static <T extends EntityAccess> EntitySection<T> copy(Class<T> type, EntitySection<T> entitySection) {
        EntitySection<T> copy = new EntitySection<>(type, entitySection.getStatus());
        entitySection.getEntities().forEach(copy::add);
        return copy;
    }

}
