package org.doothy.untitled.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.doothy.untitled.screen.ManaFurnaceMenu;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * The screen for the Mana Furnace GUI.
 * Handles rendering of the background, progress bars, and tooltips.
 */
public class ManaFurnaceScreen extends AbstractContainerScreen<ManaFurnaceMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "untitled",
                    "textures/gui/mana_furnace.png"
            );

    // Smooth interpolation
    private float visualProgress = 0f;
    private float visualMana = 0f;

    public ManaFurnaceScreen(ManaFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.imageWidth = 176;
        this.imageHeight = 166;

        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    // ------------------------
    // Rendering
    // ------------------------

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderManaTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {

        int x = this.leftPos;
        int y = this.topPos;

        // ------------------------
        // Background
        // ------------------------
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                y,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256
        );

        // ============================================================
        // Progress Bar (HORIZONTAL — mask-based)
        // ============================================================

        if (menu.getMaxProgress() > 0) {
            int target = (menu.getProgress() * 32) / menu.getMaxProgress();
            visualProgress += (target - visualProgress) * 0.25f;
            int filled = Math.round(visualProgress);
            int remaining = 32 - filled;

            // Draw full bar
            guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    x + 78,
                    y + 41,
                    176,
                    0,
                    32,
                    4,
                    256,
                    256
            );

            // Mask unfilled part
            if (remaining > 0) {
                guiGraphics.fill(
                        x + 78 + filled,
                        y + 41,
                        x + 78 + 32,
                        y + 41 + 4,
                        0xFF000000 // black mask
                );
            }
        }
        // ============================================================
        // Mana Bar (VERTICAL — mask-based + pulse overlay)
        // ============================================================

        if (menu.getMaxMana() > 0) {
            int target = (menu.getMana() * 52) / menu.getMaxMana();
            visualMana += (target - visualMana) * 0.25f;
            int filled = Math.round(visualMana);
            int remaining = 52 - filled;

            // --- Draw full mana bar ---
            guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    x + 8,
                    y + 18,
                    176,
                    16,
                    8,
                    52,
                    256,
                    256
            );

            // --- Pulse overlay (white with animated alpha) ---
            if (filled > 0) {
                float pulse = getManaPulse();

                // Alpha between ~40 and ~80 (subtle!)
                int alpha = (int) (40 + pulse * 40);
                int color = (alpha << 24) | 0xFFFFFF;

                guiGraphics.fill(
                        x + 8,
                        y + 18 + remaining,
                        x + 16,
                        y + 18 + 52,
                        color
                );
            }

            // --- Mask unfilled part ---
            if (remaining > 0) {
                guiGraphics.fill(
                        x + 8,
                        y + 18,
                        x + 16,
                        y + 18 + remaining,
                        0xFF000000
                );
            }

            // --- Mana frame (no pulse) ---
            guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    x + 8,
                    y + 18,
                    184,
                    16,
                    8,
                    52,
                    256,
                    256
            );
        }
    }

    // ------------------------
    // Mana Tooltip
    // ------------------------

    private void renderManaTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        int barX1 = x + 8;
        int barX2 = barX1 + 8;
        int barY1 = y + 18;
        int barY2 = barY1 + 52;

        if (mouseX >= barX1 && mouseX <= barX2 &&
                mouseY >= barY1 && mouseY <= barY2) {

            List<ClientTooltipComponent> tooltip = List.of(
                    ClientTooltipComponent.create(
                            Component.literal(
                                    "Mana: " + menu.getMana() + "/" + menu.getMaxMana()
                            ).getVisualOrderText()
                    )
            );

            guiGraphics.renderTooltip(
                    this.font,
                    tooltip,
                    mouseX,
                    mouseY,
                    DefaultTooltipPositioner.INSTANCE,
                    null
            );
        }
    }
    private float getManaPulse() {
        // Speed of pulse (milliseconds)
        double time = System.currentTimeMillis() / 600.0;

        // Sin wave: 0.0 → 1.0 → 0.0
        return 0.5f + 0.5f * (float) Math.sin(time * Math.PI * 2);
    }
}
