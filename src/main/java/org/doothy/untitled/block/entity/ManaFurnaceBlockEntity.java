package org.doothy.untitled.block.entity;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.doothy.untitled.Untitled;
import org.doothy.untitled.api.mana.*;
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.items.ManaBatteryItem;
import org.doothy.untitled.screen.ManaFurnaceMenu;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ManaFurnaceBlockEntity extends BlockEntity
        implements ExtendedScreenHandlerFactory<BlockPos>,
        Container,
        ManaProducer,
        ManaConsumer {

    // Inventory
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);

    // Mana storage (single source of truth)
    private final ManaAttachment mana = new ManaAttachment(5_000);

    // Slots
    private static final int INPUT_SLOT = 0;
    private static final int FUEL_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;

    // Smelting
    private int progress = 0;
    private static final int MAX_PROGRESS = 100;
    private static final int MANA_COST_PER_TICK = 2;

    // GUI sync
    private final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> MAX_PROGRESS;
                case 2 -> (int) mana.getMana();
                case 3 -> (int) mana.getMaxMana();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ManaFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_FURNACE_BE, pos, state);
    }

    // ───────────────────────── API ─────────────────────────

    @Override
    public ManaStorage getManaInput() {
        return mana;
    }

    @Override
    public ManaStorage getManaOutput() {
        return mana;
    }

    // ───────────────────────── TICK ─────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, ManaFurnaceBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean dirty = false;

        // 1. BATTERY → FURNACE TRANSFER
        ItemStack fuel = entity.inventory.get(FUEL_SLOT);

        if (!fuel.isEmpty() && fuel.getItem() instanceof ManaBatteryItem) {
            int batteryMana = fuel.getOrDefault(Untitled.STORED_MANA, 0);

            if (batteryMana > 0 && !entity.mana.isFull()) {
                int transferRate = 10;

                long accepted = entity.mana.insertMana(
                        Math.min(transferRate, batteryMana),
                        ManaTransaction.EXECUTE
                );

                if (accepted > 0) {
                    fuel.set(Untitled.STORED_MANA, batteryMana - (int) accepted);
                    dirty = true;
                }
            }
        }

        // 2. SMELTING
        if (entity.hasRecipe()) {
            long extracted = entity.mana.extractMana(
                    MANA_COST_PER_TICK,
                    ManaTransaction.SIMULATE
            );

            if (extracted == MANA_COST_PER_TICK) {
                entity.mana.extractMana(MANA_COST_PER_TICK, ManaTransaction.EXECUTE);
                entity.progress++;

                if (entity.progress >= MAX_PROGRESS) {
                    entity.craftItem();
                    entity.progress = 0;
                }
                dirty = true;
            } else if (entity.progress > 0) {
                entity.progress = 0;
                dirty = true;
            }
        } else if (entity.progress > 0) {
            entity.progress = 0;
            dirty = true;
        }

        if (dirty) {
            setChanged(level, pos, state);
        }
    }

    // ───────────────────────── RECIPES ─────────────────────────

    private boolean hasRecipe() {
        if (level == null) return false;

        ItemStack input = inventory.get(INPUT_SLOT);
        if (input.isEmpty()) return false;

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<SmeltingRecipe>> recipe =
                level.recipeAccess()
                        .getSynchronizedRecipes()
                        .getFirstMatch(RecipeType.SMELTING, recipeInput, level);

        if (recipe.isEmpty()) return false;

        ItemStack result = recipe.get().value().assemble(recipeInput, level.registryAccess());
        ItemStack output = inventory.get(OUTPUT_SLOT);

        return output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    private void craftItem() {
        if (level == null) return;

        SingleRecipeInput input = new SingleRecipeInput(inventory.get(INPUT_SLOT));
        Optional<RecipeHolder<SmeltingRecipe>> recipe =
                level.recipeAccess()
                        .getSynchronizedRecipes()
                        .getFirstMatch(RecipeType.SMELTING, input, level);

        recipe.ifPresent(holder -> {
            ItemStack result = holder.value().assemble(input, level.registryAccess());
            ItemStack output = inventory.get(OUTPUT_SLOT);

            if (output.isEmpty()) {
                inventory.set(OUTPUT_SLOT, result.copy());
            } else {
                output.grow(result.getCount());
            }

            inventory.get(INPUT_SLOT).shrink(1);
        });
    }

    // ───────────────────────── SAVE / LOAD ─────────────────────────

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Mana", mana.getMana());
        output.putInt("Progress", progress);
        ContainerHelper.saveAllItems(output, inventory);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mana.insertMana(
                input.read("Mana", Codec.LONG).orElse(0L),
                ManaTransaction.EXECUTE
        );
        progress = input.read("Progress", Codec.INT).orElse(0);
        ContainerHelper.loadAllItems(input, inventory);
    }

    // ───────────────────────── CONTAINER ─────────────────────────
    public ContainerData getContainerData() {
        return this.propertyDelegate;
    }
    @Override public int getContainerSize() { return inventory.size(); }
    @Override public boolean isEmpty() { return inventory.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return inventory.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return ContainerHelper.removeItem(inventory, slot, amount); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(inventory, slot); }
    @Override public void setItem(int slot, ItemStack stack) { inventory.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { inventory.clear(); }

    @Override
    public Component getDisplayName() {
        return Component.literal("Mana Furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new ManaFurnaceMenu(syncId, playerInventory, this, propertyDelegate);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }
}
