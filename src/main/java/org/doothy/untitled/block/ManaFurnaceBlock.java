package org.doothy.untitled.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.doothy.untitled.block.entity.ManaFurnaceBlockEntity;
import org.doothy.untitled.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * A custom furnace block that uses mana.
 */
public class ManaFurnaceBlock extends BaseEntityBlock {
    public static final MapCodec<ManaFurnaceBlock> CODEC = simpleCodec(ManaFurnaceBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ManaFurnaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, net.minecraft.core.Direction.NORTH)
                        .setValue(LIT, false)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    /**
     * Defines the render shape of the block.
     * Essential for the block to actually render the model, unlike standard BlockEntities.
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaFurnaceBlockEntity(pos, state);
    }

    /**
     * Handles block interaction (right-click).
     * Opens the furnace GUI.
     */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ManaFurnaceBlockEntity furnace) {
                // Opens the screen handler
                player.openMenu(furnace);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, false);
    }

    /**
     * Provides the ticker for the block entity.
     * Connects the ticking logic.
     */
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MANA_FURNACE_BE, ManaFurnaceBlockEntity::tick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;

        // subtle smoke
        if (random.nextFloat() < 0.3f) {
            level.addParticle(
                    ParticleTypes.SMOKE,
                    x,
                    y,
                    z,
                    0.0,
                    0.05,
                    0.0
            );
        }

        // mana spark (rare)
        if (random.nextFloat() < 0.1f) {
            level.addParticle(
                    ParticleTypes.ENCHANT,
                    x + (random.nextDouble() - 0.5) * 0.3,
                    y,
                    z + (random.nextDouble() - 0.5) * 0.3,
                    0.0,
                    0.02,
                    0.0
            );
        }

        if (random.nextFloat() < 0.02f) {
            level.playLocalSound(
                    x,
                    y,
                    z,
                    SoundEvents.FURNACE_FIRE_CRACKLE,
                    SoundSource.BLOCKS,
                    0.4f,
                    1.0f,
                    false
            );
        }
    }
}
