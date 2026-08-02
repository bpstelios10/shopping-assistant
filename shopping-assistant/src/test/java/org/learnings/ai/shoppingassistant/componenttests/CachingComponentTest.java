package org.learnings.ai.shoppingassistant.componenttests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.config.TestCacheConfig;
import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;
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
import java.util.UUID;

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
        Objects.requireNonNull(cacheManager.getCache("products-per-category")).clear();
        Objects.requireNonNull(cacheManager.getCache("all-products")).clear();
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

    @Test
    void searchProducts_whenTwoRequestsRun_cachesTheValues() {
        ProductSearchCriteria criteria = new ProductSearchCriteria(null, null, "Electronics");
        Product product = new Product(UUID.randomUUID(), "Smartphone", "Electronics", 699.99f);
        when(productClient.search(criteria)).thenReturn(List.of(product));

        List<Product> allCategories1 = productService.search(criteria);
        List<Product> allCategories2 = productService.search(criteria);
        Cache productsCache = cacheManager.getCache("products-per-category");

        assertThat(allCategories1).hasSize(1);
        assertThat(allCategories1).containsExactlyInAnyOrder(product);
        assertThat(allCategories1).isEqualTo(allCategories2);
        verify(productClient, times(1)).search(criteria);
        verifyNoMoreInteractions(productClient);

        // also validate cache hit directly
        assertThat(productsCache).isNotNull();
        Cache.ValueWrapper valueWrapper = productsCache.get("Electronics");
        assertThat(valueWrapper).isNotNull();
        assertThat(valueWrapper.get()).isNotNull();
    }

    @Test
    void searchProducts_whenTwoRequestsRunAndSecondIsAfterCacheExpiration_noCacheUsed() {
        ProductSearchCriteria criteria = new ProductSearchCriteria(null, null, "Electronics");
        Product product = new Product(UUID.randomUUID(), "Smartphone", "Electronics", 699.99f);
        when(productClient.search(criteria)).thenReturn(List.of(product));

        productService.search(criteria);
        await()
                .atMost(Duration.ofMillis(600))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    productService.search(criteria);

                    verify(productClient, times(2))
                            .search(criteria);
                });

        verifyNoMoreInteractions(productClient);
    }

    @Test
    void getAllProducts_whenTwoRequestsRun_cachesTheValues() {
        Product product = new Product(UUID.randomUUID(), "Smartphone", "Electronics", 699.99f);
        when(productClient.getAllProducts()).thenReturn(List.of(product));

        List<Product> allCategories1 = productService.getAllProducts();
        List<Product> allCategories2 = productService.getAllProducts();
        Cache categoriesCache = cacheManager.getCache("all-products");

        assertThat(allCategories1).hasSize(1);
        assertThat(allCategories1).containsExactlyInAnyOrder(product);
        assertThat(allCategories1).isEqualTo(allCategories2);
        verify(productClient, times(1)).getAllProducts();
        verifyNoMoreInteractions(productClient);

        // also validate cache hit directly
        assertThat(categoriesCache).isNotNull();
        Cache.ValueWrapper valueWrapper = categoriesCache.get(SimpleKey.EMPTY);
        assertThat(valueWrapper).isNotNull();
        assertThat(valueWrapper.get()).isNotNull();
    }

    @Test
    void getAllProducts_whenTwoRequestsRunAndSecondIsAfterCacheExpiration_noCacheUsed() {
        Product product = new Product(UUID.randomUUID(), "Smartphone", "Electronics", 699.99f);
        when(productClient.getAllProducts()).thenReturn(List.of(product));

        productService.getAllProducts();
        await()
                .atMost(Duration.ofMillis(600))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    productService.getAllProducts();

                    verify(productClient, times(2))
                            .getAllProducts();
                });

        verifyNoMoreInteractions(productClient);
    }
}
