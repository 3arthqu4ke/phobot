package me.earth.phobot.pathfinder.movement;

import me.earth.phobot.Phobot;
import me.earth.phobot.pathfinder.Path;
import me.earth.phobot.pathfinder.algorithm.AlgorithmRenderer;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.util.CancellationTaskUtil;
import me.earth.phobot.util.NullabilityUtil;
import me.earth.pingbypass.PingBypass;

public class VisualizingMovementPathfinder extends MovementPathfinder {
    public VisualizingMovementPathfinder(PingBypass pingBypass) {
        super(pingBypass);
        this.getListeners().clear();
    }

    @Override
    public void follow(Phobot phobot, Path<MeshNode> path) {
        phobot.getMinecraft().submit(() -> NullabilityUtil.safe(phobot.getMinecraft(), ((localPlayer, level, gameMode) -> {
            MovementPathfindingAlgorithm algorithm = new MovementPathfindingAlgorithm(phobot, level, path, localPlayer, null, null);
            MovementPathfindingAlgorithm asyncAlgorithm = new MovementPathfindingAlgorithm(phobot, level, path, localPlayer, null, null);
            CancellationTaskUtil.runWithTimeOut(asyncAlgorithm, null, 0, phobot.getExecutorService());
            this.state = new State(new AlgorithmRenderer<>(asyncAlgorithm), algorithm, path, algorithm.getStart());
            phobot.getPingBypass().getEventBus().subscribe(state.algorithmRenderer);
        })));
    }

}
