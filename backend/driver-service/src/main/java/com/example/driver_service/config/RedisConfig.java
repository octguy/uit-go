package com.example.driver_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Configuration for Master-Replica Setup
 * - Master: Handles all WRITE operations
 * - Replica: Handles all READ operations
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.master.host}")
    private String masterHost;

    @Value("${spring.data.redis.master.port}")
    private int masterPort;

    @Value("${spring.data.redis.replica.host}")
    private String replicaHost;

    @Value("${spring.data.redis.replica.port}")
    private int replicaPort;

    /**
     * Connection factory for Redis MASTER (writes)
     */
    @Bean(name = "redisMasterConnectionFactory")
    @Primary
    public RedisConnectionFactory redisMasterConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(masterHost);
        config.setPort(masterPort);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Connection factory for Redis REPLICA (reads)
     */
    @Bean(name = "redisReplicaConnectionFactory")
    public RedisConnectionFactory redisReplicaConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(replicaHost);
        config.setPort(replicaPort);
        return new LettuceConnectionFactory(config);
    }

    /**
     * RedisTemplate for WRITE operations (uses master)
     */
    @Bean(name = "redisMasterTemplate")
    @Primary
    public RedisTemplate<String, String> redisMasterTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisMasterConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisTemplate for READ operations (uses replica)
     */
    @Bean(name = "redisReplicaTemplate")
    public RedisTemplate<String, String> redisReplicaTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisReplicaConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisTemplate for Object storage (uses master for writes)
     * Used by TripNotificationService to store PendingTripNotification objects
     */
    @Bean(name = "redisObjectTemplate")
    public RedisTemplate<String, Object> redisObjectTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisMasterConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        // Use default JDK serialization for objects
        template.afterPropertiesSet();
        return template;
    }
}
