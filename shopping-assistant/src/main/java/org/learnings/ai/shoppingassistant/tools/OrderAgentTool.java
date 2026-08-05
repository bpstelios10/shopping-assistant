package org.learnings.ai.shoppingassistant.tools;

/**
 * Marker interface for tools available to the Order Agent.
 * <p>
 * Implement this interface for any {@code @Component} exposing {@code @Tool}-annotated methods
 * related to order management, such as placing orders, retrieving order details, checking order
 * status, or cancellations. Shared tools may implement this interface alongside other agent tool
 * interfaces.
 */
public interface OrderAgentTool {
}
