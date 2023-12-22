package me.earth.phobot.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.earth.pingbypass.api.command.CommandSource;
import me.earth.pingbypass.api.command.impl.AbstractCommand;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public class KitCommand extends AbstractCommand {
    public KitCommand() {
        super("kit", "Gives you a kit.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            LocalPlayer player = ctx.getSource().getMinecraft().player;
            if (player != null) {
                ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
                unbreakingAndMending(helmet);
                helmet.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
                helmet.enchant(Enchantments.AQUA_AFFINITY, 1);
                helmet.enchant(Enchantments.RESPIRATION, 3);

                ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
                unbreakingAndMending(chest);
                chest.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);

                ItemStack legs = new ItemStack(Items.NETHERITE_LEGGINGS);
                unbreakingAndMending(legs);
                legs.enchant(Enchantments.BLAST_PROTECTION, 4);

                ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
                unbreakingAndMending(boots);
                boots.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
                boots.enchant(Enchantments.FALL_PROTECTION, 4);
                boots.enchant(Enchantments.SOUL_SPEED, 3);
                // boots.enchant(Enchantments.DEPTH_STRIDER, 3);

                ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
                unbreakingAndMending(sword);
                sword.enchant(Enchantments.SHARPNESS, 5);
                sword.enchant(Enchantments.FIRE_ASPECT, 2);
                sword.enchant(Enchantments.KNOCKBACK, 2);

                ItemStack pick = new ItemStack(Items.NETHERITE_PICKAXE);
                unbreakingAndMending(pick);
                pick.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
                pick.enchant(Enchantments.BLOCK_FORTUNE, 3);

                ShulkerBoxBlockEntity shulkerBoxBlockEntity = new ShulkerBoxBlockEntity(BlockPos.ZERO, Blocks.SHULKER_BOX.defaultBlockState());
                shulkerBoxBlockEntity.setItem(0, helmet);
                shulkerBoxBlockEntity.setItem(1, chest);
                shulkerBoxBlockEntity.setItem(2, legs);
                shulkerBoxBlockEntity.setItem(3, boots);
                shulkerBoxBlockEntity.setItem(4, sword);
                shulkerBoxBlockEntity.setItem(5, pick);
                shulkerBoxBlockEntity.setItem(6, new ItemStack(Items.CHORUS_FRUIT, 64));
                shulkerBoxBlockEntity.setItem(7, new ItemStack(Items.OBSIDIAN, 64));
                shulkerBoxBlockEntity.setItem(8, new ItemStack(Items.ENDER_CHEST, 64));
                for (int i = 9; i < 16; i++) {
                    shulkerBoxBlockEntity.setItem(i, new ItemStack(Items.EXPERIENCE_BOTTLE, 64));
                }

                for (int i = 16; i < 18; i++) {
                    shulkerBoxBlockEntity.setItem(i, new ItemStack(Items.TOTEM_OF_UNDYING));
                }

                for (int i = 18; i < 25; i++) {
                    shulkerBoxBlockEntity.setItem(i, new ItemStack(Items.END_CRYSTAL, 64));
                }

                for (int i = 25; i < 27; i++) {
                    shulkerBoxBlockEntity.setItem(i, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 64));
                }

                CompoundTag tag = shulkerBoxBlockEntity.saveWithFullMetadata();
                ItemStack stack = new ItemStack(Items.SHULKER_BOX);
                BlockItem.setBlockEntityData(stack, shulkerBoxBlockEntity.getType(), tag);

                player.getInventory().setItem(0, stack);
                if (player.isCreative()) {
                    player.connection.send(new ServerboundSetCreativeModeSlotPacket(36, stack));
                } else if (ctx.getSource().getMinecraft().isSingleplayer()) {
                    IntegratedServer server = ctx.getSource().getMinecraft().getSingleplayerServer();
                    if (server != null) {
                        Player singlePlayer = server.getPlayerList().getPlayer(player.getUUID());
                        if (singlePlayer != null) {
                            singlePlayer.getInventory().setItem(0, stack);
                        }
                    }
                }
            }

            return Command.SINGLE_SUCCESS;
        });
    }

    private void unbreakingAndMending(ItemStack stack) {
        stack.enchant(Enchantments.UNBREAKING, 3);
        stack.enchant(Enchantments.MENDING, 1);
    }

}
