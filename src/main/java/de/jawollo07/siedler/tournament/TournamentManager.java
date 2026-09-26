package de.jawollo07.siedler.tournament;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.core.MessageManager;
import org.powernukkitx.Player;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.entity.EntityDamageEvent;
import org.powernukkitx.event.player.PlayerDeathEvent;
import org.powernukkitx.event.player.PlayerQuitEvent;
import org.powernukkitx.utils.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TournamentManager implements Listener {
    public enum State { IDLE, REGISTRATION, COUNTDOWN, RUNNING, FINISHED }
    private record Participant(UUID uuid, String name) {}
    private record Match(UUID id, UUID a, UUID b, int round) {}

    private final SiedlerPlugin plugin;
    private final MessageManager messages = new MessageManager();
    private final Config config;
    private final TournamentKitManager kits;
    private final Map<UUID, String> selectedKits = new ConcurrentHashMap<>();
    private final Map<UUID, Participant> participants = new LinkedHashMap<>();
    private final Map<UUID, Match> matches = new ConcurrentHashMap<>();
    private final Set<UUID> spectators = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> wins = new ConcurrentHashMap<>();
    private final List<UUID> roundWinners = new ArrayList<>();
    private int pendingMatches;
    private int round;
    private State state = State.IDLE;
    private UUID winner;

    public TournamentManager(SiedlerPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        this.config = plugin.getConfig();
        this.kits = new TournamentKitManager(plugin);
    }

    public State getState() { return state; }
    public int getParticipantCount() { return participants.size(); }
    public int getMaxParticipants() { return maxParticipants(); }

    public synchronized boolean openRegistration() {
        if (!enabled() || state == State.REGISTRATION || state == State.COUNTDOWN || state == State.RUNNING) return false;
        participants.clear(); matches.clear(); spectators.clear(); wins.clear(); roundWinners.clear(); selectedKits.clear();
        pendingMatches = 0; round = 0; winner = null; state = State.REGISTRATION;
        broadcast(msg("registration-open")
                .replace("{seconds}", String.valueOf(config.getInt("tournament.registration.duration-seconds", 120)))
                .replace("{min}", String.valueOf(minParticipants()))
                .replace("{max}", String.valueOf(maxParticipants())));
        plugin.getServer().getScheduler().scheduleDelayedTask(this::registrationTimeout,
                Math.max(20, config.getInt("tournament.registration.duration-seconds", 120) * 20));
        return true;
    }

    public synchronized boolean join(Player player) {
        if (!enabled()) { tell(player, "disabled"); return false; }
        if (state != State.REGISTRATION) { tell(player, "registration-closed"); return false; }
        if (participants.containsKey(player.getUniqueId())) { tell(player, "already-joined"); return false; }
        if (participants.size() >= maxParticipants()) { tell(player, "full"); return false; }
        participants.put(player.getUniqueId(), new Participant(player.getUniqueId(), player.getName()));
        wins.put(player.getUniqueId(), 0);
        String defaultKit = kits.getDefaultKit();
        if (!defaultKit.isBlank()) selectedKits.put(player.getUniqueId(), defaultKit);
        broadcast(msg("joined").replace("{player}", player.getName())
                .replace("{count}", String.valueOf(participants.size()))
                .replace("{max}", String.valueOf(maxParticipants())));
        if (config.getBoolean("tournament.registration.auto-start-when-full", true)
                && participants.size() >= maxParticipants()) startCountdown();
        return true;
    }

    public synchronized boolean leave(Player player) {
        if (state != State.REGISTRATION || participants.remove(player.getUniqueId()) == null) {
            tell(player, "not-in-registration"); return false;
        }
        wins.remove(player.getUniqueId());
        selectedKits.remove(player.getUniqueId());
        tell(player, "left");
        return true;
    }

    public synchronized boolean forceStart() {
        if (state != State.REGISTRATION || participants.size() < minParticipants()) return false;
        startCountdown();
        return true;
    }

    private synchronized void registrationTimeout() {
        if (state != State.REGISTRATION) return;
        if (participants.size() < minParticipants()) {
            state = State.IDLE;
            broadcast(msg("not-enough").replace("{min}", String.valueOf(minParticipants()))
                    .replace("{count}", String.valueOf(participants.size())));
        } else startCountdown();
    }

    private synchronized void startCountdown() {
        if (state != State.REGISTRATION) return;
        state = State.COUNTDOWN;
        int seconds = Math.max(1, config.getInt("tournament.start-countdown-seconds", 10));
        broadcast(msg("countdown").replace("{seconds}", String.valueOf(seconds)));
        plugin.getServer().getScheduler().scheduleDelayedTask(this::beginTournament, seconds * 20);
    }

    private synchronized void beginTournament() {
        if (state != State.COUNTDOWN) return;
        List<UUID> seeded = new ArrayList<>(participants.keySet());
        Collections.shuffle(seeded);
        state = State.RUNNING;
        round = 1;
        broadcast(msg("tournament-started").replace("{players}", String.valueOf(seeded.size())));
        startRound(seeded);
    }

    private synchronized void startRound(List<UUID> players) {
        if (players.size() == 1) { finish(players.get(0)); return; }
        roundWinners.clear();
        pendingMatches = 0;
        for (int i = 0; i < players.size(); i += 2) {
            UUID a = players.get(i);
            if (i + 1 >= players.size()) {
                roundWinners.add(a);
                wins.merge(a, 1, Integer::sum);
                broadcast(msg("bye").replace("{player}", name(a)));
                continue;
            }
            UUID b = players.get(i + 1);
            Match match = new Match(UUID.randomUUID(), a, b, round);
            matches.put(match.id(), match);
            pendingMatches++;
            prepareMatch(match);
        }
        if (pendingMatches == 0) advanceRound();
    }

    private void prepareMatch(Match match) {
        Player a = find(match.a()), b = find(match.b());
        if (a == null || b == null) {
            resolveMatch(match, a == null ? match.b() : match.a());
            return;
        }
        applyKit(a); applyKit(b);
        teleport(a, "spawn-a"); teleport(b, "spawn-b");
        broadcast(msg("match-ready").replace("{a}", a.getName()).replace("{b}", b.getName())
                .replace("{round}", String.valueOf(match.round())));
        int delay = Math.max(0, config.getInt("tournament.match-countdown-seconds", 5));
        plugin.getServer().getScheduler().scheduleDelayedTask(() -> {
            if (matches.containsKey(match.id())) {
                broadcast(msg("fight").replace("{a}", name(match.a())).replace("{b}", name(match.b())));
            }
        }, delay * 20);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (state != State.RUNNING || !(event.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        try {
            Object value = event.getClass().getMethod("getDamager").invoke(event);
            if (value instanceof Player p) attacker = p;
        } catch (Exception ignored) {}
        if (attacker == null) return;
        Match match = findMatch(victim.getUniqueId());
        if (match == null || !contains(match, attacker.getUniqueId())) {
            if (config.getBoolean("tournament.protection.cancel-player-damage-outside-matches", true)) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (state != State.RUNNING) return;
        Player loser = event.getEntity();
        Match match = findMatch(loser.getUniqueId());
        if (match == null) return;
        if (config.getBoolean("tournament.match.keep-inventory", true)) {
            event.setKeepInventory(true);
        }
        UUID win = match.a().equals(loser.getUniqueId()) ? match.b() : match.a();
        resolveMatch(match, win);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (state != State.RUNNING) return;
        Match match = findMatch(event.getPlayer().getUniqueId());
        if (match != null) resolveMatch(match,
                match.a().equals(event.getPlayer().getUniqueId()) ? match.b() : match.a());
    }

    private synchronized void resolveMatch(Match match, UUID win) {
        if (!matches.remove(match.id(), match)) return;
        UUID loser = match.a().equals(win) ? match.b() : match.a();
        roundWinners.add(win);
        wins.merge(win, 1, Integer::sum);
        broadcast(msg("match-finished").replace("{winner}", name(win)).replace("{loser}", name(loser))
                .replace("{round}", String.valueOf(match.round())));
        pendingMatches--;
        Player loserPlayer = find(loser);
        if (loserPlayer != null) {
            spectators.add(loser);
            plugin.getServer().getScheduler().scheduleDelayedTask(() -> {
                loserPlayer.setGamemode(3); teleport(loserPlayer, "spectator");
            }, 20);
        }
        if (pendingMatches == 0) {
            plugin.getServer().getScheduler().scheduleDelayedTask(this::advanceRound, 20);
        }
    }

    private synchronized void advanceRound() {
        if (state != State.RUNNING) return;
        List<UUID> next = new ArrayList<>(roundWinners);
        roundWinners.clear();
        round++;
        if (next.size() == 1) { finish(next.get(0)); return; }
        startRound(next);
    }

    private synchronized void finish(UUID winningPlayer) {
        winner = winningPlayer;
        state = State.FINISHED;
        Player p = find(winningPlayer);
        if (p != null) {
            p.setGamemode(0); teleport(p, "lobby"); reward(p);
        }
        broadcast(msg("winner").replace("{player}", name(winningPlayer)));
        for (UUID uuid : new ArrayList<>(spectators)) {
            Player spectator = find(uuid);
            if (spectator != null) { spectator.setGamemode(0); teleport(spectator, "lobby"); }
        }
        matches.clear(); spectators.clear();
    }

    public synchronized void stop(String reason) {
        if (state == State.IDLE) return;
        for (UUID uuid : participants.keySet()) {
            Player p = find(uuid);
            if (p != null) { p.setGamemode(0); teleport(p, "lobby"); }
        }
        participants.clear(); matches.clear(); spectators.clear(); roundWinners.clear();
        state = State.IDLE; pendingMatches = 0;
        broadcast(msg("stopped").replace("{reason}", reason == null ? "" : reason));
    }

    public void status(Player player) {
        player.sendMessage(msg("status").replace("{state}", state.name())
                .replace("{participants}", String.valueOf(participants.size()))
                .replace("{max}", String.valueOf(maxParticipants()))
                .replace("{round}", String.valueOf(round))
                .replace("{winner}", winner == null ? "-" : name(winner)));
    }

    public void bracket(Player player) {
        player.sendMessage(msg("bracket-header"));
        for (Participant p : participants.values()) {
            player.sendMessage(msg("bracket-entry").replace("{player}", p.name())
                    .replace("{wins}", String.valueOf(wins.getOrDefault(p.uuid(), 0)))
                    .replace("{status}", spectators.contains(p.uuid()) ? "OUT" : "ACTIVE"));
        }
    }

    public boolean selectKit(Player player, String kit) {
        if (state != State.REGISTRATION) { tell(player, "kit-selection-closed"); return false; }
        if (!participants.containsKey(player.getUniqueId())) { tell(player, "not-in-registration"); return false; }
        if (!kits.kitExists(kit)) { tell(player, "kit-not-found"); return false; }
        selectedKits.put(player.getUniqueId(), kit);
        tell(player, "kit-selected");
        return true;
    }

    public void kits(Player player) {
        List<String> names = kits.getKitNames();
        player.sendMessage(msg("kit-list-header"));
        if (names.isEmpty()) { player.sendMessage(msg("kit-list-empty")); return; }
        String selected = selectedKits.getOrDefault(player.getUniqueId(), kits.getDefaultKit());
        for (String kit : names) {
            player.sendMessage(msg("kit-list-entry").replace("{kit}", kit)
                    .replace("{selected}", kit.equalsIgnoreCase(selected) ? "*" : ""));
        }
    }

    public boolean adminKitCreate(String kit) { return kits.createKit(kit); }
    public boolean adminKitDelete(String kit) { return kits.deleteKit(kit); }
    public boolean adminKitAdd(String kit, String command) { return kits.addCommand(kit, command); }
    public boolean adminKitClear(String kit) { return kits.clearCommands(kit); }
    public List<String> adminKitNames() { return kits.getKitNames(); }
    public List<String> adminKitCommands(String kit) { return kits.getCommands(kit); }
    public String adminKitDefault() { return kits.getDefaultKit(); }
    public boolean adminConfigSet(String path, String value) { return kits.setConfigValue(path, value); }
    public Object adminConfigGet(String path) { return kits.getConfigValue(path); }
    public boolean adminConfigReload() { return kits.reloadConfig(); }

    private void applyKit(Player player) {
        String kit = selectedKits.getOrDefault(player.getUniqueId(), kits.getDefaultKit());
        if (!kit.isBlank()) kits.apply(player, kit);
    }

    public boolean spectate(Player player) {
        if (state != State.RUNNING && state != State.FINISHED) { tell(player, "spectate-unavailable"); return false; }
        spectators.add(player.getUniqueId()); player.setGamemode(3); teleport(player, "spectator");
        tell(player, "spectating"); return true;
    }

    private void reward(Player p) {
        String command = config.getString("tournament.rewards.command", "");
        if (command == null || command.isBlank()) return;
        try { plugin.getServer().getCommandMap().executeCommand(plugin.getServer().getConsoleSender(), command.replace("{player}", p.getName())); }
        catch (Exception e) { plugin.getLogger().warning("Tournament reward failed: " + e.getMessage()); }
    }

    private Match findMatch(UUID uuid) {
        for (Match m : matches.values()) if (contains(m, uuid)) return m;
        return null;
    }
    private boolean contains(Match m, UUID uuid) { return m.a().equals(uuid) || m.b().equals(uuid); }

    private Player find(UUID uuid) {
        for (Player p : plugin.getServer().getOnlinePlayers().values()) if (uuid.equals(p.getUniqueId())) return p;
        return null;
    }
    private String name(UUID uuid) {
        Participant p = participants.get(uuid);
        return p == null ? String.valueOf(uuid) : p.name();
    }
    private void broadcast(String text) {
        for (Player p : plugin.getServer().getOnlinePlayers().values()) p.sendMessage(text);
        plugin.getLogger().info(text.replaceAll("§.", ""));
    }
    private void tell(Player p, String key) { p.sendMessage(msg(key)); }
    private String msg(String key) { return messages.getMessage("messages.tournament." + key); }
    private boolean enabled() { return config.getBoolean("tournament.enabled", true); }
    private int minParticipants() { return Math.max(2, config.getInt("tournament.registration.min-participants", 2)); }
    private int maxParticipants() { return Math.max(minParticipants(), config.getInt("tournament.registration.max-participants", 16)); }

    private void teleport(Player player, String target) {
        try {
            String base = target.equals("lobby") ? "tournament.lobby" : "tournament.arena." + target;
            String worldName = config.getString(base + ".world", player.getLevel().getName());
            Object level = plugin.getServer().getLevelByName(worldName);
            if (level == null) { plugin.getLogger().warning("Tournament world not loaded: " + worldName); return; }
            double x = config.getDouble(base + ".x"), y = config.getDouble(base + ".y"), z = config.getDouble(base + ".z");
            Class<?> lc = Class.forName("org.powernukkitx.level.Location");
            Object location = null;
            for (Constructor<?> c : lc.getConstructors()) {
                Class<?>[] t = c.getParameterTypes();
                if (t.length == 6 && t[0] == double.class && t[1] == double.class && t[2] == double.class
                        && t[3] == float.class && t[4] == float.class && t[5].isAssignableFrom(level.getClass())) {
                    location = c.newInstance(x, y, z, player.getYaw(), player.getPitch(), level); break;
                }
            }
            if (location == null) return;
            for (Method m : player.getClass().getMethods()) {
                if (!m.getName().equals("teleport") || m.getParameterCount() != 2) continue;
                if (!m.getParameterTypes()[0].isAssignableFrom(lc)) continue;
                Class<?> cause = m.getParameterTypes()[1];
                if (!cause.isEnum() || cause.getEnumConstants().length == 0) continue;
                Object value = cause.getEnumConstants()[0];
                for (Object v : cause.getEnumConstants()) if ("COMMAND".equals(String.valueOf(v))) { value = v; break; }
                m.invoke(player, location, value); return;
            }
        } catch (Exception e) { plugin.getLogger().warning("Tournament teleport failed: " + e.getMessage()); }
    }
}