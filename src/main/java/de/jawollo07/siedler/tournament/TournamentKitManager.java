package de.jawollo07.siedler.tournament;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.Player;

import java.lang.reflect.Method;
import java.util.*;

public final class TournamentKitManager {
    private final SiedlerPlugin plugin;
    private final Object config;

    public TournamentKitManager(SiedlerPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        this.config = plugin.getConfig();
    }

    public List<String> getKitNames() {
        Object kits = get("tournament.kits");
        if (!(kits instanceof Map<?, ?> map)) return List.of();
        List<String> names = new ArrayList<>();
        for (Object key : map.keySet()) if (key != null) names.add(String.valueOf(key));
        names.remove("default");
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public String getDefaultKit() {
        Object value = get("tournament.kits.default");
        return value == null ? "" : String.valueOf(value);
    }

    public boolean kitExists(String kit) {
        if (kit == null || kit.isBlank()) return false;
        return get("tournament.kits." + kit) instanceof Map<?, ?>;
    }

    public boolean createKit(String kit) {
        if (!validName(kit) || kitExists(kit)) return false;
        set("tournament.kits." + kit + ".commands", new ArrayList<String>());
        return save();
    }

    public boolean deleteKit(String kit) {
        if (!kitExists(kit)) return false;
        Object kits = get("tournament.kits");
        if (!(kits instanceof Map<?, ?> map)) return false;
        try {
            map.remove(kit);
            return save();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean addCommand(String kit, String command) {
        if (!kitExists(kit) || command == null || command.isBlank()) return false;
        List<String> commands = getCommands(kit);
        commands.add(command.trim());
        return set("tournament.kits." + kit + ".commands", commands) && save();
    }

    public boolean clearCommands(String kit) {
        if (!kitExists(kit)) return false;
        return set("tournament.kits." + kit + ".commands", new ArrayList<String>()) && save();
    }

    public List<String> getCommands(String kit) {
        Object value = get("tournament.kits." + kit + ".commands");
        if (!(value instanceof Collection<?> collection)) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (Object entry : collection) if (entry != null) result.add(String.valueOf(entry));
        return result;
    }

    public boolean apply(Player player, String kit) {
        if (kit == null || kit.isBlank() || !kitExists(kit)) return false;
        execute("clear " + player.getName());
        for (String command : getCommands(kit)) {
            if (!command.isBlank()) execute(command.replace("{player}", player.getName()));
        }
        return true;
    }

    private void execute(String command) {
        try {
            plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(),
                    command.replace("{player}", "").trim()
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Tournament kit command failed: " + command + " (" + e.getMessage() + ")");
        }
    }

    public Object getConfigValue(String path) {
        return get(path);
    }

    public boolean setConfigValue(String path, String raw) {
        if (path == null || path.isBlank() || !path.startsWith("tournament.")) return false;
        return set(path, parse(raw)) && save();
    }

    public boolean reloadConfig() {
        try {
            Method reload = config.getClass().getMethod("reload");
            reload.invoke(config);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Object parse(String raw) {
        if (raw == null) return "";
        if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) return Boolean.parseBoolean(raw);
        try { return Integer.parseInt(raw); } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(raw); } catch (NumberFormatException ignored) {}
        return raw;
    }

    private Object get(String path) {
        try {
            Method method = config.getClass().getMethod("get", String.class);
            return method.invoke(config, path);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean set(String path, Object value) {
        try {
            Method method = config.getClass().getMethod("set", String.class, Object.class);
            method.invoke(config, path, value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean save() {
        try {
            Method method = config.getClass().getMethod("save");
            method.invoke(config);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean validName(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{1,32}");
    }
}
