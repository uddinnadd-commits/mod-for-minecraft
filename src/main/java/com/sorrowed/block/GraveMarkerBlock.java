package com.sorrowed.block;

import com.sorrowed.cutscene.CutsceneServer;
import com.sorrowed.mind.Sorrow;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.story.Thoughts;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A nameless stone. Right-click it and the player thinks something. */
public class GraveMarkerBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(3, 0, 6, 13, 15, 10);

    public GraveMarkerBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.STONE)
                .noLootTable());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            CompoundTag root = sp.getPersistentData();
            CompoundTag saved = root.getCompound("PlayerPersisted");
            boolean seen = saved.getBoolean("sorrowed_grave_seen");
            boolean played = !seen && CutsceneServer.play(sp, "grave", Vec3.atBottomCenterOf(pos),
                    () -> Sorrow.add(sp, -15.0F));
            if (played) {
                saved.putBoolean("sorrowed_grave_seen", true);
                root.put("PlayerPersisted", saved);
            } else if (!CutsceneServer.isPlaying(sp)) {
                ModNetwork.thought(sp, Thoughts.pick(Thoughts.GRAVE, level.random));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
