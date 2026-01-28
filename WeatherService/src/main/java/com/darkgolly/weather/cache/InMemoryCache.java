package com.darkgolly.weather.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCache<V> {
    private final Map<String, Entry<V>> storage = new ConcurrentHashMap<>();
    private final Duration ttl;

    public InMemoryCache(Duration ttl) {
        this.ttl = ttl;
    }

    public V get(String key) {
        Entry<V> entry = storage.get(key);
        if (entry == null) {
            return null;
        }
        if (Instant.now().isAfter(entry.expiresAt)) {
            storage.remove(key);
            return null;
        }
        return entry.value;
    }

    public void put(String key, V value) {
        storage.put(key, new Entry<>(value, Instant.now().plus(ttl)));
    }

    private record Entry<V>(V value, Instant expiresAt) {
    }
}
