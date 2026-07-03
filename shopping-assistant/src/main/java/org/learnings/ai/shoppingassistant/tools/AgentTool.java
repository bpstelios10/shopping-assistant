package org.learnings.ai.shoppingassistant.tools;

/**
 * Marker interface for any component that exposes {@code @Tool}-annotated methods to the agent.
 * <p>
 * Every Spring bean implementing this interface is automatically discovered (injected as a
 * {@code List<AgentTool>}) and registered with the {@code ChatClient}. To add new capabilities,
 * create a new {@code @Component} that implements this interface and annotate its methods with
 * {@code @Tool} — no changes to the agent are required.
 */
public interface AgentTool {
}

