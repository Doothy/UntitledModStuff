package org.doothy.untitled.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.doothy.untitled.block.entity.ManaFurnaceBlockEntity;
import org.doothy.untitled.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * A custom furnace block that uses mana.
 */
public class ManaFurnaceBlock extends BaseEntityBlock {
    public static final MapCodec<ManaFurnaceBlock> CODEC = simpleCodec(ManaFurnaceBlock::new);

    public ManaFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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

    /**
     * Provides the ticker for the block entity.
     * Connects the ticking logic.
     */
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MANA_FURNACE_BE, ManaFurnaceBlockEntity::tick);
    }
}