package dev.obsidian;

import net.fabricmc.api.ModInitializer;

public class ObsidianMod implements ModInitializer {
    public static final String MOD_ID = "obsidian";
    public static final int DEFAULT_PORT = 49156;

    @Override
    public void onInitialize() {
        System.out.println("[ObsidianMessenger] Zero-Server P2P E2EE In-Game Messenger initialized.");
    }
}
