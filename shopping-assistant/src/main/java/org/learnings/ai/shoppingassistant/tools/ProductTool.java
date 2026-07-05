package org.learnings.ai.shoppingassistant.tools;

import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.domain.ProductSearchCriteria;
import org.learnings.ai.shoppingassistant.services.products.ProductService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductTool implements AgentTool {

    private final ProductService productService;

    public ProductTool(ProductService productService) {
        this.productService = productService;
    }

    @Tool(description = "List every product currently available in the store's catalog. "
            + "Use this when the shopper wants to browse or asks to see all products.")
    public List<Product> listAllProducts() {
        return productService.getAllProducts();
    }

    @Tool(description = "Search the store's catalog for products matching the shopper's request. "
            + "Extract any price limit or category the shopper mentions and pass them as filters. "
            + "Returns an empty list when nothing matches.")
    public List<Product> searchProducts(
            @ToolParam(description = "Free-text keywords describing the product the shopper wants, "
                    + "e.g. 'espresso maker' or 'running shoes'.")
            String query,
            @ToolParam(required = false, description = "Maximum price the shopper is willing to pay, if mentioned.")
            Double maxPrice,
            @ToolParam(required = false, description = "Product category to filter by, if mentioned. "
                    + "Must be one of the values returned by the listCategories tool — call listCategories "
                    + "first to get the valid categories, and only pass a value from that list.")
            String category) {
        return productService.search(new ProductSearchCriteria(query, maxPrice, category));
    }
}
