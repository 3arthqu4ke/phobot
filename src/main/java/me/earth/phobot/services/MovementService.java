package me.earth.phobot.services;

import lombok.Getter;
import me.earth.phobot.movement.BunnyHopCC;
import me.earth.phobot.movement.Movement;

@Getter
public class MovementService {
    private final Movement movement = new BunnyHopCC();

}
