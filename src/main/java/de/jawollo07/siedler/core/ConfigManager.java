package de.jawollo07.siedler.core;

import java.io.File;
import org.powernukkitx.utils.Config;

public class ConfigManager {
    private Config config;
    public void initialize(File dataFolder) {
        File configFile = new File(dataFolder, "config.yml");
        config = new Config(configFile, Config.YAML);
    }
    public void save() {
        if (config != null) {
            config.save();
        }
    }
    public Config getConfig() {
        return config;
    }
}
