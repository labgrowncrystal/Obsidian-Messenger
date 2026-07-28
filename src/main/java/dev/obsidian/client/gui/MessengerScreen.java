package dev.obsidian.client.gui;

import dev.obsidian.client.ObsidianClient;
import dev.obsidian.storage.VaultManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Main In-Game E2EE Messenger GUI Screen for Obsidian Messenger.
 * Features Sidebar for Contacts, Chat Window, Text Input, and Lock Vault action.
 */
public class MessengerScreen extends Screen {
    private EditBox messageInput;
    private final List<String> chatMessages = new ArrayList<>();
    private String selectedContact = "Alex";

    public MessengerScreen() {
        super(Component.translatable("obsidian.gui.title"));
        chatMessages.add("§8[19:42] §eAlex: §fHey! Are you in the Nether fortress?");
        chatMessages.add("§8[19:43] §dYou: §fYeah, collecting blaze rods.");
        chatMessages.add("§8[19:44] §eAlex: §fAwesome, bring me a few!");
    }

    @Override
    protected void init() {
        int sidebarWidth = 140;
        int inputHeight = 20;

        // Sidebar Add Contact Button
        this.addRenderableWidget(Button.builder(Component.translatable("obsidian.gui.add_contact"), button -> {
            // Modal for Add Contact
        }).bounds(10, 35, sidebarWidth - 20, 18).build());

        // Lock Vault / Logout Button (Security Memory Hygiene + Deferred Screen Close)
        this.addRenderableWidget(Button.builder(Component.literal("🔒 Lock Vault"), button -> {
            VaultManager.lockVaultSession();
            ObsidianClient.scheduleScreenClose();
        }).bounds(10, this.height - 25, sidebarWidth - 20, 18).build());

        // Chat Input Box
        int chatX = sidebarWidth + 15;
        int chatWidth = this.width - chatX - 75;
        int chatY = this.height - 30;

        this.messageInput = new EditBox(this.font, chatX, chatY, chatWidth, inputHeight, Component.translatable("obsidian.gui.input_placeholder"));
        this.messageInput.setMaxLength(500);
        this.addRenderableWidget(this.messageInput);

        // Send Button
        this.addRenderableWidget(Button.builder(Component.translatable("obsidian.gui.send_btn"), button -> {
            sendMessage();
        }).bounds(chatX + chatWidth + 5, chatY, 60, inputHeight).build());
    }

    private void sendMessage() {
        String text = messageInput.getValue();
        if (text != null && !text.trim().isEmpty()) {
            chatMessages.add("§8[Now] §dYou: §f" + text.trim());
            messageInput.setValue("");
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent keyEvent) {
        if (keyEvent.key() == 257 || keyEvent.key() == 335) { // Enter key
            sendMessage();
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(extractor, mouseX, mouseY, partialTick);

        int sidebarWidth = 140;

        // Draw Left Sidebar Panel Box (Dark Obsidian Overlay)
        extractor.fill(5, 5, sidebarWidth, this.height - 5, 0xDD111118);
        extractor.centeredText(this.font, Component.translatable("obsidian.gui.contacts"), sidebarWidth / 2, 15, 0x00FF88);

        // Draw Contacts List in Sidebar
        extractor.text(this.font, "🟢 Alex (Online)", 15, 60, 0x55FF55);
        extractor.text(this.font, "💤 Bob (AFK)", 15, 80, 0xFFFF55);
        extractor.text(this.font, "🔴 Charlie (Offline)", 15, 100, 0x888888);

        // Draw Main Chat Header & Window
        int chatX = sidebarWidth + 15;
        extractor.fill(sidebarWidth + 5, 5, this.width - 5, this.height - 35, 0xCC181822);
        extractor.text(this.font, "💬 Chat with: §e" + selectedContact + " §8(AES-256-GCM E2EE)", chatX, 15, 0xFFFFFF);
        extractor.fill(chatX, 28, this.width - 15, 29, 0x55555555);

        // Render Chat History Messages Stream
        int msgY = 35;
        for (String msg : chatMessages) {
            extractor.text(this.font, msg, chatX, msgY, 0xFFFFFF);
            msgY += 14;
        }

        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
