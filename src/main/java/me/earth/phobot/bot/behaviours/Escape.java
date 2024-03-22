package me.earth.phobot.bot.behaviours;

import lombok.Getter;
import me.earth.phobot.bot.Bot;
import me.earth.phobot.event.PreKeybindHandleEvent;
import me.earth.phobot.mixins.entity.IPlayer;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.services.inventory.InventoryContext;
import me.earth.phobot.util.InventoryUtil;
import me.earth.pingbypass.api.event.SafeListener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.event.network.ReceiveListener;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

// TODO: if we are trapped, mine the block above us, and already calculate paths with a special MeshNode that contains the adjacent for when we break the block?
// TODO: this is currently very predictable, we will chorus as soon as we get trapped!
// TODO: check Chorus Cooldown?
@Getter
public class Escape extends Behaviour {
    private boolean finishedGapple;
    private boolean trapped;

    public Escape(Bot bot) {
        super(bot, PRIORITY_ESCAPE);
        listen(new SafeListener<PreKeybindHandleEvent>(mc, 100) {
            @Override
            public void onEvent(PreKeybindHandleEvent event, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (trapped && finishedGapple) {
                    if (player.getUseItem().is(Items.CHORUS_FRUIT)) {
                        event.setCancelled(true);
                        return;
                    }

                    phobot.getInventoryService().use(context -> {
                        Item item = context.find(Items.CHORUS_FRUIT);
                        if (item == null) {
                            pingBypass.getChat().delete("EscapeChorus");
                            pingBypass.getChat().sendWithoutLogging(Component.literal("AutoEat could not find Chorus Fruit!").withStyle(ChatFormatting.RED), "EscapeChorus");
                        } else {
                            int flags = InventoryContext.PREFER_MAINHAND | InventoryContext.SET_CARRIED_ITEM;
                            if (bot.getModules().getKillAura().isEnabled()
                                    && !phobot.getInventoryService().isLockedIntoTotem()
                                    && !InventoryUtil.isHoldingWeapon(player)) {
                                flags = InventoryContext.DEFAULT_SWAP_SWITCH;
                            }

                            InventoryContext.SwitchResult switchResult = context.switchTo(Items.CHORUS_FRUIT, flags);
                            if (switchResult != null) {
                                gameMode.useItem(player, switchResult.hand());
                            }

                            event.setCancelled(true);
                        }
                    });
                }
            }
        });

        listen(new ReceiveListener.Scheduled.Safe<ClientboundSetEntityDataPacket>(mc) {
            @Override
            public void onSafeEvent(PacketEvent.Receive<ClientboundSetEntityDataPacket> event, LocalPlayer localPlayer, ClientLevel level, MultiPlayerGameMode gameMode) {
                if (trapped && event.getPacket().id() == localPlayer.getId()) {
                    for (SynchedEntityData.DataValue<?> value : event.getPacket().packedItems()) {
                        if (value.id() == IPlayer.getDataPlayerAbsorptionId().getId() && value.value() instanceof Float absorptionAmount) {
                            float previousAbsorptionAmount = localPlayer.getAbsorptionAmount();
                            if (absorptionAmount == 4.0f && previousAbsorptionAmount < absorptionAmount) { // normal Golden Apple
                                finishedGapple = true;
                            } else if (absorptionAmount == 16.0f) { // Enchanted Golden Apple
                                finishedGapple = true;
                            } // sadly no way to detect normal apple on top of enchanted one I think
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void update(LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode) {
        if (phobot.getPathfinder().isFollowingPath() || bot.getJumpDownFromSpawn().isAboveSpawn(player)) {
            finishedGapple = false;
            trapped = false;
            return;
        }

        Optional<MeshNode> meshNode = phobot.getNavigationMeshManager().getStartNode(player);
        // TODO: not sure if this covers all cases yet. What if we are inside a block, with a block above us? We would be relly trapped with a MeshNode far away.
        //  this is mostly a problem of .getNavigationMeshManager().getStartNode. We need to be able to reach that node.
        // TODO: service that checks once every tick whether we can reach the currently closest MeshNode?
        if (meshNode.isEmpty()) {
            trapped = false;
        } else if (meshNode.get().distanceSqToCenter(player.getX(), player.getY(), player.getZ()) >= 2.0 && player.onGround()) {
            trapped = true;
        } else {
            trapped = isTrapped(meshNode.get());
        }

        if (!trapped) {
            finishedGapple = false;
        } else {
            finishedGapple = finishedGapple || player.getAbsorptionAmount() >= 16.0f;
        }
    }

    public boolean isTrapped(@Nullable MeshNode targetNode) {
        if (targetNode == null) {
            return true;
        }

        Set<MeshNode> nodes = new HashSet<>();
        nodes.add(targetNode);
        int amountOfNodesNeededToBeNotTrapped = 6;
        // check how many MeshNodes there are around us
        recurseUntilSizeOfMapReaches(targetNode, nodes, amountOfNodesNeededToBeNotTrapped);
        return nodes.size() < amountOfNodesNeededToBeNotTrapped;
    }

    private boolean recurseUntilSizeOfMapReaches(MeshNode current, Set<MeshNode> visited, int size) {
        for (MeshNode adjacent : current.getAdjacent()) {
            if (adjacent != null && visited.add(adjacent)) {
                if (visited.size() >= size || recurseUntilSizeOfMapReaches(adjacent, visited, size)) {
                    return true;
                }
            }
        }

        return false;
    }

}
