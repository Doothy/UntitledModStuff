package org.doothy.untitled.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.doothy.untitled.api.mana.ManaProducer;
import org.doothy.untitled.api.mana.ManaStorage;
import org.doothy.untitled.api.mana.ManaTransaction;
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.mana.network.ManaProducerRegistry;
import org.doothy.untitled.mana.storage.BlockManaStorageComponent;
import org.doothy.untitled.mana.storage.ManaDataComponents;

import static net.minecraft.world.level.storage.loot.functions.SetComponentsFunction.setComponent;

public class ManaBatteryBlockEntity
        extends BlockEntity
        implements ManaProducer {

    // Internal mana storage
    private final ManaAttachment storage = new ManaAttachment(10000);

    private boolean registered;

    public ManaBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_BATTERY_BE, pos, state);
    }

    /* ───────────────── ManaProducer ───────────────── */

    @Override
    public int simulateExtract(int amount) {
        return (int) storage.extractMana(amount, ManaTransaction.SIMULATE);
    }

    @Override
    public int extract(int amount) {
        return (int) storage.extractMana(amount, ManaTransaction.EXECUTE);
    }

    @Override
    public ManaStorage getManaOutput() {
        return storage;
    }

    /* ───────────────── Lifecycle ───────────────── */

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);

        if (!registered && level instanceof ServerLevel serverLevel) {
            ManaProducerRegistry.register(
                    serverLevel,
                    this,
                    worldPosition
            );
            registered = true;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ManaProducerRegistry.unregister(this);
        registered = false;
    }
}




