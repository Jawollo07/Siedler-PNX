package de.jawollo07.siedler.eco;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;

public class TaxManager {
    private final StorageManager storageManager;
    public TaxManager(SiedlerPlugin plugin) {
        this.storageManager = plugin.getStorage();
    }
    public Integer getTeamTax(String teamID) {
        
        return 0;
    }
}
