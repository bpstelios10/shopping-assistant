package org.learnings.ai.shoppingassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        basePackages = "org.learnings.ai.shoppingassistant",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "org\\.learnings\\.ai\\.shoppingassistant\\.rag\\.loaddocs\\..*"
        )
)
public class ShoppingAssistantApplication {

    static void main(String[] args) {
        SpringApplication.run(ShoppingAssistantApplication.class, args);
    }
}
