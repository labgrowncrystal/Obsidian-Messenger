package dev.obsidian.client;

import dev.obsidian.client.gui.MessengerScreen;
import dev.obsidian.client.gui.VaultUnlockScreen;
import dev.obsidian.client.util.LoggerHelper;
import dev.obsidian.storage.VaultManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;

/**
 * Client-side Mod Initializer for Obsidian Messenger.
 * Registers /om and /obsidian client commands to open the In-Game Messenger GUI Screen.
 */
public class ObsidianClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LoggerHelper.info("ObsidianClient", "Initializing Obsidian Messenger Client Engine & Commands...");

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("om")
                .executes(ctx -> {
                    Minecraft.getInstance().execute(() -> {
                        if (VaultManager.isVaultUnlocked()) {
                            Minecraft.getInstance().setScreenAndShow(new MessengerScreen());
                        } else {
                            Minecraft.getInstance().setScreenAndShow(new VaultUnlockScreen());
                        }
                    });
                    return 1;
                })
            );

            dispatcher.register(ClientCommands.literal("obsidian")
                .executes(ctx -> {
                    Minecraft.getInstance().execute(() -> {
                        if (VaultManager.isVaultUnlocked()) {
                            Minecraft.getInstance().setScreenAndShow(new MessengerScreen());
                        } else {
                            Minecraft.getInstance().setScreenAndShow(new VaultUnlockScreen());
                        }
                    });
                    return 1;
                })
            );
        });
    }
}
