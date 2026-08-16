package org.learnings.ai.shoppingassistant.infrastructure.restclient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.learnings.ai.shoppingassistant.web.filters.RequestIdFilter.MDC_KEY;
import static org.learnings.ai.shoppingassistant.web.filters.RequestIdFilter.REQUEST_ID_HEADER;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestIdInterceptorTest {

    @Mock
    private HttpRequest request;
    @Mock
    private ClientHttpRequestExecution execution;
    @Mock
    private ClientHttpResponse response;
    private final RequestIdInterceptor interceptor = new RequestIdInterceptor();

    private static final String TEST_UUID = "123e4567-e89b-12d3-a456-426614174000";

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void intercept_whenMdcContainsRequestId_addsHeader() throws IOException {
        MDC.put(MDC_KEY, TEST_UUID);
        byte[] body = new byte[0];
        when(execution.execute(request, body)).thenReturn(response);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        ClientHttpResponse actualResponse = interceptor.intercept(request, body, execution);

        assertThat(headers.getFirst(REQUEST_ID_HEADER)).isEqualTo(TEST_UUID);
        assertThat(actualResponse).isSameAs(response);
        verify(execution).execute(request, body);
    }

    @Test
    void intercept_whenNoMdc_addsNothing() throws IOException {
        byte[] body = new byte[0];
        when(execution.execute(request, body)).thenReturn(response);

        ClientHttpResponse actualResponse = interceptor.intercept(request, body, execution);

        assertThat(actualResponse).isSameAs(response);
        verify(execution).execute(request, body);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\r"})
    void intercept_whenBlankMdc_addsNothing(String mdcValue) throws IOException {
        MDC.put(MDC_KEY, mdcValue);
        byte[] body = new byte[0];
        when(execution.execute(request, body)).thenReturn(response);

        ClientHttpResponse actualResponse = interceptor.intercept(request, body, execution);

        assertThat(actualResponse).isSameAs(response);
        verify(execution).execute(request, body);
    }
}
