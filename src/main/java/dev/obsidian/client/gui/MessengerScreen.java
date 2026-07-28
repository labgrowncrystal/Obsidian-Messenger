package dev.obsidian.client.gui;

import dev.obsidian.client.ObsidianClient;
import dev.obsidian.storage.VaultManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

/**
 * Main In-Game E2EE Messenger GUI Screen for Obsidian Messenger.
 * Features Sidebar for Contacts, Chat Window, Add Contact Modal, Personal Privacy Profile Modal, and Lock Vault action.
 * Uses Component.literal(...) for 100% reliable GuiGraphicsExtractor text rendering in Minecraft 26.2.
 */
public class MessengerScreen extends Screen {
    private EditBox messageInput;
    private Button sendBtn;
    private Button addContactBtn;
    private Button profileBtn;
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

    // Personal Privacy Profile Modal State & Widgets
    private boolean showProfileModal = false;
    private boolean isIpVisible = false;
    private Button toggleIpBtn;
    private Button copyIpBtn;
    private Button copyTokenBtn;
    private Button closeProfileBtn;
    private Component profileStatus = Component.empty();

    private final String myUsername;
    private final String myToken = "OM-8F4A-9B2C-7E1D";
    private final String myIpAddress = "192.168.1.15:25575";

    public MessengerScreen() {
        super(Component.translatable("obsidian.gui.title"));
        
        Minecraft mc = Minecraft.getInstance();
        myUsername = (mc != null && mc.getUser() != null) ? mc.getUser().getName() : "Player";

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

        // Sidebar Personal Profile Button
        this.profileBtn = Button.builder(Component.literal("Mein Profil"), button -> {
            openProfileModal();
        }).bounds(10, this.height - 48, sidebarWidth - 20, 18).build();
        this.addRenderableWidget(this.profileBtn);

        // Lock Vault / Logout Button
        this.lockVaultBtn = Button.builder(Component.literal("Lock Vault"), button -> {
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

        // Initialize Add Contact Modal Input Boxes & Buttons (Centered inside 240x170 Modal Box)
        int modalCenterX = this.width / 2;
        int modalCenterY = this.height / 2;

        this.modalNameInput = new EditBox(this.font, modalCenterX - 100, modalCenterY - 42, 200, 18, Component.literal("1. Name des Freundes (z.B. Alex)"));
        this.modalNameInput.setHint(Component.literal("1. Name des Freundes (z.B. Alex)"));
        this.modalNameInput.setMaxLength(32);
        this.modalNameInput.setVisible(false);
        this.addRenderableWidget(this.modalNameInput);

        this.modalTokenInput = new EditBox(this.font, modalCenterX - 100, modalCenterY + 1, 200, 18, Component.literal("2. IP oder Token (optional)"));
        this.modalTokenInput.setHint(Component.literal("2. IP oder Token (optional)"));
        this.modalTokenInput.setMaxLength(128);
        this.modalTokenInput.setVisible(false);
        this.addRenderableWidget(this.modalTokenInput);

        this.saveContactBtn = Button.builder(Component.literal("Kontakt Speichern"), button -> {
            saveNewContact();
        }).bounds(modalCenterX - 100, modalCenterY + 45, 95, 20).build();
        this.saveContactBtn.visible = false;
        this.addRenderableWidget(this.saveContactBtn);

        this.cancelContactBtn = Button.builder(Component.literal("Abbrechen"), button -> {
            closeAddContactModal();
        }).bounds(modalCenterX + 5, modalCenterY + 45, 95, 20).build();
        this.cancelContactBtn.visible = false;
        this.addRenderableWidget(this.cancelContactBtn);

        // Initialize Personal Profile Modal Widgets (Centered inside 260x190 Modal Box)
        this.copyTokenBtn = Button.builder(Component.literal("Token Kopieren"), button -> {
            copyToClipboard(myToken);
            profileStatus = Component.literal("§aToken in Zwischenablage kopiert!");
        }).bounds(modalCenterX - 100, modalCenterY - 20, 200, 18).build();
        this.copyTokenBtn.visible = false;
        this.addRenderableWidget(this.copyTokenBtn);

        this.toggleIpBtn = Button.builder(Component.literal("IP Anzeigen"), button -> {
            isIpVisible = !isIpVisible;
            toggleIpBtn.setMessage(Component.literal(isIpVisible ? "IP Verbergen" : "IP Anzeigen"));
        }).bounds(modalCenterX - 100, modalCenterY + 36, 95, 18).build();
        this.toggleIpBtn.visible = false;
        this.addRenderableWidget(this.toggleIpBtn);

        this.copyIpBtn = Button.builder(Component.literal("IP Kopieren"), button -> {
            copyToClipboard(myIpAddress);
            profileStatus = Component.literal("§aIP-Adresse in Zwischenablage kopiert!");
        }).bounds(modalCenterX + 5, modalCenterY + 36, 95, 18).build();
        this.copyIpBtn.visible = false;
        this.addRenderableWidget(this.copyIpBtn);

        this.closeProfileBtn = Button.builder(Component.literal("Schliessen"), button -> {
            closeProfileModal();
        }).bounds(modalCenterX - 50, modalCenterY + 62, 100, 18).build();
        this.closeProfileBtn.visible = false;
        this.addRenderableWidget(this.closeProfileBtn);
    }

    private void copyToClipboard(String text) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.keyboardHandler != null) {
                mc.keyboardHandler.setClipboard(text);
            }
        } catch (Exception ignored) {}

