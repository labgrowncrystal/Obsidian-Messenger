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
 * Features Sidebar for Contacts, Chat Window, Text Input, Add Contact Modal, and Lock Vault action.
 */
public class MessengerScreen extends Screen {
    private EditBox messageInput;
    private Button sendBtn;
    private Button addContactBtn;
    private Button lockVaultBtn;

    private final List<String> chatMessages = new ArrayList<>();
    private final List<VaultManager.Contact> contactsList = new ArrayList<>();
    private String selectedContact = "Alex";

    // Add Contact Modal State & Input Widgets
    private boolean showAddContactModal = false;
    private EditBox modalNameInput;
    private EditBox modalTokenInput;
    private Button saveContactBtn;
    private Button cancelContactBtn;
    private Component modalStatus = Component.empty();

    public MessengerScreen() {
        super(Component.translatable("obsidian.gui.title"));
        
        // Mock sample contacts
        contactsList.add(new VaultManager.Contact("1", "Alex", "192.168.1.10:25575", "", true, System.currentTimeMillis()));
        contactsList.add(new VaultManager.Contact("2", "Bob", "192.168.1.12:25575", "", false, System.currentTimeMillis() - 3600000));
        contactsList.add(new VaultManager.Contact("3", "Charlie", "192.168.1.15:25575", "", false, 0));

        chatMessages.add("§8[19:42] §eAlex: §fHey! Are you in the Nether fortress?");
        chatMessages.add("§8[19:43] §dYou: §fYeah, collecting blaze rods.");
        chatMessages.add("§8[19:44] §eAlex: §fAwesome, bring me a few!");
    }

    @Override
    protected void init() {
        int sidebarWidth = 140;
        int inputHeight = 20;

        // Sidebar Add Contact Button
        this.addContactBtn = Button.builder(Component.translatable("obsidian.gui.add_contact"), button -> {
            openAddContactModal();
        }).bounds(10, 35, sidebarWidth - 20, 18).build();
        this.addRenderableWidget(this.addContactBtn);

        // Lock Vault / Logout Button
        this.lockVaultBtn = Button.builder(Component.literal("🔒 Lock Vault"), button -> {
            VaultManager.lockVaultSession();
            ObsidianClient.scheduleScreenClose();
        }).bounds(10, this.height - 25, sidebarWidth - 20, 18).build();
        this.addRenderableWidget(this.lockVaultBtn);

        // Main Chat Input Box
        int chatX = sidebarWidth + 15;
        int chatWidth = this.width - chatX - 75;
        int chatY = this.height - 30;

        this.messageInput = new EditBox(this.font, chatX, chatY, chatWidth, inputHeight, Component.translatable("obsidian.gui.input_placeholder"));
        this.messageInput.setMaxLength(500);
        this.setInitialFocus(this.messageInput);
        this.addRenderableWidget(this.messageInput);

        // Send Button
        this.sendBtn = Button.builder(Component.translatable("obsidian.gui.send_btn"), button -> {
            sendMessage();
        }).bounds(chatX + chatWidth + 5, chatY, 60, inputHeight).build();
        this.addRenderableWidget(this.sendBtn);

        // Initialize Modal Input Boxes & Buttons (Centered inside 240x145 Modal Box)
        int modalCenterX = this.width / 2;
        int modalCenterY = this.height / 2;

        this.modalNameInput = new EditBox(this.font, modalCenterX - 100, modalCenterY - 38, 200, 18, Component.literal("Spielername (z.B. Alex)"));
        this.modalNameInput.setMaxLength(32);
        this.modalNameInput.setVisible(false);
        this.addRenderableWidget(this.modalNameInput);

        this.modalTokenInput = new EditBox(this.font, modalCenterX - 100, modalCenterY + 5, 200, 18, Component.literal("IP oder Token (z.B. 192.168.1.5)"));
        this.modalTokenInput.setMaxLength(128);
        this.modalTokenInput.setVisible(false);
        this.addRenderableWidget(this.modalTokenInput);

        this.saveContactBtn = Button.builder(Component.literal("Kontakt Speichern"), button -> {
            saveNewContact();
        }).bounds(modalCenterX - 100, modalCenterY + 33, 95, 20).build();
        this.saveContactBtn.visible = false;
        this.addRenderableWidget(this.saveContactBtn);

        this.cancelContactBtn = Button.builder(Component.literal("Abbrechen"), button -> {
            closeAddContactModal();
        }).bounds(modalCenterX + 5, modalCenterY + 33, 95, 20).build();
        this.cancelContactBtn.visible = false;
        this.addRenderableWidget(this.cancelContactBtn);
    }

