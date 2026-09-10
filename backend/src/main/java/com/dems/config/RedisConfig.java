package com.dems.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis configuration that gracefully handles connection failures in local dev environments.
 *
 * <p>When {@code app.redis.optional=true}, Redis startup validation is disabled so the
 * application context loads even when Redis is not running. Redis-backed features
 * (JWT token revocation, refresh token storage) degrade silently, as {@link
 * com.dems.security.service.JwtTokenService} wraps all Redis operations in try/catch blocks.
 *
 * <p>In production, remove {@code app.redis.optional} or set it to {@code false} to ensure
 * Redis availability is enforced at startup.
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * Provides a non-validating Lettuce connection factory for optional Redis in local dev.
     * Only activated when {@code app.redis.optional=true}.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.redis.optional", havingValue = "true")
    public LettuceConnectionFactory optionalRedisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.database:0}") int database) {

        log.info("Configuring Redis as OPTIONAL — app will start even if Redis is unavailable");

        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);
        serverConfig.setDatabase(database);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, clientConfig);
        // Disable eager validation — don't throw if Redis is down at startup
        factory.setValidateConnection(false);
        factory.setShareNativeConnection(true);
        return factory;
    }

    /**
     * Fault-tolerant StringRedisTemplate that swallows afterPropertiesSet() errors.
     * Only activated when {@code app.redis.optional=true}.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.redis.optional", havingValue = "true")
    public StringRedisTemplate optionalStringRedisTemplate(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.database:0}") int database) {

        LettuceConnectionFactory factory = optionalRedisConnectionFactory(host, port, database);
        factory.afterPropertiesSet();

        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        try {
            template.afterPropertiesSet();
            log.info("Redis connected successfully at {}:{}", host, port);
        } catch (Exception e) {
            log.warn("Redis unavailable at {}:{} — JWT revocation disabled. Start Redis to enable it.", host, port);
        }
        return template;
    }
}

