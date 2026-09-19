package de.jawollo07.siedler.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.powernukkitx.utils.Config;

public class MessageManager {
    private static Config config;
    private static final Pattern NUMERIC_YAML_KEY = Pattern.compile("^(\\s+)(\\d+):(?=\\s)", Pattern.MULTILINE);

    public void initialize(File dataFolder) {
        File configFile = new File(dataFolder, "messages.yml");
        copyDefaultsIfMissing(configFile);
        normalizeLegacyNumericKeys(configFile);
        config = new Config(configFile, Config.YAML);
        Config defaults = new Config(Config.YAML);
        try (InputStream input = MessageManager.class.getClassLoader().getResourceAsStream("messages.yml")) {
            if (input == null) {
                throw new IllegalStateException("Bundled messages.yml was not found");
            }
            defaults.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load bundled messages.yml", e);
        }
        if (config.setDefault(defaults.getRootSection()) > 0) {
            config.save();
        }
    }

    private void copyDefaultsIfMissing(File configFile) {
        if (configFile.isFile()) {
            return;
        }
        try (InputStream input = MessageManager.class.getClassLoader().getResourceAsStream("messages.yml")) {
            if (input == null) {
                throw new IllegalStateException("Bundled messages.yml was not found");
            }
            Files.copy(input, configFile.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create messages.yml", e);
        }
    }

    private void normalizeLegacyNumericKeys(File configFile) {
        if (!configFile.isFile()) {
            return;
        }
        try {
            String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            Matcher matcher = NUMERIC_YAML_KEY.matcher(content);
            StringBuffer normalized = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(normalized, Matcher.quoteReplacement(matcher.group(1) + "\"" + matcher.group(2) + "\":"));
            }
            matcher.appendTail(normalized);
            if (!content.equals(normalized.toString())) {
                Files.writeString(configFile.toPath(), normalized, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not migrate messages.yml", e);
        }
    }
    public void save() {
        if (config != null) {
            config.save();
        }
    }
    public Config getConfig() {
        return config;
    }
    public String getPrefix(String moduleName) {
        String path = "messages." + moduleName + ".prefix";
        return config.getString(path);
    }
    public String getMessage(String moduleName, String type) {
        String path = "messages." + moduleName + "." + type;
        return config.getString(path);
    }
    public String getMessageFromPath(String path) {
        return config.getString(path);
    }
    public String getCommandMessage(String type) {
        String path = "messages." + type;
        return config.getString(path);
    }
}
