package com.audit.platform.infra.common.util;

import jakarta.annotation.Resource;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class NumGenerator {
    @Resource
    private ObjectProvider<RedissonClient> redissonClientProvider;

    private final ConcurrentHashMap<String, AtomicLong> local = new ConcurrentHashMap<>();

    public String next(String prefix, int width) {
        long value;
        RedissonClient client = redissonClientProvider.getIfAvailable();
        if (client != null) {
            try {
                value = client.getAtomicLong("seq:" + prefix).incrementAndGet();
            } catch (Exception ignored) {
                value = localNext(prefix);
            }
        } else {
            value = localNext(prefix);
        }
        return prefix + String.format("%0" + width + "d", value);
    }

    private long localNext(String prefix) {
        return local.computeIfAbsent(prefix, key -> new AtomicLong(System.currentTimeMillis() % 90_000 + 100))
                .incrementAndGet();
    }
}
