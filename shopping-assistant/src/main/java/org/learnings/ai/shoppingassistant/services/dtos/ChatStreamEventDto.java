package org.learnings.ai.shoppingassistant.services.dtos;

public sealed interface ChatStreamEventDto {

    String ROUTED = "routed";
    String DELTA = "delta";
    String TOOL = "tool";
    String DONE = "done";
    String ERROR = "error";

    String event();

    record Routed(String agent, String conversationId, double confidence) implements ChatStreamEventDto {
        @Override
        public String event() {
            return ROUTED;
        }
    }

    record Delta(String text) implements ChatStreamEventDto {
        @Override
        public String event() {
            return DELTA;
        }
    }

    record ToolCalled(String id, String type, String name, String arguments) implements ChatStreamEventDto {
        @Override
        public String event() {
            return TOOL;
        }
    }

    record Done(String model, Integer promptTokens, Integer completionTokens) implements ChatStreamEventDto {
        @Override
        public String event() {
            return DONE;
        }
    }

    record Error(String message) implements ChatStreamEventDto {
        @Override
        public String event() {
            return ERROR;
        }
    }
}
