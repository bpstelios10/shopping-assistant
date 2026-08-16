package org.learnings.ai.shoppingassistant.infrastructure.restclient;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.learnings.ai.shoppingassistant.web.filters.RequestIdFilter.MDC_KEY;
import static org.learnings.ai.shoppingassistant.web.filters.RequestIdFilter.REQUEST_ID_HEADER;

public class RequestIdInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public @NonNull ClientHttpResponse intercept(@NonNull HttpRequest request, byte @NonNull [] body,
                                                 @NonNull ClientHttpRequestExecution execution) throws IOException {
        String requestId = MDC.get(MDC_KEY);

        if (requestId != null && !requestId.isBlank()) {
            request.getHeaders().set(REQUEST_ID_HEADER, requestId);
        }

        return execution.execute(request, body);
    }
}
