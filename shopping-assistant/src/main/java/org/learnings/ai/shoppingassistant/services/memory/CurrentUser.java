package org.learnings.ai.shoppingassistant.services.memory;

/**
 * Request-scoped holder for the current user's id, so {@code @Tool} beans (which are
 * singletons) can access the id resolved per-request by the agent, without the model
 * having to pass it as a tool argument.
 */
public final class CurrentUser {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(String userId) {
        CURRENT.set(userId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
