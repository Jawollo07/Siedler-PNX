package de.jawollo07.siedler.core;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;

/** Coordinates shared services used by Siedler gameplay modules. */
public final class SiedlerManager {
    private final SiedlerPlugin plugin;
    private final StorageManager storage;

    public SiedlerManager(SiedlerPlugin plugin, StorageManager storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public SiedlerPlugin getPlugin() {
        return plugin;
    }

    public StorageManager getStorage() {
        return storage;
    }
}
