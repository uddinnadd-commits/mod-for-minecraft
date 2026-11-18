package com.sorrowed.block;

import com.sorrowed.item.PapersItem;
import com.sorrowed.story.Findings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A few sheets lying on the floor. Pick them up and they are a page of Papers: which one depends
 * on the world you are in and where you found it. The worlds are full of them, if you look.
 */
public class LoosePapersBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 1, 15);

    public LoosePapersBlock() {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .noOcclusion()
                .instabreak()
                .sound(SoundType.WOOL)
                .noLootTable());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.getBlockState(pos.below()).isAir();
    }

    private static ItemStack page(Level level, BlockPos pos) {
        return PapersItem.of(Findings.pick(level.dimension(), pos.asLong()));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) {
            ItemStack stack = page(level, pos);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            sl.removeBlock(pos, false);
            sl.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.9F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity be, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, be, tool);
        if (!level.isClientSide) Block.popResource(level, pos, page(level, pos));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // A corner lifts in a draught that is not there.
        if (random.nextInt(40) == 0) {
            level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + random.nextDouble(), pos.getY() + 0.1D,
                    pos.getZ() + random.nextDouble(), 0.0D, 0.02D, 0.0D);
        }
    }
}
