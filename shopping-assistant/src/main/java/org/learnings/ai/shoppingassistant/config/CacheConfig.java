package org.learnings.ai.shoppingassistant.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    CacheManager cacheManager() {
        CaffeineCache productCategoriesCache = new CaffeineCache(
                "product-categories",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(1))
                        .maximumSize(1)
                        .build()
        );
        CaffeineCache productsCache = new CaffeineCache(
                "products-per-category",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .maximumSize(10)
                        .build()
        );
        CaffeineCache featuredProductsCache = new CaffeineCache(
                "all-products",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(4))
                        .maximumSize(1)
                        .build()
        );

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(productCategoriesCache, productsCache, featuredProductsCache));

        return cacheManager;
    }
}
