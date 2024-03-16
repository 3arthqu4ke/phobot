package me.earth.phobot.holes;

import com.google.common.collect.Sets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

public interface HoleBlocks {
    Set<Block> NO_BLAST = Sets.newHashSet(Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ANVIL, Blocks.ENDER_CHEST);
    Set<Block> UNSAFE = Sets.newHashSet(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ANVIL, Blocks.ENDER_CHEST);

    default Set<Block> noBlastBlocks() {
        return NO_BLAST;
    }

    default Set<Block> unsafeBlocks() {
        return UNSAFE;
    }

}
