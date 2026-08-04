package org.learnings.ai.shoppingassistant.config;

import org.learnings.ai.shoppingassistant.infrastructure.orders.RestOrderClient;
import org.learnings.ai.shoppingassistant.infrastructure.products.RestProductClient;
import org.learnings.ai.shoppingassistant.services.orders.OrderClient;
import org.learnings.ai.shoppingassistant.services.products.ProductClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DownstreamClientsProperties.class)
public class RestClientsConfig {

    @Bean
    ProductClient productClient(RestClientFactory factory, DownstreamClientsProperties properties) {
        return new RestProductClient(factory.create(properties.services().get("products")));
    }

    @Bean
    OrderClient orderClient(RestClientFactory factory, DownstreamClientsProperties properties) {
        return new RestOrderClient(factory.create(properties.services().get("orders")));
    }
}
