package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Manages short-lived player-to-player teleport requests. */
public final class TPAManager {
    public enum Result { SENT, ACCEPTED, DECLINED, EXPIRED, NOT_FOUND, SELF, ALREADY_PENDING }

    public record Request(String id, UUID requester, UUID target, String requesterName,
                          String targetName, long createdAt, long expiresAt) {
        public boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }

    private static final long DEFAULT_TIMEOUT_MS = 60_000L;
    private final SiedlerPlugin plugin;
    private final Map<UUID, Request> incoming = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> outgoing = new ConcurrentHashMap<>();

    public TPAManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
    }

    public Result request(Player requester, Player target) {
        if (requester == null || target == null) return Result.NOT_FOUND;
        if (requester.getUniqueId().equals(target.getUniqueId())) return Result.SELF;
        cleanup(requester.getUniqueId());
        cleanup(target.getUniqueId());

        if (incoming.containsKey(target.getUniqueId()) || outgoing.containsKey(requester.getUniqueId())) {
            return Result.ALREADY_PENDING;
        }

        long now = System.currentTimeMillis();
        Request request = new Request(
                UUID.randomUUID().toString(),
                requester.getUniqueId(), target.getUniqueId(),
                requester.getName(), target.getName(),
                now, now + DEFAULT_TIMEOUT_MS
        );
        incoming.put(target.getUniqueId(), request);
        outgoing.put(requester.getUniqueId(), target.getUniqueId());
        return Result.SENT;
    }

    public Request getIncoming(Player target) {
        if (target == null) return null;
        cleanup(target.getUniqueId());
        return incoming.get(target.getUniqueId());
    }

    public Result accept(Player target) throws Exception {
        Request request = getIncoming(target);
        if (request == null) return Result.NOT_FOUND;
        if (request.isExpired()) {
            remove(request);
            return Result.EXPIRED;
        }

        Player requester = findOnline(request.requester());
        if (requester == null) {
            remove(request);
            return Result.NOT_FOUND;
        }

        teleport(requester, target);
        remove(request);
        return Result.ACCEPTED;
    }

    public Result decline(Player target) {
        Request request = getIncoming(target);
        if (request == null) return Result.NOT_FOUND;
        if (request.isExpired()) {
            remove(request);
            return Result.EXPIRED;
        }
        remove(request);
        return Result.DECLINED;
    }

    public Result cancel(Player requester) {
        if (requester == null) return Result.NOT_FOUND;
        UUID targetId = outgoing.remove(requester.getUniqueId());
        if (targetId == null) return Result.NOT_FOUND;
        Request request = incoming.get(targetId);
        if (request != null && request.requester().equals(requester.getUniqueId())) {
            incoming.remove(targetId);
        }
        return Result.DECLINED;
    }

    private void cleanup(UUID playerId) {
        Request incomingRequest = incoming.get(playerId);
        if (incomingRequest != null && incomingRequest.isExpired()) {
            remove(incomingRequest);
        }
        UUID targetId = outgoing.get(playerId);
        if (targetId != null) {
            Request outgoingRequest = incoming.get(targetId);
            if (outgoingRequest == null || outgoingRequest.isExpired()) {
                outgoing.remove(playerId);
            }
        }
    }

    private void remove(Request request) {
        incoming.remove(request.target(), request);
        outgoing.remove(request.requester(), request.target());
    }

    private Player findOnline(UUID uuid) {
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player.getUniqueId().equals(uuid)) return player;
        }
        return null;
    }

    private void teleport(Player requester, Player target) throws Exception {
        Class<?> locationClass = Class.forName("org.powernukkitx.level.Location");
        Object location = null;
        for (java.lang.reflect.Constructor<?> constructor : locationClass.getConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length == 6 && types[0] == double.class && types[1] == double.class
                    && types[2] == double.class && types[3] == float.class
                    && types[4] == float.class && types[5].isAssignableFrom(target.getLevel().getClass())) {
                location = constructor.newInstance(
                        target.getPosition().getX(), target.getPosition().getY(), target.getPosition().getZ(),
                        (float) target.getYaw(), (float) target.getPitch(), target.getLevel());
                break;
            }
        }
        if (location == null) throw new IllegalStateException("Die PNX-Location-API konnte nicht aufgelöst werden.");

        for (java.lang.reflect.Method method : requester.getClass().getMethods()) {
            if (!"teleport".equals(method.getName()) || method.getParameterCount() != 2) continue;
            if (!method.getParameterTypes()[0].isAssignableFrom(locationClass)) continue;
            Class<?> causeType = method.getParameterTypes()[1];
            if (!causeType.isEnum()) continue;
            Object cause = null;
            for (Object constant : causeType.getEnumConstants()) {
                if ("COMMAND".equals(String.valueOf(constant))) { cause = constant; break; }
            }
            if (cause == null && causeType.getEnumConstants().length > 0) cause = causeType.getEnumConstants()[0];
            if (cause == null) throw new IllegalStateException("Keine Teleport-Ursache verfügbar.");
            Object result = method.invoke(requester, location, cause);
            if (result instanceof Boolean success && !success) {
                throw new IllegalStateException("Der Teleport wurde abgelehnt.");
            }
            return;
        }
        throw new IllegalStateException("Die PNX-Teleport-API konnte nicht aufgelöst werden.");
    }
}
