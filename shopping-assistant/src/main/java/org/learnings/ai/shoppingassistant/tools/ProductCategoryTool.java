package org.learnings.ai.shoppingassistant.tools;

import org.learnings.ai.shoppingassistant.services.products.ProductService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductCategoryTool implements AgentTool {

    private final ProductService productService;

    public ProductCategoryTool(ProductService productService) {
        this.productService = productService;
    }

    @Tool(description = "List the valid product categories. Call this before searching "
            + "if you need to map the shopper's request to a real category.")
    public List<String> listCategories() {
        return productService.getAllCategories();
    }
}
