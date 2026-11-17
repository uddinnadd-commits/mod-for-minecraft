package com.sorrowed.block;

import com.sorrowed.mind.Transition;
import com.sorrowed.worldgen.HubBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The portal. Stand in it and you go to the target dimension. If you are already in the
 * target dimension it takes you back to where you came in (or to the fallback dimension).
 */
public class DoorwayBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape SHAPE_X = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape SHAPE_Z = Block.box(6, 0, 0, 10, 16, 16);

    private final ResourceKey<Level> target;
    private final ResourceKey<Level> fallback;

    public DoorwayBlock(ResourceKey<Level> target, ResourceKey<Level> fallback) {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .noOcclusion()
                .strength(-1.0F, 3600000.0F)
                .lightLevel(state -> 9)
                .sound(SoundType.GLASS)
                .noLootTable());
        this.target = target;
        this.fallback = fallback;
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.ASH,
                    pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(),
                    0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel from)) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (player.isPassenger() || player.isVehicle()) return;
        if (Transition.inTransit(player)) return;
        if (player.isOnPortalCooldown()) {
            player.setPortalCooldown();
            return;
        }

        MinecraftServer server = from.getServer();
        Direction.Axis axis = state.getValue(AXIS);

        // Which side of the doorway the player is on, so the way back puts them outside it.
        double offset = axis == Direction.Axis.X
                ? player.getZ() - (pos.getZ() + 0.5)
                : player.getX() - (pos.getX() + 0.5);
        int side = offset >= 0 ? 1 : -1;
        double outX = axis == Direction.Axis.X ? pos.getX() + 0.5 : pos.getX() + 0.5 + side * 2;
        double outZ = axis == Direction.Axis.X ? pos.getZ() + 0.5 + side * 2 : pos.getZ() + 0.5;

        String key = "sorrowed_return_" + target.location().getPath();
        CompoundTag data = player.getPersistentData();
        ServerLevel destLevel;
        Vec3 dest;
        float yaw = 180.0F;

        if (from.dimension().equals(target)) {
            // Leaving: go back where we came from.
            destLevel = null;
            dest = null;
            String[] parts = data.getString(key).split("\\|");
            if (parts.length == 4) {
                ResourceLocation id = ResourceLocation.tryParse(parts[0]);
                if (id != null) {
                    destLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
                    try {
                        dest = new Vec3(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                                Double.parseDouble(parts[3]));
                        yaw = player.getYRot();
                    } catch (NumberFormatException ex) {
                        dest = null;
                    }
                }
            }
            if (destLevel == null || dest == null) {
                destLevel = server.getLevel(fallback);
                if (destLevel == null) return;
                dest = HubBuilder.arrival(destLevel, fallback);
                yaw = 180.0F;
            }
        } else {
            // Entering: remember the spot outside this doorway, then go to the hub.
            destLevel = server.getLevel(target);
            if (destLevel == null) return;
            data.putString(key, from.dimension().location() + "|" + outX + "|" + player.getY() + "|" + outZ);
            dest = HubBuilder.arrival(destLevel, target);
        }

        Transition.go(player, destLevel, dest, yaw);
    }
}
