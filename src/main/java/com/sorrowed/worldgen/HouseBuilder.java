package com.sorrowed.worldgen;

import com.sorrowed.story.Letters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * The half-built house: the west half has walls and a roof, the east half stops at knee height
 * with scaffolding. Origin = centre, first air block above the ground. 7x7 footprint.
 */
public final class HouseBuilder {
    private HouseBuilder() {}

    private static BlockPos at(BlockPos o, int x, int y, int z) {
        return o.offset(x, y, z);
    }

    public static void build(LevelAccessor level, BlockPos o, RandomSource rand, boolean finalPage) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();

        // Clear.
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = 0; y <= 6; y++) {
                    BlockPos p = at(o, x, y, z);
                    if (!level.getBlockState(p).isAir()) BuildUtil.set(level, p, air);
                }
            }
        }

        // Floor and foundation.
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BuildUtil.set(level, at(o, x, -1, z), planks);
                BuildUtil.fillDown(level, at(o, x, -2, z), Blocks.COBBLESTONE.defaultBlockState(), 8);
            }
        }

        // Walls: full height on the west half, knee height on the east half.
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3;
                if (!edge) continue;
                boolean corner = Math.abs(x) == 3 && Math.abs(z) == 3;
                int height = x <= 0 ? 4 : 2;
                for (int y = 0; y < height; y++) {
                    BuildUtil.set(level, at(o, x, y, z), corner ? log : planks);
                }
            }
        }

        // Doorway (never got a door), windows.
        BuildUtil.set(level, at(o, 0, 0, 3), air);
        BuildUtil.set(level, at(o, 0, 1, 3), air);
        BuildUtil.set(level, at(o, -3, 1, 0), Blocks.GLASS_PANE.defaultBlockState());
        BuildUtil.set(level, at(o, 0, 1, -3), Blocks.GLASS_PANE.defaultBlockState());

        // Roof over the west half only.
        for (int x = -3; x <= 0; x++) {
            for (int z = -3; z <= 3; z++) {
                BuildUtil.set(level, at(o, x, 4, z), planks);
            }
        }

        // Scaffolding on the east side: fence posts and a couple of beams.
        for (int y = 2; y <= 4; y++) {
            BuildUtil.set(level, at(o, 3, y, 3), Blocks.OAK_FENCE.defaultBlockState());
            BuildUtil.set(level, at(o, 3, y, -3), Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int x = 1; x <= 3; x++) {
            BuildUtil.set(level, at(o, x, 4, -3), log);
        }

        // Furniture.
        BlockPos foot = at(o, -2, 0, -2);
        BuildUtil.set(level, foot, Blocks.GRAY_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT));
        BuildUtil.set(level, foot.east(), Blocks.GRAY_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.HEAD));
        BuildUtil.set(level, at(o, -2, 0, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        BuildUtil.set(level, at(o, -2, 1, 2), Blocks.LANTERN.defaultBlockState());
        BuildUtil.set(level, at(o, -1, 0, 2), Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH));

        BlockPos chestPos = at(o, -2, 0, 0);
        BuildUtil.set(level, chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            if (finalPage) {
                chest.setItem(0, Letters.lastPage());
            } else {
                chest.setItem(0, Letters.notesBook());
                chest.setItem(1, new ItemStack(Items.BREAD, 6));
                chest.setItem(2, new ItemStack(Items.TORCH, 16));
                chest.setItem(3, new ItemStack(Items.COOKED_BEEF, 8));
            }
        }
    }
}
