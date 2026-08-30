package com.audit.platform.infra.common.lock;

import com.audit.platform.infra.common.exception.LockException;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Redis 分布式锁；无 Redisson 时回退 JVM 锁，便于本地启动。
 */
@Component
public class RedisLockHelper {
    @Resource
    private ObjectProvider<RedissonClient> redissonClientProvider;

    private final ConcurrentHashMap<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    public <T> T execute(String key, Supplier<T> action) {
        RedissonClient redissonClient = this.redissonClientProvider.getIfAvailable();
        if (redissonClient != null) {
            RLock lock = redissonClient.getLock(key);
            try {
                if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                    throw new LockException("操作过于频繁，请稍后重试");
                }
                try {
                    return action.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockException("获取锁被中断", e);
            }
        }
        ReentrantLock local = localLocks.computeIfAbsent(key, k -> new ReentrantLock());
        if (!local.tryLock()) {
            throw new LockException("操作过于频繁，请稍后重试");
        }
        try {
            return action.get();
        } finally {
            local.unlock();
        }
    }

    public void execute(String key, Runnable action) {
        execute(key, () -> {
            action.run();
            return null;
        });
    }

    public boolean tryLock(String key) {
        RedissonClient redissonClient = this.redissonClientProvider.getIfAvailable();
        if (redissonClient != null) {
            try {
                return redissonClient.getLock(key).tryLock(5, 30, TimeUnit.SECONDS);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return tryLocalLock(key);
            }
        }
        return tryLocalLock(key);
    }

    public void unlock(String key) {
        RedissonClient redissonClient = this.redissonClientProvider.getIfAvailable();
        if (redissonClient != null) {
            try {
                RLock lock = redissonClient.getLock(key);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    return;
                }
            } catch (Exception ignored) {
                // fall through to local
            }
        }
        ReentrantLock local = localLocks.get(key);
        if (local != null && local.isHeldByCurrentThread()) {
            local.unlock();
        }
    }

    private boolean tryLocalLock(String key) {
        ReentrantLock local = localLocks.computeIfAbsent(key, k -> new ReentrantLock());
        try {
            return local.tryLock(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
