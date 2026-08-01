package org.learnings.ai.shoppingassistant.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@TestConfiguration
public class TestCacheConfig {

    @Bean
    @Primary
    CacheManager testCacheManager() {
        CaffeineCacheManager manager =
                new CaffeineCacheManager("product-categories", "products-per-category");

        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMillis(300))
        );

        return manager;
    }
}
