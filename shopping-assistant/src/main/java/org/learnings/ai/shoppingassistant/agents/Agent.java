package org.learnings.ai.shoppingassistant.agents;

import reactor.core.publisher.Flux;

public interface Agent {

    AgentChatResult chat(AgentRequest request);

    /**
     * Streaming counterpart of {@link #chat(AgentRequest)}.
     *
     * <p>The returned flux is cold: nothing is sent to the model until it is subscribed, and cancelling the
     * subscription (a shopper closing the tab) propagates upstream and abandons the inference.
     */
    Flux<AgentStreamEvent> chatStream(AgentRequest request);

    String name();
}
