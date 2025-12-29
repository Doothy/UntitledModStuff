package org.doothy.untitled.attachment;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

import static org.doothy.untitled.Untitled.MOD_ID;

public final class ModAttachments {

    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<ManaAttachment> MANA =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(MOD_ID, "mana"),
                    builder -> builder
                            .persistent(ManaAttachment.CODEC)
                            .copyOnDeath()
                            .initializer(() -> new ManaAttachment(100))
            );

    public static void initialize() {}

    private ModAttachments() {}
}
