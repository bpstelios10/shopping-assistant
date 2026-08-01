package org.learnings.ai.shoppingassistant.componenttests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.config.TestCacheConfig;
import org.learnings.ai.shoppingassistant.services.products.ProductClient;
import org.learnings.ai.shoppingassistant.services.products.ProductService;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("component-test")
@Import(TestCacheConfig.class)
public class CachingComponentTest {

    @MockitoBean
    private VectorStore vectorStore;
    @MockitoBean
    private RedisChatMemoryRepository redisChatMemoryRepository;
    @MockitoBean
    ProductClient productClient;
    @Autowired
    ProductService productService;
    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void cleanCache() {
        Objects.requireNonNull(cacheManager.getCache("product-categories")).clear();
    }

    @Test
    void getCategories_whenTwoRequestsRun_cachesTheValues() {
        when(productClient.getAllCategories()).thenReturn(List.of("Electronics", "Books", "Clothing"));

        List<String> allCategories1 = productService.getAllCategories();
        List<String> allCategories2 = productService.getAllCategories();
        Cache categoriesCache = cacheManager.getCache("product-categories");

        assertThat(allCategories1).hasSize(3);
        assertThat(allCategories1).containsExactlyInAnyOrder("Electronics", "Books", "Clothing");
        assertThat(allCategories1).isEqualTo(allCategories2);
        verify(productClient, times(1)).getAllCategories();
        verifyNoMoreInteractions(productClient);

        // also validate cache hit directly
        assertThat(categoriesCache).isNotNull();
        Cache.ValueWrapper valueWrapper = categoriesCache.get(SimpleKey.EMPTY);
        assertThat(valueWrapper).isNotNull();
        assertThat(valueWrapper.get()).isNotNull();
    }

    @Test
    void getCategories_whenTwoRequestsRunAndSecondIsAfterCacheExpiration_noCacheUsed() {
        when(productClient.getAllCategories()).thenReturn(List.of("Electronics", "Books", "Clothing"));

        productService.getAllCategories();
        await()
                .atMost(Duration.ofMillis(600))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    productService.getAllCategories();

                    verify(productClient, times(2))
                            .getAllCategories();
                });

        verifyNoMoreInteractions(productClient);
    }
}
