package org.learnings.ai.shoppingassistant.tools;

/**
 * Marker interface for tools available to the Shopping Agent.
 * <p>
 * Implement this interface for any {@code @Component} exposing {@code @Tool}-annotated methods
 * that assist with product discovery, recommendations, inventory, or other shopping-related
 * capabilities. Shared tools may implement this interface alongside other agent tool interfaces.
 */
public interface ShoppingAgentTool {
}
