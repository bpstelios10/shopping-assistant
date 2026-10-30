package org.learnings.ai.shoppingassistant.config;

import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class InMemorySessionConfig {

    @Bean
    SessionRepository sessionRepository() {
        return InMemorySessionRepository.builder().build();
    }
}
