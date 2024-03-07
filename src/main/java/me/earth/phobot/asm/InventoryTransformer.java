package me.earth.phobot.asm;

import lombok.extern.slf4j.Slf4j;
import me.earth.pingbypass.api.launch.Transformer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

@Slf4j
public class InventoryTransformer extends Transformer implements MakesFieldVolatile {
    public InventoryTransformer() {
        super("net/minecraft/class_1661", "net/minecraft/world/entity/player/Inventory", "net/minecraft/entity/player/PlayerInventory");
    }

    @Override
    public void transform(ClassNode classNode) {
        try {
            makeFieldVolatile(classNode, Type.INT_TYPE, "field_7545", "selected", "selectedSlot");
            log.info("Made Inventory.selected volatile successfully.");
        } catch (FailedToMakeFieldVolatileException e) {
            log.error("Failed to make Inventory.selected volatile", e);
        }
    }

}

