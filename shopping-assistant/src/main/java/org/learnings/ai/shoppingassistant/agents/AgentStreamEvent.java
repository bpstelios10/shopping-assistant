package org.learnings.ai.shoppingassistant.agents;

import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * Domain-level events produced while an agent streams a turn.
 *
 * <p>Deliberately not raw text: a shopper-facing stream has to carry more than tokens (which agent answered, which
 * tools ran, what the turn cost), and a typed event stream keeps that contract explicit instead of encoding it into
 * the text itself. The web layer maps these to server-sent events.
 *
 * <p>Ordering guarantee for a successful turn:
 * {@link Routed} → zero or more {@link ToolInvoked}/{@link Delta} → exactly one {@link Completed}.
 * A failed turn ends with {@link Failed} instead, and no further events follow it.
 */
public sealed interface AgentStreamEvent {

    /** The routing decision, emitted before the first token so a client can render the agent immediately. */
    record Routed(String agent, String conversationId, double confidence) implements AgentStreamEvent {
    }

    /** An incremental chunk of assistant text. */
    record Delta(String text) implements AgentStreamEvent {
    }

    /** A tool the model decided to execute during this turn. */
    record ToolInvoked(AssistantMessage.ToolCall toolCall) implements AgentStreamEvent {
    }

    /** Terminal success event carrying the metadata that is only known once the stream is exhausted. */
    record Completed(String model, Integer promptTokens, Integer completionTokens) implements AgentStreamEvent {
    }

    /**
     * Terminal failure event. Errors that happen after the response has been committed cannot become an HTTP status,
     * so they are delivered in-band and the stream is completed normally.
     */
    record Failed(String message) implements AgentStreamEvent {
    }
}

