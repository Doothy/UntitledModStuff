package org.doothy.untitled.attachment;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

import static org.doothy.untitled.Untitled.MOD_ID;

public class ModAttachments {
    // This is the "Key" we use to access attachments like Mana.
    // We make it "Persistent" so it saves to the player's .dat file automatically.
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<ManaAttachment> MANA = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(MOD_ID, "mana"),
            builder -> builder
                    .persistent(ManaAttachment.CODEC)
                    .copyOnDeath()
                    .initializer(() -> new ManaAttachment(100, 100))
                    .buildAndRegister(Identifier.fromNamespaceAndPath(MOD_ID, "mana"))
    );

    public static void initialize() {
        // simply forces the static field above to be registered.
    }
}
