package org.doothy.untitled.attachment;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

import static org.doothy.untitled.Untitled.MOD_ID;

/**
 * Declares and registers all custom attachments used by the mod.
 */
public final class ModAttachments {

    /**
     * Attachment storing per-entity mana state (e.g., for players).
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

    /**
     * Attachment storing a temporary lightning shield state (remaining ticks).
     */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<ShieldAttachment> LIGHTNING_SHIELD =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(MOD_ID, "lightning_shield"),
                    builder -> builder
                            .persistent(ShieldAttachment.CODEC)
                            .initializer(() -> new ShieldAttachment(0))
            );

    /**
     * No-op initializer; present for symmetry with other registries.
     */
    public static void initialize() {}

    private ModAttachments() {}
}
