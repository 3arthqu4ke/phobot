package me.earth.phobot.modules.misc;

import me.earth.phobot.Phobot;
import me.earth.phobot.event.PostMotionPlayerUpdateEvent;
import me.earth.phobot.event.PreMotionPlayerUpdateEvent;
import me.earth.phobot.modules.PhobotModule;
import me.earth.phobot.services.StealingDetectionService;
import me.earth.phobot.services.SurroundService;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.math.RotationUtil;
import me.earth.phobot.util.mutables.MutVec3;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.input.Key;
import me.earth.pingbypass.api.input.Keys;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static me.earth.phobot.services.inventory.InventoryContext.*;

public class Repair extends PhobotModule {
    private final Setting<Boolean> middleClickExp = bool("MiddleClick", true, "Allows you to mend with middle click.");
    private final Setting<Integer> packets = number("Packets", 1, 1, 10, "Sends additional packets to throw more experience bottles.");
    private final Setting<Boolean> auto = bool("Auto", false, "Automatically mends your armor.");
    private final Setting<Boolean> rotate = bool("Rotate", true, "Rotates to throw Experience in front of you.");
    private final StealingDetectionService stealingDetectionService = new StealingDetectionService();
    private final MutVec3 vec = new MutVec3();
    private boolean throwing;

    public Repair(Phobot phobot, SurroundService surroundService) {
        super(phobot, "Repair", Categories.MISC, "Tweaks for mending.");
        listen(new SafeListener<PreMotionPlayerUpdateEvent>(mc, -1000/*after KILLAURA!!!*/) {
            @Override
            public void onEvent(PreMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                throwing = false;
                phobot.getInventoryService().use(ctx -> {
                    if (!ctx.has(Items.EXPERIENCE_BOTTLE)) {
                        return;
                    }
                    // TODO: this isnt like the greatest way to do this
                    Criticality criticality = getCriticality(player);
                    if (middleClickExp.getValue() && phobot.getPingBypass().getKeyBoardAndMouse().isPressed(Key.Type.MOUSE, Keys.MOUSE_3)
                            || auto.getValue() && (criticality.criticality == 2
                                                    || (criticality.totalDurabilityMissing > 0.7f
                                                            && !stealingDetectionService.couldDropsGetStolen(player, level)))
                                                    || (criticality.totalDurabilityMissing > 0.3f
                                                            && surroundService.isSurrounded()
                                                            && !stealingDetectionService.couldDropsGetStolen(player, level))) {
                        if (rotate.getValue()) {
                            float[] rotations = RotationUtil.lookIntoMoveDirection(player, vec);
                            phobot.getMotionUpdateService().rotate(player, rotations[0], rotations[1]);
                        }

                        throwing = true;
                    }
                });
            }
        });

        listen(new SafeListener<PostMotionPlayerUpdateEvent>(mc, 999) {
            @Override
            public void onEvent(PostMotionPlayerUpdateEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (throwing) {
                    phobot.getInventoryService().use(ctx -> {
                        InventoryContext.SwitchResult result = ctx.switchTo(Items.EXPERIENCE_BOTTLE, SWITCH_BACK | PREFER_MAINHAND | SET_CARRIED_ITEM);
                        if (result != null) {
                            for (int i = 0; i < packets.getValue(); i++) {
                                player.connection.send(new ServerboundUseItemPacket(result.hand(), 0));
                            }
                        }
                    });
                }
            }
        });
    }

    public Criticality getCriticality(Player player) {
        int criticality = 0;
        float totalDurability = 4.0f;
        for (ItemStack stack : player.getInventory().armor) {
            float durability = (float) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
            if (durability <= 0.3f) {
                criticality = 2;
            } else if (durability <= 0.6f) {
                criticality = Math.max(criticality, 1);
            }

            totalDurability -= durability;
        }

        return new Criticality(totalDurability, criticality);
    }

    public record Criticality(float totalDurabilityMissing, int criticality) { }

}
