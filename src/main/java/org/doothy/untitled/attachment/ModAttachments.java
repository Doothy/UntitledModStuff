package org.doothy.untitled.attachment;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

import static org.doothy.untitled.Untitled.MOD_ID;

/**
 * Registry class for all custom data attachments in the mod.
 */
public class ModAttachments {
    /**
     * The Mana attachment type.
     * Persistent (saves to disk), copies on death, and initializes with 100/100 mana.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<ManaAttachment> MANA = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(MOD_ID, "mana"),
            builder -> builder
                    .persistent(ManaAttachment.CODEC)
                    .copyOnDeath()
                    .initializer(() -> new ManaAttachment(100, 100))
                    .buildAndRegister(Identifier.fromNamespaceAndPath(MOD_ID, "mana"))
    );

    /**
     * Initializes the attachments.
     */
    public static void initialize() {
        // simply forces the static field above to be registered.
    }
}