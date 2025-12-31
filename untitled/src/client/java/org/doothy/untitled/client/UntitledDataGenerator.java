package org.doothy.untitled.client;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Entry point for data generation.
 */
public class UntitledDataGenerator implements DataGeneratorEntrypoint {

    /**
     * Initializes the data generator.
     *
     * @param fabricDataGenerator The Fabric data generator instance.
     */
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
    }
}
