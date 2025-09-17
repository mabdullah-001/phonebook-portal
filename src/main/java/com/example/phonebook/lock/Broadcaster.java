package com.example.phonebook.lock;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Very small publish/subscribe broadcaster used together with Vaadin Push.
 * Register a Consumer<String> to receive messages.
 */
public final class Broadcaster {

    private static final Set<Consumer<String>> listeners = new CopyOnWriteArraySet<>();
    private static final Executor executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "broadcaster-executor");
        t.setDaemon(true);
        return t;
    });

    private Broadcaster() {}

    public static void register(Consumer<String> listener) {
        listeners.add(listener);
    }

    public static void unregister(Consumer<String> listener) {
        listeners.remove(listener);
    }

    /**
     * Broadcast a string message to all registered listeners.
     * The message format is up to the caller (we use "LOCK:<id>:<sessionId>:<meta>" and "UNLOCK:<id>:<sessionId>").
     */
    public static void broadcast(String message) {
        for (Consumer<String> l : listeners) {
            executor.execute(() -> {
                try {
                    l.accept(message);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }
}

