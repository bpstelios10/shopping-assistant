package org.learnings.ai.shoppingassistant.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@TestConfiguration
public class TestRestClientConfig {

    private MockRestServiceServer mockServer;

    @Bean
    RestClient.Builder builder() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://shopping-assistant");

        mockServer = MockRestServiceServer.bindTo(builder).build();

        return builder;
    }

    // needs to be created before the actual clients are created. so i do this in builder() and here we just return it
    @Bean
    MockRestServiceServer mockRestServiceServer() {
        return mockServer;
    }

    // i need to override this cause the real one uses .requestFactory() that damages the mock-server
    @Bean
    @Primary
    RestClientFactory restClientFactory(RestClient.Builder builder) {
        return new RestClientFactory(builder) {
            @Override
            public RestClient create(DownstreamClientsProperties.ClientConfig config) {
                return builder.clone()
                        .baseUrl(config.baseUrl())
                        .build();
            }
        };
    }
}
