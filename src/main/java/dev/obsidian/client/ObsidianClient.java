package dev.obsidian.client;

import dev.obsidian.client.gui.MessengerScreen;
import dev.obsidian.client.gui.VaultUnlockScreen;
import dev.obsidian.client.util.LoggerHelper;
import dev.obsidian.storage.VaultManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.Screen;

/**
 * Client-side Mod Initializer for Obsidian Messenger.
 * Defers screen switching to END_CLIENT_TICK to prevent GPU surface acquisition crashes.
 */
public class ObsidianClient implements ClientModInitializer {
    private static volatile Screen pendingScreen = null;

    @Override
    public void onInitializeClient() {
        LoggerHelper.info("ObsidianClient", "Initializing Obsidian Messenger Client Engine & Commands...");

        // Safely process screen switching on END_CLIENT_TICK to prevent render pipeline conflicts
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingScreen != null && client.player != null) {
                Screen screenToOpen = pendingScreen;
                pendingScreen = null;
                try {
                    client.setScreenAndShow(screenToOpen);
                } catch (Exception e) {
                    LoggerHelper.error("ObsidianClient", "Error opening screen: " + e.getMessage());
                }
            }
        });

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
        if (VaultManager.isVaultUnlocked()) {
            pendingScreen = new MessengerScreen();
        } else {
            pendingScreen = new VaultUnlockScreen();
        }
    }
}
