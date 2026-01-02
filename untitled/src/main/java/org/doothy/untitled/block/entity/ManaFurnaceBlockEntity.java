package org.doothy.untitled.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.doothy.untitled.Untitled;
import org.doothy.untitled.api.mana.*;
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.block.ManaFurnaceBlock;
import org.doothy.untitled.items.ManaBatteryItem;
import org.doothy.untitled.mana.network.ManaConsumerRegistry;
import org.doothy.untitled.screen.ManaFurnaceMenu;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public class ManaFurnaceBlockEntity extends BlockEntity
        implements ExtendedScreenHandlerFactory<BlockPos>,
        Container,
        ManaConsumer {

    /* ───────────────────────── Inventory ───────────────────────── */

    private final NonNullList<ItemStack> inventory =
            NonNullList.withSize(3, ItemStack.EMPTY);

    private static final int INPUT_SLOT = 0;
    private static final int FUEL_SLOT  = 1;
    private static final int OUTPUT_SLOT = 2;

    /* ───────────────────────── Mana Buffer ───────────────────────── */

    // Small local buffer – filled by batteries or network
    private final ManaAttachment buffer = new ManaAttachment(200);

    // Network delivery (one tick only)
    private int pendingNetworkMana = 0;

    private static final int MANA_COST_PER_TICK = 2;

    /* ───────────────────────── Smelting ───────────────────────── */

    private int progress = 0;
    private static final int MAX_PROGRESS = 100;

    /* ───────────────────────── GUI Sync ───────────────────────── */

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> MAX_PROGRESS;
                case 2 -> (int) buffer.getMana();
                case 3 -> (int) buffer.getMaxMana();
                default -> 0;
            };
        }

        @Override public void set(int i, int v) {
            if (i == 0) progress = v;
        }

        @Override public int getCount() {
            return 4;
        }
    };

    public ManaFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_FURNACE_BE, pos, state);
    }

    /* ───────────────────────── ManaConsumer ───────────────────────── */

    @Override
    public int getRequestedManaPerTick() {
        if (!hasRecipe()) return 0;

        long missing = MANA_COST_PER_TICK - buffer.getMana();
        return (int) Math.max(0, missing);
    }

    @Override
    public void acceptMana(int amount) {
        pendingNetworkMana += amount;
    }

    @Override
    public ManaStorage getBuffer() {
        return buffer;
    }

    @Override
    public @NonNull BlockPos getBlockPos() {
        return worldPosition;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    /* ───────────────────────── Tick ───────────────────────── */

    public static void tick(Level level, BlockPos pos, BlockState state,
                            ManaFurnaceBlockEntity be) {
        if (level.isClientSide()) return;

        boolean dirty = false;

        /* 1. Apply network delivery */
        if (be.pendingNetworkMana > 0) {
            be.buffer.insertMana(be.pendingNetworkMana, ManaTransaction.EXECUTE);
            be.pendingNetworkMana = 0;
            dirty = true;
        }

        /* 2. Drain battery → buffer */
        ItemStack fuel = be.inventory.get(FUEL_SLOT);
        if (!fuel.isEmpty() && fuel.getItem() instanceof ManaBatteryItem) {
            int stored = fuel.getOrDefault(Untitled.STORED_MANA, 0);
            if (stored > 0 && !be.buffer.isFull()) {
                int moved = (int) be.buffer.insertMana(
                        Math.min(10, stored),
                        ManaTransaction.EXECUTE
                );
                fuel.set(Untitled.STORED_MANA, stored - moved);
                dirty = true;
            }
        }

        /* 3. Smelting */
        if (be.hasRecipe()
                && be.buffer.extractMana(
                MANA_COST_PER_TICK, ManaTransaction.SIMULATE
        ) == MANA_COST_PER_TICK) {

            be.buffer.extractMana(MANA_COST_PER_TICK, ManaTransaction.EXECUTE);
            be.progress++;

            if (be.progress >= MAX_PROGRESS) {
                be.craftItem();
                be.progress = 0;
            }
            dirty = true;
        } else if (be.progress > 0) {
            be.progress = 0;
            dirty = true;
        }

        /* 4. Visual state */
        boolean lit = state.getValue(ManaFurnaceBlock.LIT);
        boolean shouldBeLit = be.progress > 0;

        if (lit != shouldBeLit) {
            level.setBlock(pos, state.setValue(ManaFurnaceBlock.LIT, shouldBeLit), 3);
        }

        if (dirty) setChanged(level, pos, state);
    }

    /* ───────────────────────── Recipes ───────────────────────── */

    private boolean hasRecipe() {
        if (level == null) return false;

        ItemStack input = inventory.get(INPUT_SLOT);
        if (input.isEmpty()) return false;

        SingleRecipeInput in = new SingleRecipeInput(input);
        Optional<RecipeHolder<SmeltingRecipe>> recipe =
                level.recipeAccess()
                        .getSynchronizedRecipes()
                        .getFirstMatch(RecipeType.SMELTING, in, level);

        if (recipe.isEmpty()) return false;

        ItemStack result =
                recipe.get().value().assemble(in, level.registryAccess());
        ItemStack output = inventory.get(OUTPUT_SLOT);

        return output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount()
                <= output.getMaxStackSize());
    }

    private void craftItem() {
        if (level == null) return;

        SingleRecipeInput in =
                new SingleRecipeInput(inventory.get(INPUT_SLOT));

        level.recipeAccess()
                .getSynchronizedRecipes()
                .getFirstMatch(RecipeType.SMELTING, in, level)
                .ifPresent(holder -> {
                    ItemStack result =
                            holder.value().assemble(in, level.registryAccess());
                    ItemStack output = inventory.get(OUTPUT_SLOT);

                    if (output.isEmpty()) {
                        inventory.set(OUTPUT_SLOT, result.copy());
                    } else {
                        output.grow(result.getCount());
                    }
                    inventory.get(INPUT_SLOT).shrink(1);
                });
    }

    /* ───────────────────────── Container / GUI ───────────────────────── */

    private boolean registered;
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);

        if (!registered && level instanceof ServerLevel serverLevel) {
            ManaConsumerRegistry.register(
                    serverLevel,
                    this,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ()
            );
            registered = true;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ManaConsumerRegistry.unregister(this);
    }
    @Override public int getContainerSize() { return inventory.size(); }
    @Override public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }
    @Override public ItemStack getItem(int slot) { return inventory.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(inventory, slot, amount);
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventory, slot);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        setChanged();
    }
    @Override public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }
    @Override public void clearContent() { inventory.clear(); }

    @Override
    public Component getDisplayName() {
        return Component.literal("Mana Furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int syncId, Inventory inv, Player player
    ) {
        return new ManaFurnaceMenu(syncId, inv, this, data);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }
}
