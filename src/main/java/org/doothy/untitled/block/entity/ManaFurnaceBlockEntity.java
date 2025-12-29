package org.doothy.untitled.block.entity;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
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
import org.doothy.untitled.items.ManaBatteryItem;
import org.doothy.untitled.screen.ManaFurnaceMenu;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The BlockEntity for the Mana Furnace.
 * Handles inventory, mana storage, and smelting logic.
 */
public class ManaFurnaceBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos>, Container {

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);

    // Slots
    private static final int INPUT_SLOT = 0;
    private static final int FUEL_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;

    // Stats
    private int progress = 0;
    private int maxProgress = 100; // 5 seconds (Vanilla is 200/10s). Faster!
    private int manaStored = 0;
    private final int MAX_MANA = 5000;
    private final int MANA_COST_PER_TICK = 2; // Cost to run

    // Syncing data to GUI
    protected final ContainerData propertyDelegate;

    public ManaFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_FURNACE_BE, pos, state);

        // This delegate allows the Screen to read these integers
        this.propertyDelegate = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> ManaFurnaceBlockEntity.this.progress;
                    case 1 -> ManaFurnaceBlockEntity.this.maxProgress;
                    case 2 -> ManaFurnaceBlockEntity.this.manaStored;
                    case 3 -> ManaFurnaceBlockEntity.this.MAX_MANA;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> ManaFurnaceBlockEntity.this.progress = value;
                    case 1 -> ManaFurnaceBlockEntity.this.maxProgress = value;
                    case 2 -> ManaFurnaceBlockEntity.this.manaStored = value;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    /**
     * Ticks the block entity.
     * Handles mana recharging from batteries and smelting items.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, ManaFurnaceBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean isDirty = false;

        // 1. MANA RECHARGING LOGIC
        // We look at the item in the fuel slot (1)
        ItemStack fuelStack = entity.inventory.get(FUEL_SLOT);

        // Check if it's a battery and if we have space for mana
        if (!fuelStack.isEmpty() && fuelStack.getItem() instanceof ManaBatteryItem batteryItem && entity.manaStored < entity.MAX_MANA) {

            // Get current mana in the BATTERY
            int batteryMana = fuelStack.getOrDefault(Untitled.STORED_MANA, 0);

            if (batteryMana > 0) {
                // Determine how much to move:
                // Don't take more than the battery has, don't take more than the furnace needs, limit by transfer rate
                // Hardcoding transfer rate to 10 for the furnace input
                int transferRate = 10;
                int spaceInFurnace = entity.MAX_MANA - entity.manaStored;
                int amountToTake = Math.min(transferRate, Math.min(batteryMana, spaceInFurnace));

                if (amountToTake > 0) {
                    // Add to Furnace
                    entity.manaStored += amountToTake;

                    // Remove from Battery Item
                    fuelStack.set(Untitled.STORED_MANA, batteryMana - amountToTake);

                    isDirty = true;
                }
            }
        }

        // 2. SMELTING LOGIC
        if (entity.hasRecipe()) {
            // Check if we have enough mana to run this tick
            if (entity.manaStored >= entity.MANA_COST_PER_TICK) {
                entity.manaStored -= entity.MANA_COST_PER_TICK;
                entity.progress++;

                if (entity.progress >= entity.maxProgress) {
                    entity.craftItem();
                    entity.progress = 0;
                }
                isDirty = true;
            } else {
                entity.progress = 0;
                isDirty = true;
            }
        } else {
            // Reset progress if recipe is invalid or inputs removed
            if (entity.progress > 0) {
                entity.progress = 0;
                isDirty = true;
            }
        }

        if (isDirty) {
            setChanged(level, pos, state);
        }
    }

    /**
     * Checks if the current input item has a valid smelting recipe.
     */
    private boolean hasRecipe() {
        if (this.level == null || this.level.isClientSide()) return false;

        ItemStack input = inventory.get(INPUT_SLOT);
        if (input.isEmpty()) return false;

        ItemStack output = inventory.get(OUTPUT_SLOT);

        // Use the SingleRecipeInput helper
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);

        Optional<RecipeHolder<SmeltingRecipe>> recipe = this.level.recipeAccess()
                .getSynchronizedRecipes()
                .getFirstMatch(RecipeType.SMELTING, recipeInput, this.level);

        if (recipe.isEmpty()) return false;

        // FIX: Use assemble() instead of getResultItem()
        ItemStack result = recipe.get().value().assemble(recipeInput, this.level.registryAccess());

        return output.isEmpty() ||
                (output.getItem() == result.getItem() && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    public ContainerData getPropertyDelegate() {
        return this.propertyDelegate;
    }

    /**
     * Crafts the item and updates the inventory.
     */
    private void craftItem() {
        if (this.level == null || this.level.isClientSide()) return;

        // Create the input object once
        SingleRecipeInput recipeInput = new SingleRecipeInput(inventory.get(INPUT_SLOT));

        Optional<RecipeHolder<SmeltingRecipe>> recipe = this.level.recipeAccess()
                .getSynchronizedRecipes()
                .getFirstMatch(RecipeType.SMELTING, recipeInput, this.level);

        if (recipe.isPresent()) {
            // FIX: Use assemble() here as well
            ItemStack result = recipe.get().value().assemble(recipeInput, this.level.registryAccess());

            ItemStack outputStack = inventory.get(OUTPUT_SLOT);

            if (outputStack.isEmpty()) {
                inventory.set(OUTPUT_SLOT, result.copy());
            } else {
                outputStack.grow(result.getCount());
            }

            inventory.get(INPUT_SLOT).shrink(1);
        }
    }

    // --- Saving & Loading ---
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        // 1. Save Integers
        output.putInt("mana", this.manaStored);
        output.putInt("progress", this.progress);

        // 2. Save Inventory
        // ContainerHelper now writes directly to the output stream.
        // It likely uses the standard "Items" key automatically.
        ContainerHelper.saveAllItems(output, this.inventory);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.manaStored = input.read("mana", Codec.INT).orElse(0);
        this.progress = input.read("progress", Codec.INT).orElse(0);

        ContainerHelper.loadAllItems(input, this.inventory);
    }

    // --- Container Methods ---

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
        return new ManaFurnaceMenu(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.worldPosition;
    }
}