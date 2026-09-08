package org.learnings.ai.shoppingassistant.agents;

/**
 * Everything an {@link Agent} needs to serve one turn.
 *
 * <p>The user id is carried explicitly rather than through a {@code ThreadLocal}: under streaming the model call,
 * the tool executions and the response assembly all happen on Reactor scheduler threads, not on the request thread,
 * so any thread-bound context is already gone by the time it is needed.
 *
 * @param message        the (possibly router-rewritten) task for the agent
 * @param conversationId the memory key, already namespaced by user
 * @param userId         the resolved caller, propagated to tools via {@code ToolContext}
 */
public record AgentRequest(String message, String conversationId, String userId) {
}
