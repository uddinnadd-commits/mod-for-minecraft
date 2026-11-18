package com.sorrowed.block;

import com.sorrowed.event.StoryEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A screen that is still on: the chat in the hallway, the console in the server room, the old
 * television in the static, his laptop in the orchard. Use it and it shows you what is on it.
 * 'kind' is one of chat, console, snow, clear, laptop.
 */
public class ScreenBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape MONITOR_Z = Block.box(1, 0, 5, 15, 12, 11);
    private static final VoxelShape MONITOR_X = Block.box(5, 0, 1, 11, 12, 15);
    private static final VoxelShape LAPTOP = Block.box(2, 0, 2, 14, 9, 14);

    private final String kind;

    public ScreenBlock(String kind, int light) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(state -> light)
                .noLootTable());
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public String kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if ("laptop".equals(kind)) return LAPTOP;
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? MONITOR_Z : MONITOR_X;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && hand == InteractionHand.MAIN_HAND) {
            StoryEvents.useScreen(sp, kind, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
