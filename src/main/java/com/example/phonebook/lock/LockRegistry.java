package com.example.phonebook.lock;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple central in-JVM lock registry mapping recordId -> sessionId.
 * When acquiring or releasing, it broadcasts events via Broadcaster so all UIs stay in sync.
 *
 * Message format:
 *  - LOCK:<recordId>:<sessionId>:<metaEncoded>
 *  - UNLOCK:<recordId>:<sessionId>
 *
 * Note: meta is URLEncoded to avoid ":" collisions.
 */
public final class LockRegistry {

    private static final ConcurrentMap<Integer, String> openEditors = new ConcurrentHashMap<>();

    private LockRegistry() {}

    /**
     * Try to acquire lock for recordId by sessionId.
     * If success returns true and broadcasts LOCK event.
     * If already held by another session returns false.
     */
    public static boolean tryAcquire(int recordId, String sessionId, String meta) {
        // putIfAbsent returns previous value; if null then we acquired
        String prev = openEditors.putIfAbsent(recordId, sessionId);
        if (prev == null || prev.equals(sessionId)) {
            // broadcast lock to other UIs
            String safeMeta = meta == null ? "" : URLEncoder.encode(meta, StandardCharsets.UTF_8);
            Broadcaster.broadcast("LOCK:" + recordId + ":" + sessionId + ":" + safeMeta);
            return true;
        }
        return false;
    }

    /**
     * Release lock only if the given sessionId is the holder.
     * Broadcasts UNLOCK if released.
     */
    public static boolean release(int recordId, String sessionId) {
        boolean removed = openEditors.remove(recordId, sessionId);
        if (removed) {
            Broadcaster.broadcast("UNLOCK:" + recordId + ":" + sessionId);
        }
        return removed;
    }

    /**
     * Force release (admin or cleanup) without checking sessionId.
     */
    public static boolean forceRelease(int recordId) {
        String prev = openEditors.remove(recordId);
        if (prev != null) {
            Broadcaster.broadcast("UNLOCK:" + recordId + ":" + prev);
            return true;
        }
        return false;
    }

    public static Optional<String> getHolderSessionId(int recordId) {
        return Optional.ofNullable(openEditors.get(recordId));
    }
}

