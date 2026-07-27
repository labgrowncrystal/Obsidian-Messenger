package dev.obsidian.client.gui;

import dev.obsidian.storage.VaultManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Vault Master Passphrase / PIN Unlock Screen for Obsidian Messenger.
 */
public class VaultUnlockScreen extends Screen {
    private EditBox passphraseInput;
    private Component statusMessage = Component.empty();

    public VaultUnlockScreen() {
        super(Component.translatable("obsidian.gui.unlock_title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.passphraseInput = new EditBox(this.font, centerX - 100, centerY - 10, 200, 20, Component.translatable("obsidian.gui.enter_passphrase"));
        this.passphraseInput.setMaxLength(64);
        this.addRenderableWidget(this.passphraseInput);

        this.addRenderableWidget(Button.builder(Component.translatable("obsidian.gui.unlock_btn"), button -> {
            attemptUnlock();
        }).bounds(centerX - 50, centerY + 20, 100, 20).build());
    }

    private void attemptUnlock() {
        String input = passphraseInput.getValue();
        if (input == null || input.trim().isEmpty()) {
            statusMessage = Component.literal("§cPlease enter a PIN or Passphrase.");
            return;
        }
        char[] pass = input.toCharArray();
        if (VaultManager.unlockVaultSession(pass, null)) {
            Minecraft.getInstance().setScreenAndShow(new MessengerScreen());
        } else {
            statusMessage = Component.literal("§cInvalid Master PIN / Passphrase!");
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(extractor, mouseX, mouseY, partialTick);
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        extractor.centeredText(this.font, this.title, centerX, centerY - 45, 0x00FF88);
        extractor.centeredText(this.font, Component.translatable("obsidian.gui.enter_passphrase"), centerX, centerY - 28, 0xAAAAAA);

        if (statusMessage != null) {
            extractor.centeredText(this.font, statusMessage, centerX, centerY + 48, 0xFF5555);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
