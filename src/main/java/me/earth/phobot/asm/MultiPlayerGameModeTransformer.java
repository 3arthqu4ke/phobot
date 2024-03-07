package me.earth.phobot.asm;

import lombok.extern.slf4j.Slf4j;
import me.earth.pingbypass.api.launch.Transformer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

@Slf4j
public class MultiPlayerGameModeTransformer extends Transformer implements MakesFieldVolatile {
    public MultiPlayerGameModeTransformer() {
        super("net/minecraft/class_636", "net/minecraft/client/multiplayer/MultiPlayerGameMode", "net/minecraft/client/network/ClientPlayerInteractionManager");
    }

    @Override
    public void transform(ClassNode classNode) {
        try {
            makeFieldVolatile(classNode, Type.INT_TYPE, "field_3721", "carriedIndex", "lastSelectedSlot");
            log.info("Made MultiPlayerGameMode.carriedIndex volatile successfully.");
        } catch (FailedToMakeFieldVolatileException e) {
            log.error("Failed to make MultiPlayerGameMode.carriedIndex volatile", e);
        }
    }

}
