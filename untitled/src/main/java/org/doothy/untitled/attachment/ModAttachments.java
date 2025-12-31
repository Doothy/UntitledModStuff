package org.doothy.untitled.attachment;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

import static org.doothy.untitled.Untitled.MOD_ID;

/**
 * Registry class for all custom attachments in the mod.
 */
public final class ModAttachments {

    /**
     * The Mana attachment.
     * Stores mana on entities (like players).
     */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<ManaAttachment> MANA =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(MOD_ID, "mana"),
                    builder -> builder
                            .persistent(ManaAttachment.CODEC)
                            .copyOnDeath()
                            .initializer(() -> new ManaAttachment(100))
            );
    public static final AttachmentType<ShieldAttachment> LIGHTNING_SHIELD =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(MOD_ID, "lightning_shield"),
                    builder -> builder
                        .persistent(ShieldAttachment.CODEC)
                            .initializer(() -> new ShieldAttachment(0))
                    );

    /**
     * Initializes the attachments.
     */
    public static void initialize() {}

    private ModAttachments() {}
}
