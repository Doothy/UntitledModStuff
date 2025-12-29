package org.doothy.untitled.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.doothy.untitled.block.entity.ManaFurnaceBlockEntity;
import org.doothy.untitled.items.ManaBatteryItem;

/**
 * The menu handler for the Mana Furnace GUI.
 * Handles slot layout and data synchronization between server and client.
 */
public class ManaFurnaceMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    /**
     * Client-side constructor.
     *
     * @param syncId    The synchronization ID.
     * @param playerInv The player's inventory.
     * @param pos       The position of the block entity.
     */
    public ManaFurnaceMenu(int syncId, Inventory playerInv, BlockPos pos) {
        this(
                syncId,
                playerInv,
                playerInv.player.level().getBlockEntity(pos) instanceof Container container
                        ? container
                        : new SimpleContainer(3),
                playerInv.player.level().getBlockEntity(pos) instanceof ManaFurnaceBlockEntity be
                        ? be.getContainerData()
                        : new SimpleContainerData(4)
        );
    }

    /**
     * Server-side constructor.
     *
     * @param syncId          The synchronization ID.
     * @param playerInventory The player's inventory.
     * @param container       The container (block entity).
     * @param data            The data delegate for syncing integers.
     */
    public ManaFurnaceMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModScreenHandlers.MANA_FURNACE_MENU, syncId);
        this.container = container;
        this.data = data;

        checkContainerSize(container, 3);

        // Add Our Slots
        // Input
        this.addSlot(new Slot(container, 0, 56, 17));
        // Fuel (Battery)
        this.addSlot(new Slot(container, 1, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ManaBatteryItem;
            }
        });
        // Output
        this.addSlot(new Slot(container, 2, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        // Add Player Inventory
        addPlayerInventory(playerInventory);
        // Add Data Slots (for syncing progress bars)
        addDataSlots(data);
    }

    public int getProgress() { return this.data.get(0); }
    public int getMaxProgress() { return this.data.get(1); }
    public int getMana() { return this.data.get(2); }
    public int getMaxMana() { return this.data.get(3); }

    // Standard Shift-Click logic
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (index < 3) {
                if (!this.moveItemStackTo(originalStack, 3, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(originalStack, 0, 3, false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

}