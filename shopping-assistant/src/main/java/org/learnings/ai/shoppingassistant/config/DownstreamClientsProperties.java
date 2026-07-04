package org.learnings.ai.shoppingassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Config for downstream HTTP services, keyed by service name (e.g. "products").
 * Adding a new backend (orders, users, ...) is just another entry under
 * {@code clients.services.<name>} — no new class needed.
 */
@ConfigurationProperties(prefix = "clients")
public record DownstreamClientsProperties(Map<String, ClientConfig> services) {

    public record ClientConfig(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    }
}
