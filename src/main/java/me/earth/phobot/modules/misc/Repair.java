package me.earth.phobot.modules.misc;

import me.earth.phobot.Phobot;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.pingbypass.api.input.Key;
import me.earth.pingbypass.api.input.Keys;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.commons.event.SafeListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static me.earth.phobot.services.inventory.InventoryContext.*;

public class Repair extends PhobotModule {
    private final Setting<Boolean> middleClickExp = bool("MiddleClick", true, "Allows you to mend with middleclick.");
    private final Setting<Integer> packets = number("Packets", 1, 1, 10, "Sends additional packets to throw more experience bottles.");
    private final Setting<Boolean> auto = bool("Auto", false, "Automatically mends your armor.");

    public Repair(Phobot phobot) {
        super(phobot, "Repair", Categories.MISC, "Tweaks for mending.");
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                phobot.getInventoryService().use(ctx -> {
                    boolean critical = false;
                    float totalDurability = 4.0f;
                    for (ItemStack stack : player.getInventory().armor) {
                        float durability = (float) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
                        if (durability <= 0.5f) {
                            critical = true;
                        }

                        totalDurability -= durability;
                    }

                    if (auto.getValue() && (critical || totalDurability > 0.3f) || middleClickExp.getValue() && phobot.getPingBypass().getKeyBoardAndMouse().isPressed(Key.Type.MOUSE, Keys.MOUSE_3)) {
                        // TODO: look in move direction
                        // phobot.getMotionUpdateService().rotate(player, rotations[0], rotations[1]);
                        InventoryContext.SwitchResult result = ctx.switchTo(Items.EXPERIENCE_BOTTLE, SWITCH_BACK | PREFER_MAINHAND | SET_CARRIED_ITEM);
                        if (result != null) {
                            for (int i = 0; i < packets.getValue(); i++) {
                                player.connection.send(new ServerboundUseItemPacket(result.hand(), 0));
                            }
                        }
                    }
                });
            }
        });
    }

}
