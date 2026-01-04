package org.doothy.untitled.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.doothy.untitled.block.entity.ManaBatteryBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A simple block that hosts a {@link org.doothy.untitled.block.entity.ManaBatteryBlockEntity}
 * used to store and expose mana. Provides facing, standard model rendering, and an
 * interaction to display current mana to the player when sneaking.
 */
public class ManaBatteryBlock extends BaseEntityBlock {

    public static final MapCodec<ManaBatteryBlock> CODEC =
            simpleCodec(ManaBatteryBlock::new);

    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    /**
     * Creates a new Mana Battery block with default NORTH facing.
     */
    public ManaBatteryBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
        );
    }

    /* ───────────────────────── Codec ───────────────────────── */

    /**
     * Codec used by the game to (de)serialize block state and creation.
     */
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /* ───────────────────────── BlockState ───────────────────────── */

    /**
     * Adds the {@link #FACING} property to this block's state definition.
     */
    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }

    /**
     * Orients the block to face the player upon placement.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                );
    }

    /* ───────────────────────── Rendering ───────────────────────── */

    /**
     * Renders as a normal block model.
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /* ───────────────────────── Block Entity ───────────────────────── */

    /**
     * Creates the associated {@link ManaBatteryBlockEntity} instance.
     */
    @Override
    public @Nullable BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new ManaBatteryBlockEntity(pos, state);
    }

    /* ───────────────────────── Interaction ───────────────────────── */

    /**
     * Handles sneak-right-click to show current mana/capacity to the player on the server.
     */
    @Override
    public InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        // Only respond on SHIFT + right-click
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ManaBatteryBlockEntity battery) {

                var storage = battery.getManaOutput();

                player.displayClientMessage(
                        Component.translatable(
                                "message.untitled.mana_battery",
                                storage.getMana(),
                                storage.getMaxMana()
                        ),
                        true // action bar
                );
            }
        }

        return InteractionResult.SUCCESS;
    }
}
