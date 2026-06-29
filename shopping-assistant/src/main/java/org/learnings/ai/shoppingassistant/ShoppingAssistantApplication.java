package org.learnings.ai.shoppingassistant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class ShoppingAssistantApplication {

    static void main(String[] args) {
        log.debug("Starting the app");

        SpringApplication.run(ShoppingAssistantApplication.class, args);
    }
}
