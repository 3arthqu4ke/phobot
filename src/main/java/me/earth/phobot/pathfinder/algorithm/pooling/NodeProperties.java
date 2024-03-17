package me.earth.phobot.pathfinder.algorithm.pooling;

import lombok.Data;
import me.earth.phobot.pathfinder.mesh.MeshNode;

/**
 * Properties a node needs for A-Star pathfinding.
 */
@Data
public class NodeProperties {
    private final int algorithmIndex;
    private MeshNode cameFrom;
    private int heapIndex;
    private double fScore;
    private double gScore;
    
}