    private void openAddContactModal() {
        showAddContactModal = true;
        modalNameInput.setValue("");
        modalTokenInput.setValue("");
        modalStatus = Component.empty();
        
        modalNameInput.setVisible(true);
        modalTokenInput.setVisible(true);
        saveContactBtn.visible = true;
        cancelContactBtn.visible = true;

        // Disable underlying chat controls while modal is open
        messageInput.setVisible(false);
        sendBtn.visible = false;
        addContactBtn.active = false;
        lockVaultBtn.active = false;

        this.setInitialFocus(modalNameInput);
    }

    private void closeAddContactModal() {
        showAddContactModal = false;
        
        modalNameInput.setVisible(false);
        modalTokenInput.setVisible(false);
        saveContactBtn.visible = false;
        cancelContactBtn.visible = false;

        // Re-enable underlying chat controls
        messageInput.setVisible(true);
        sendBtn.visible = true;
        addContactBtn.active = true;
        lockVaultBtn.active = true;

        this.setInitialFocus(messageInput);
    }

    private void saveNewContact() {
        String name = modalNameInput.getValue();
        String token = modalTokenInput.getValue();

        if (name == null || name.trim().isEmpty()) {
            modalStatus = Component.literal("§cBitte gib den Spielernamen deines Freundes ein!");
            return;
        }

        if (token == null || token.trim().isEmpty()) {
            // Default fallback if left blank
            token = "P2P-Local";
        }

        String contactId = "cnt_" + System.currentTimeMillis();
        VaultManager.Contact newContact = new VaultManager.Contact(contactId, name.trim(), token.trim(), "", false, System.currentTimeMillis());
        contactsList.add(newContact);
        selectedContact = newContact.name;

        closeAddContactModal();
        chatMessages.add("§8[System] §aNeuer Kontakt hinzugefügt: §e" + newContact.name);
    }

    private void sendMessage() {
        if (showAddContactModal) return;
        String text = messageInput.getValue();
        if (text != null && !text.trim().isEmpty()) {
            chatMessages.add("§8[Now] §dYou: §f" + text.trim());
            messageInput.setValue("");
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isFocused) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (!showAddContactModal && mouseX >= 5 && mouseX <= 140 && mouseY >= 60) {
            int index = (int) ((mouseY - 60) / 20);
            if (index >= 0 && index < contactsList.size()) {
                selectedContact = contactsList.get(index).name;
                return true;
            }
        }
        return super.mouseClicked(event, isFocused);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent keyEvent) {
        if (keyEvent.key() == 257 || keyEvent.key() == 335) { // Enter key
            if (showAddContactModal) {
                saveNewContact();
            } else {
                sendMessage();
            }
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Render dark obsidian overlay background
        extractor.fill(0, 0, this.width, this.height, 0xEE0B0B10);

        int sidebarWidth = 140;

        // Draw Left Sidebar Panel Box (Dark Obsidian Overlay)
        extractor.fill(5, 5, sidebarWidth, this.height - 5, 0xDD111118);
        extractor.centeredText(this.font, Component.translatable("obsidian.gui.contacts"), sidebarWidth / 2, 15, 0x00FF88);

        // Draw Contacts List in Sidebar dynamically
        int contactY = 60;
        for (VaultManager.Contact contact : contactsList) {
            String statusIcon = contact.favorite ? "⭐ " : (contact.lastSeen > 0 ? "🟢 " : "🔴 ");
            int color = contact.name.equals(selectedContact) ? 0x00FF88 : 0xCCCCCC;
            extractor.text(this.font, statusIcon + contact.name, 15, contactY, color);
            contactY += 20;
        }

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

        // Render Add Contact Modal Dialog Overlay BEFORE super.extractRenderState so widgets render ON TOP!
        if (showAddContactModal) {
            int modalCenterX = this.width / 2;
            int modalCenterY = this.height / 2;
            int modalW = 240;
            int modalH = 145;

            // Modal Screen Veil & Dark Glass Panel Box
            extractor.fill(0, 0, this.width, this.height, 0xAA000000);
            extractor.fill(modalCenterX - (modalW / 2), modalCenterY - (modalH / 2), modalCenterX + (modalW / 2), modalCenterY + (modalH / 2), 0xFF181824);
            extractor.outline(modalCenterX - (modalW / 2), modalCenterY - (modalH / 2), modalW, modalH, 0xFF00FF88);

            extractor.centeredText(this.font, "➕ Neuer E2EE Kontakt", modalCenterX, modalCenterY - 60, 0x00FF88);
            
            // Clear Labels above input boxes
            extractor.text(this.font, "1. Minecraft Name (z.B. Alex):", modalCenterX - 100, modalCenterY - 50, 0xAAAAAA);
            extractor.text(this.font, "2. IP-Adresse / Token (optional):", modalCenterX - 100, modalCenterY - 7, 0xAAAAAA);

            if (modalStatus != null) {
                extractor.centeredText(this.font, modalStatus, modalCenterX, modalCenterY + 58, 0xFF5555);
            }
        }

        // Render all widgets (buttons, input boxes) ON TOP of the modal panel
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
