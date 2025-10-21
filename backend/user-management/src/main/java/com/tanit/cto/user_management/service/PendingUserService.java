package com.tanit.cto.user_management.service;

import com.tanit.cto.user_management.model.User;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class PendingUserService {

    private final RedisTemplate<String, Object> redisTemplate;

    public PendingUserService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String PREFIX = "pending_user:";

    public void savePendingUser(String token, User user) {
        redisTemplate.opsForValue().set(PREFIX + token, user, 24, TimeUnit.HOURS);
    }

    public User getPendingUser(String token) {
        Object obj = redisTemplate.opsForValue().get(PREFIX + token);
        return (obj instanceof User) ? (User) obj : null;
    }

    public void deletePendingUser(String token) {
        redisTemplate.delete(PREFIX + token);
    }
}