        try {
            StringSelection selection = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        } catch (Exception ignored) {}
    }

    private void openAddContactModal() {
        closeProfileModal();
        showAddContactModal = true;
        modalNameInput.setValue("");
        modalTokenInput.setValue("");
        modalStatus = Component.empty();
        
        modalNameInput.setVisible(true);
        modalTokenInput.setVisible(true);
        saveContactBtn.visible = true;
        cancelContactBtn.visible = true;

        messageInput.setVisible(false);
        sendBtn.visible = false;
        addContactBtn.visible = false;
        profileBtn.visible = false;
        lockVaultBtn.visible = false;

        this.setInitialFocus(modalNameInput);
    }

    private void closeAddContactModal() {
        showAddContactModal = false;
        
        modalNameInput.setVisible(false);
        modalTokenInput.setVisible(false);
        saveContactBtn.visible = false;
        cancelContactBtn.visible = false;

        messageInput.setVisible(true);
        sendBtn.visible = true;
        addContactBtn.visible = true;
        profileBtn.visible = true;
        lockVaultBtn.visible = true;

        this.setInitialFocus(messageInput);
    }

    private void openProfileModal() {
        closeAddContactModal();
        showProfileModal = true;
        isIpVisible = false;
        profileStatus = Component.empty();

        copyTokenBtn.visible = true;
        toggleIpBtn.visible = true;
        toggleIpBtn.setMessage(Component.literal("IP Anzeigen"));
        copyIpBtn.visible = true;
        closeProfileBtn.visible = true;

        messageInput.setVisible(false);
        sendBtn.visible = false;
        addContactBtn.visible = false;
        profileBtn.visible = false;
        lockVaultBtn.visible = false;
    }

    private void closeProfileModal() {
        showProfileModal = false;
        isIpVisible = false;

        copyTokenBtn.visible = false;
        toggleIpBtn.visible = false;
        copyIpBtn.visible = false;
        closeProfileBtn.visible = false;

        messageInput.setVisible(true);
        sendBtn.visible = true;
        addContactBtn.visible = true;
        profileBtn.visible = true;
        lockVaultBtn.visible = true;

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
        if (showAddContactModal || showProfileModal) return;
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
        if (!showAddContactModal && !showProfileModal && mouseX >= 5 && mouseX <= 140 && mouseY >= 60 && mouseY <= this.height - 55) {
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
            } else if (!showProfileModal) {
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
        extractor.centeredText(this.font, Component.translatable("obsidian.gui.title"), sidebarWidth / 2, 15, 0xFF00FF88);

        // Draw Contacts List in Sidebar dynamically
        int contactY = 60;
        for (VaultManager.Contact contact : contactsList) {
            String statusIcon = contact.favorite ? "* " : (contact.lastSeen > 0 ? "[ON] " : "[OFF] ");
            int color = contact.name.equals(selectedContact) ? 0xFF00FF88 : 0xFFCCCCCC;
            extractor.text(this.font, Component.literal(statusIcon + contact.name), 15, contactY, color);
            contactY += 20;
        }

        // Draw Main Chat Header & Window
        int chatX = sidebarWidth + 15;
        extractor.fill(sidebarWidth + 5, 5, this.width - 5, this.height - 35, 0xCC181822);
        extractor.text(this.font, Component.literal("Chat mit: §e" + selectedContact + " §8(AES-256-GCM E2EE)"), chatX, 15, 0xFFFFFFFF);
        extractor.fill(chatX, 28, this.width - 15, 29, 0x55555555);

        // Render Chat History Messages Stream
        int msgY = 35;
        for (String msg : chatMessages) {
            extractor.text(this.font, Component.literal(msg), chatX, msgY, 0xFFFFFFFF);
            msgY += 14;
        }

        // Render Add Contact Modal Dialog Overlay
        if (showAddContactModal) {
            int modalCenterX = this.width / 2;
            int modalCenterY = this.height / 2;
            int modalW = 240;
            int modalH = 170;

            // Modal Screen Veil & Dark Glass Panel Box
            extractor.fill(0, 0, this.width, this.height, 0xAA000000);
            extractor.fill(modalCenterX - (modalW / 2), modalCenterY - (modalH / 2), modalCenterX + (modalW / 2), modalCenterY + (modalH / 2), 0xFF181824);
            extractor.outline(modalCenterX - (modalW / 2), modalCenterY - (modalH / 2), modalW, modalH, 0xFF00FF88);

            extractor.centeredText(this.font, Component.literal("Neuer E2EE Kontakt"), modalCenterX, modalCenterY - 72, 0xFF00FF88);
            
            // Labels above input boxes using Component.literal
            extractor.text(this.font, Component.literal("1. Minecraft Name (z.B. Alex):"), modalCenterX - 100, modalCenterY - 55, 0xFFFFFFFF);
            extractor.text(this.font, Component.literal("2. IP-Adresse / Token (optional):"), modalCenterX - 100, modalCenterY - 12, 0xFFFFFFFF);

            if (modalStatus != null) {
                extractor.centeredText(this.font, modalStatus, modalCenterX, modalCenterY + 27, 0xFFFF5555);
            }
        }

        // Render Personal Privacy Profile Modal Overlay
        if (showProfileModal) {
            int modalCenterX = this.width / 2;
            int modalCenterY = this.height / 2;
            int modalW = 260;
            int modalH = 190;

            extractor.fill(0, 0, this.width, this.height, 0xAA000000);
            extractor.fill(modalCenterX - (modalW / 2), modalCenterY - (modalH / 2), modalCenterX + (modalW / 2), modalCenterY + (modalH / 2), 0xFF181824);
            extractor.outline(modalCenterX - (modalW / 2), modalCenterY - (modalH / 2), modalW, modalH, 0xFF00FF88);

            extractor.centeredText(this.font, Component.literal("Mein P2P Datenschutz Profil"), modalCenterX, modalCenterY - 82, 0xFF00FF88);
            extractor.text(this.font, Component.literal("Spieler: §e" + myUsername), modalCenterX - 100, modalCenterY - 66, 0xFFFFFFFF);

            // Token Display (Component.literal)
            extractor.text(this.font, Component.literal("Dein P2P Session Token:"), modalCenterX - 100, modalCenterY - 48, 0xFFAAAAAA);
            extractor.text(this.font, Component.literal("§a" + myToken), modalCenterX - 100, modalCenterY - 35, 0xFF00FF88);

            // IP Address Display with Privacy Masking by default (Component.literal)
            extractor.text(this.font, Component.literal("Deine IP-Adresse (Streamer Protection):"), modalCenterX - 100, modalCenterY + 8, 0xFFAAAAAA);
            String displayIp = isIpVisible ? myIpAddress : "****************";
            extractor.text(this.font, Component.literal("§d" + displayIp), modalCenterX - 100, modalCenterY + 21, 0xFF88FFFF);

            if (profileStatus != null) {
                extractor.centeredText(this.font, profileStatus, modalCenterX, modalCenterY - 7, 0xFF55FF55);
            }
        }

        // Render all widgets (buttons, input boxes) ON TOP of modal panels
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
