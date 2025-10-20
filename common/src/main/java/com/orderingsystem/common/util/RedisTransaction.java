package com.orderingsystem.common.util;

import java.util.function.Consumer;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

@Component
public class RedisTransaction {

    public void execute(RedisTemplate<String, String> redisTemplate,
                        Consumer<RedisOperations<String, String>> commands) {
        redisTemplate.execute(new SessionCallback<Void>() {
            @Override
            public <K, V> Void execute(RedisOperations<K, V> operations) throws DataAccessException {
                operations.multi();
                commands.accept((RedisOperations<String, String>) operations);
                operations.exec();
                return null;
            }
        });
    }
}
