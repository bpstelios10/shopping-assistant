package org.learnings.ai.shoppingassistant.config;

import org.learnings.ai.shoppingassistant.config.DownstreamClientsProperties.ClientConfig;
import org.learnings.ai.shoppingassistant.infrastructure.restclient.RequestIdInterceptor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Builds a configured {@link RestClient} (base URL + timeouts) from a service's config.
 * Shared factory so every downstream client (products, orders, users, ...) is wired the same way.
 */
@Component
public class RestClientFactory {

    private final RestClient.Builder builder;
    private final RequestIdInterceptor requestIdInterceptor;

    public RestClientFactory(RestClient.Builder builder, RequestIdInterceptor requestIdInterceptor) {
        this.builder = builder;
        this.requestIdInterceptor = requestIdInterceptor;
    }

    public RestClient create(ClientConfig config) {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(config.connectTimeout())
                .withReadTimeout(config.readTimeout());

        return builder.clone()
                .baseUrl(config.baseUrl())
                .requestInterceptor(requestIdInterceptor)
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(settings))
                .build();
    }
}
