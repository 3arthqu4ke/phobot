package me.earth.phobot.services;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Detects when other players could steal our {@link ExperienceOrb}s or {@link Item}s.
 */
public class StealingDetectionService {
    public boolean couldDropsGetStolen(Player self, ClientLevel level) {
        // TODO: make this detection even better. Is EntityID priority still a thing?
        // TODO: inflate a bit
        for (Player player : level.players()) {
            if (self != player && self.getBoundingBox().intersects(player.getBoundingBox())) {
                return true;
            }
        }

        return false;
    }

}
