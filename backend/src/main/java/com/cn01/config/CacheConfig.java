package com.cn01.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

/**
 * 前台只读接口的 Redis 缓存。后台任何修改都会清空相关缓存（见各 AdminService 上的 @CacheEvict），
 * 所以"后台改价，前台即时生效"；TTL 只是兜底。
 */
@Configuration
public class CacheConfig {
    public static final String PORTAL_SOLUTIONS = "portal:solutions";
    public static final String PORTAL_SOLUTION = "portal:solution";
    public static final String PORTAL_CATEGORIES = "portal:categories";
    public static final String PORTAL_PRICE = "portal:price";
    public static final String PORTAL_NEWS = "portal:news";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .prefixCacheNameWith("cn01:")
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer()));
        // transactionAware：清缓存推迟到事务提交之后，避免并发读在提交前把旧数据重新写回缓存
        return RedisCacheManager.builder(factory).cacheDefaults(config).transactionAware().build();
    }

    public static RedisSerializer<Object> valueSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                // 只允许反序列化本项目类型与 JDK 基础类型；放开 Object 会让写得进 Redis 的人借反序列化链执行代码
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.cn01.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.time.")
                        .allowIfSubType("java.math.")
                        .allowIfSubType("java.lang.")
                        .build(),
                ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
