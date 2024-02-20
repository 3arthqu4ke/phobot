package me.earth.phobot.pathfinder;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManager;
import me.earth.phobot.pathfinder.movement.MovementPathfinder;
import me.earth.phobot.pathfinder.movement.VisualizingMovementPathfinder;
import me.earth.phobot.services.MovementService;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.SubscriberImpl;

import java.util.concurrent.ExecutorService;

@Slf4j
@Getter
public class Pathfinder extends SubscriberImpl {
    private final NavigationMeshManager navigationMeshManager;
    private final MovementPathfinder movementPathfinder;

    public Pathfinder(PingBypass pingBypass, NavigationMeshManager navigationMeshManager, MovementService movementService, ExecutorService executor) {
        this.navigationMeshManager = navigationMeshManager;
        this.movementPathfinder = new VisualizingMovementPathfinder(pingBypass); // TODO: subscribe?
    }

}
