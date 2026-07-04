package org.learnings.ai.shoppingassistant.config;

import org.learnings.ai.shoppingassistant.infrastructure.products.RestProductClient;
import org.learnings.ai.shoppingassistant.services.products.ProductClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DownstreamClientsProperties.class)
public class ProductClientConfig {

    @Bean
    ProductClient productClient(RestClientFactory factory, DownstreamClientsProperties properties) {
        return new RestProductClient(factory.create(properties.services().get("products")));
    }
}
