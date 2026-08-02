package org.learnings.ai.shoppingassistant.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;

@TestConfiguration
public class TestCacheConfig {

    @Bean
    @Primary
    CacheManager testCacheManager() {
        CaffeineCache productCategoriesCache = new CaffeineCache(
                "product-categories",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMillis(300))
                        .maximumSize(1)
                        .build()
        );
        CaffeineCache productsCache = new CaffeineCache(
                "products-per-category",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMillis(300))
                        .maximumSize(10)
                        .build()
        );
        CaffeineCache featuredProductsCache = new CaffeineCache(
                "all-products",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMillis(300))
                        .maximumSize(1)
                        .build()
        );

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(productCategoriesCache, productsCache, featuredProductsCache));

        return cacheManager;
    }
}
