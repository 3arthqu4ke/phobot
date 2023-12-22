package me.earth.phobot.util.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;

/**
 * A level that copies the entities from a ClientLevel for some thread safety.
 */
public class EntityCopyingLevel extends UnusableLevels.DelegatingEntityCopyingLevel {
    public EntityCopyingLevel(ClientLevel level) {
        super(level);
    }

    public EntityCopyingLevel(ClientLevel level, LevelEntityGetterAdapter<Entity> levelEntityGetterAdapter) {
        super(level, levelEntityGetterAdapter);
    }

    // these will be called in the constructor and cannot delegate, or we crash.

    @Override
    public int getHeight() {
        return this.dimensionType().height();
    }

    @Override
    public int getMinBuildHeight() {
        return this.dimensionType().minY();
    }

    @Override
    public int getMaxBuildHeight() {
        return this.getMinBuildHeight() + this.getHeight();
    }

    @Override
    public int getSectionsCount() {
        return this.getMaxSection() - this.getMinSection();
    }

    @Override
    public int getMinSection() {
        return SectionPos.blockToSectionCoord(this.getMinBuildHeight());
    }

    @Override
    public int getMaxSection() {
        return SectionPos.blockToSectionCoord(this.getMaxBuildHeight() - 1) + 1;
    }

}
