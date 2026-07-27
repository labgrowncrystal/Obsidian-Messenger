package dev.obsidian.client;

import dev.obsidian.ObsidianMod;
import net.fabricmc.api.ClientModInitializer;

public class ObsidianClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("[ObsidianMessenger] Initializing Client Engine & Keybinds...");
    }
}
