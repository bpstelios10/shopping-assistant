package org.learnings.ai.shoppingassistant.tools;

import org.learnings.ai.shoppingassistant.domain.Product;
import org.learnings.ai.shoppingassistant.services.products.ProductService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// TODO: replace ProductService mock matching with semantic search over a VectorStore (see searchProducts).
@Component
public class ProductTools implements AgentTool {

    private final ProductService productService;

    public ProductTools(ProductService productService) {
        this.productService = productService;
    }

    @Tool(description = "List every product currently available in the store's catalog. "
            + "Use this when the shopper wants to browse or asks to see all products.")
    public List<Product> listAllProducts() {
        return productService.getProductDetails();
    }

    @Tool(description = "Search the store's catalog for products whose name matches the shopper's query. "
            + "Returns an empty list when nothing matches. ")
    // NOTE: Placeholder lexical matching (tokenize + singularise + contains) — good enough while we
    // focus on tools. It can't handle synonyms, typos, or searching by type/price/description.
    // TODO: replace with semantic search over a VectorStore (embed the catalog, similaritySearch the
    //  query) so matching is meaning-based instead of string-based.
    public List<Product> searchProducts(
            @ToolParam(description = "The shopper's search keywords describing the kind of product they want, "
                    + "e.g. 'running shoes' or 'coffee maker'. Matched case-insensitively against product names.")
            String query) {
        List<String> queryTerms = tokenize(query);

        // No usable search terms (e.g. blank query) -> nothing to match on.
        if (queryTerms.isEmpty()) {
            return List.of();
        }

        return productService.getProductDetails().stream()
                .filter(product -> matchesAnyTerm(product, queryTerms))
                .toList();
    }

    private boolean matchesAnyTerm(Product product, List<String> queryTerms) {
        List<String> nameTerms = tokenize(product.name());
        return queryTerms.stream()
                .anyMatch(queryTerm -> nameTerms.stream()
                        .anyMatch(nameTerm -> termsRelated(nameTerm, queryTerm)));
    }

    // A loose match: either word contains the other, after basic singular/plural
    // normalisation so "widgets" matches "widget" and vice versa.
    private boolean termsRelated(String nameTerm, String queryTerm) {
        String a = singularise(nameTerm);
        String b = singularise(queryTerm);
        return a.contains(b) || b.contains(a);
    }

    private String singularise(String word) {
        return word.endsWith("s") ? word.substring(0, word.length() - 1) : word;
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(word -> !word.isBlank())
                .toList();
    }
}
