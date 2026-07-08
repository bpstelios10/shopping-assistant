package org.learnings.ai.shoppingassistant.services.dtos;

import org.jspecify.annotations.NonNull;

import java.util.Comparator;

public record Message(String question, String answer, short order) implements Comparable<Message> {

    @Override
    public int compareTo(@NonNull Message o) {
        return Comparator
                .comparingInt(Message::order)
                .thenComparing(Message::question, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Message::answer, Comparator.nullsFirst(Comparator.naturalOrder()))
                .compare(this, o);
    }
}
