package me.earth.phobot.ducks;

import net.minecraft.world.entity.Entity;

public interface IServerboundInteractPacket {
    Entity getPhobot$entity();

    void setPhobot$entity(Entity phobot$entity);

    boolean isPhobot$attack();

    void setPhobot$attack(boolean phobot$attack);

}
