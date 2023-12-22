package me.earth.phobot.util.world;

import lombok.experimental.Delegate;
import me.earth.phobot.mixins.level.IClientLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Levels in this class cannot be instantiated without an Exception, unless certain methods are overridden.
 * Those methods can be found in {@link EntityCopyingLevel}, an example for how to create subclasses of these classes.
 */
public abstract class UnusableLevels {
    public abstract static class DelegatingEntityCopyingLevel extends DelegatingClientLevel {
        @Delegate
        protected final CollisionAndEntityGetter collisionAndEntityGetter;

        protected DelegatingEntityCopyingLevel(ClientLevel level) {
            this(level, EntityGetterUtil.copy(((LevelEntityGetterAdapter<Entity>) ((IClientLevel) level).getEntityStorage().getEntityGetter())));
        }

        protected DelegatingEntityCopyingLevel(ClientLevel level, LevelEntityGetterAdapter<Entity> entities) {
            super(level);
            List<AbstractClientPlayer> players = new ArrayList<>(level.players());
            this.collisionAndEntityGetter = new CollisionAndEntityGetter(entities, players, level);
        }
    }

    /**
     * Can also not be instantiated!
     */
    public abstract static class UnusableBlockStateLevel extends DelegatingClientLevel implements BlockStateLevel {
        @Delegate
        private final BlockStateLevelImpl customBlockStateLevel;

        public UnusableBlockStateLevel(ClientLevel level) {
            super(level);
            this.customBlockStateLevel = getImpl(level);
        }

        // TODO: See MovementParable, this behaviour is extremly weird!
        //  all problems stem from the overuse of @Delegate, which might save some lines of code, but makes lots of behaviour not understandable!!!!
        // TODO: REMOVE ALL USAGES OF @DELEGATE
        public BlockStateLevelImpl getImpl(ClientLevel level) {
            return new BlockStateLevelImpl(level);
        }
    }

}
