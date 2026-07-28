package dev.obsidian.client;

import dev.obsidian.client.gui.MessengerScreen;
import dev.obsidian.client.gui.VaultUnlockScreen;
import dev.obsidian.client.util.LoggerHelper;
import dev.obsidian.storage.VaultManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Client-side Mod Initializer for Obsidian Messenger.
 * Safely handles screen transitions in Minecraft 26.2 via main-thread execution task queue.
 */
public class ObsidianClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LoggerHelper.info("ObsidianClient", "Initializing Obsidian Messenger Client Engine & Commands...");

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("om")
                .executes(ctx -> {
                    scheduleScreenOpen();
                    return 1;
                })
            );

            dispatcher.register(ClientCommands.literal("obsidian")
                .executes(ctx -> {
                    scheduleScreenOpen();
                    return 1;
                })
            );
        });
    }

    public static void scheduleScreenOpen() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            try {
                Screen target = VaultManager.isVaultUnlocked() ? new MessengerScreen() : new VaultUnlockScreen();
                client.setScreenAndShow(target);
            } catch (Exception e) {
                LoggerHelper.error("ObsidianClient", "Error opening screen: " + e.getMessage());
            }
        });
    }

    public static void scheduleScreenClose() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            try {
                client.setScreenAndShow(null);
            } catch (Exception e) {
                LoggerHelper.error("ObsidianClient", "Error closing screen: " + e.getMessage());
            }
        });
    }
}
