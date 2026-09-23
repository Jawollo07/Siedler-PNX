package de.jawollo07.siedler.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.powernukkitx.utils.Config;

/** Loads and provides access to the plugin's messages.yml configuration. */
public class MessageManager {
    private static final Pattern NUMERIC_YAML_KEY = Pattern.compile("^(\\s+)(\\d+):(?=\\s|$)", Pattern.MULTILINE);
    private static final Pattern BOOLEAN_YAML_VALUE = Pattern.compile("^(\\s*(?:[^#\\n:]+:\\s*|-\\s+))(true|false|yes|no|on|off)(\\s*(?:#.*)?)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final String BUNDLED_MESSAGES = "messages.yml";

    private static volatile Config config;

    public synchronized void initialize(File dataFolder) {
        Objects.requireNonNull(dataFolder, "dataFolder");
        if (!dataFolder.exists() && !dataFolder.mkdirs() && !dataFolder.isDirectory()) {
            throw new IllegalStateException("Could not create plugin data folder: " + dataFolder);
        }
        File configFile = new File(dataFolder, BUNDLED_MESSAGES);
        copyDefaultsIfMissing(configFile);
        validateAndReportSyntax(configFile);
        normalizeLegacyNumericKeys(configFile);
        normalizeLegacyBooleanValues(configFile);

        Config loaded;
        try {
            loaded = new Config(configFile, Config.YAML);
        } catch (RuntimeException e) {
            throw new IllegalStateException(buildParserError(configFile, e), e);
        }
        Config defaults = loadBundledDefaults();
        loaded.setDefault(defaults.getRootSection());
        MessageManager.config = loaded;
        loaded.save();
    }

    private Config loadBundledDefaults() {
        Config defaults = new Config(Config.YAML);
        try (InputStream input = MessageManager.class.getClassLoader().getResourceAsStream(BUNDLED_MESSAGES)) {
            if (input == null) {
                throw new IllegalStateException("Bundled messages.yml was not found");
            }
            defaults.load(input);
            return defaults;
        } catch (IOException e) {
            throw new IllegalStateException("Could not load bundled messages.yml", e);
        }
    }

    private void copyDefaultsIfMissing(File configFile) {
        if (configFile.isFile()) return;
        try (InputStream input = MessageManager.class.getClassLoader().getResourceAsStream(BUNDLED_MESSAGES)) {
            if (input == null) {
                throw new IllegalStateException("Bundled messages.yml was not found");
            }
            Files.copy(input, configFile.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create messages.yml", e);
        }
    }

    private void validateAndReportSyntax(File configFile) {
        try {
            String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            String[] lines = content.split("\\R", -1);
            int booleanCount = 0;
            int numericKeyCount = 0;
            int tabCount = 0;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.startsWith("\uFEFF") && i == 0) {
                    throw new IllegalStateException("Invalid messages.yml syntax at line 1: UTF-8 BOM detected. Remove the BOM.");
                }
                if (line.indexOf('\t') >= 0) {
                    tabCount++;
                }
                if (NUMERIC_YAML_KEY.matcher(line).find()) {
                    numericKeyCount++;
                }
                if (BOOLEAN_YAML_VALUE.matcher(line).find()) {
                    booleanCount++;
                }
            }
            if (tabCount > 0) {
                throw new IllegalStateException("Invalid messages.yml indentation: " + tabCount
                        + " line(s) contain tab characters. Use spaces for YAML indentation.");
            }
            if (booleanCount > 0) {
                System.err.println("[Siedler] messages.yml: found " + booleanCount
                        + " unquoted boolean-like value(s); automatically converting them to strings.");
            }
            if (numericKeyCount > 0) {
                System.err.println("[Siedler] messages.yml: found " + numericKeyCount
                        + " numeric YAML key(s); automatically quoting them for PowerNukkitX compatibility.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read messages.yml for syntax validation: " + e.getMessage(), e);
        }
    }

    private String buildParserError(File configFile, RuntimeException cause) {
        StringBuilder message = new StringBuilder();
        message.append("Could not parse messages.yml: ").append(configFile.getAbsolutePath()).append('\\n');
        message.append("PowerNukkitX rejected the YAML syntax after compatibility normalization.").append('\\n');
        message.append("Check indentation (spaces only), matching quotes, ':' characters, and YAML structure.");
        if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
            message.append(" Parser: ").append(cause.getMessage());
        }
        try {
            String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            String[] lines = content.split("\\R", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.indexOf('\t') >= 0) {
                    message.append("\nPossible problem at line ").append(i + 1)
                            .append(": tab indentation is not valid YAML here.");
                    break;
                }
            }
        } catch (IOException ignored) {
            // Keep the original parser diagnostic if the file cannot be read again.
        }
        return message.toString();
    }

    private void normalizeLegacyNumericKeys(File configFile) {
        try {
            String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            Matcher matcher = NUMERIC_YAML_KEY.matcher(content);
            StringBuffer normalized = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(normalized, Matcher.quoteReplacement(
                        matcher.group(1) + "\"" + matcher.group(2) + "\":"));
            }
            matcher.appendTail(normalized);
            if (!content.equals(normalized.toString())) {
                Files.writeString(configFile.toPath(), normalized.toString(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not migrate messages.yml", e);
        }
    }

    private void normalizeLegacyBooleanValues(File configFile) {
        try {
            String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            Matcher matcher = BOOLEAN_YAML_VALUE.matcher(content);
            StringBuffer normalized = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(normalized, Matcher.quoteReplacement(
                        matcher.group(1) + "\"" + matcher.group(2) + "\"" + matcher.group(3)));
            }
            matcher.appendTail(normalized);
            if (!content.equals(normalized.toString())) {
                Files.writeString(configFile.toPath(), normalized.toString(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not migrate boolean values in messages.yml", e);
        }
    }

    public boolean isInitialized() {
        return config != null;
    }

    public void save() {
        Config current = config;
        if (current != null) current.save();
    }

    public Config getConfig() {
        return requireConfig();
    }

    public String getPrefix(String moduleName) {
        return getString("messages." + requirePathPart(moduleName, "moduleName") + ".prefix", "");
    }

    public String getMessage(String moduleName, String type) {
        return getString("messages." + requirePathPart(moduleName, "moduleName") + "."
                + requirePathPart(type, "type"), "[Fehlende Nachricht: " + moduleName + "." + type + "]");
    }

    public String getMessageFromPath(String path) {
        return getString(requirePathPart(path, "path"), "[Fehlende Nachricht: " + path + "]");
    }

    public String getMessage(String path) {
        return getMessageFromPath(path);
    }

    public String getCommandMessage(String type) {
        return getString("messages." + requirePathPart(type, "type"), "[Fehlende Nachricht: " + type + "]");
    }

    private String getString(String path, String fallback) {
        Config current = requireConfig();
        String value = current.getString(path);
        return value == null || value.isBlank() ? fallback : value;
    }

    private Config requireConfig() {
        Config current = config;
        if (current == null) {
            throw new IllegalStateException("MessageManager has not been initialized. Call initialize() first.");
        }
        return current;
    }

    private String requirePathPart(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value.trim();
    }
}
