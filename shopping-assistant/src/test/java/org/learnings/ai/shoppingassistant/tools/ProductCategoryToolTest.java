package org.learnings.ai.shoppingassistant.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.ai.shoppingassistant.services.products.ProductService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCategoryToolTest {

    @Mock
    private ProductService productService;
    @InjectMocks
    private ProductCategoryTool productCategoryTool;

    @Test
    void listCategories_whenCategoriesExist_returnsAllCategories() {
        when(productService.getAllCategories()).thenReturn(List.of("CLOTHES", "ACCESSORIES", "TECHNOLOGY"));

        List<String> categories = productCategoryTool.listCategories();

        assertThat(categories).containsExactlyInAnyOrder("CLOTHES", "ACCESSORIES", "TECHNOLOGY");
        verifyNoMoreInteractions(productService);
    }

    @Test
    void listCategories_whenNoCategoriesExist_returnsEmpty() {
        when(productService.getAllCategories()).thenReturn(List.of());

        List<String> categories = productCategoryTool.listCategories();

        assertThat(categories).isEmpty();
        verifyNoMoreInteractions(productService);
    }
}
