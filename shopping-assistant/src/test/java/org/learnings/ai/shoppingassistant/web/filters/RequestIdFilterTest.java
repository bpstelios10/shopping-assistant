package org.learnings.ai.shoppingassistant.web.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private static final String HEADER_NAME = "X-Request-ID";
    private static final String MDC_KEY = "requestId";
    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String GENERATED_UUID = "999e9999-e99b-99d3-a999-999999999999";

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilterInternal_whenHeaderIsValidUuid_usesClientProvidedUuid() throws ServletException, IOException {
        when(request.getHeader(HEADER_NAME)).thenReturn(VALID_UUID);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(HEADER_NAME, VALID_UUID);
        verify(filterChain).doFilter(request, response);
        assertThat(MDC.get(MDC_KEY)).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\n\r", "not-a-uuid", "123e4567-e89b-12d3-a456"})
    void doFilterInternal_whenHeaderIsMissingOrInvalid_generatesRandomUuid(String invalidHeaderValue) throws ServletException, IOException {
        when(request.getHeader(HEADER_NAME)).thenReturn(invalidHeaderValue);
        UUID mockedUuid = UUID.fromString(GENERATED_UUID);

        try (MockedStatic<UUID> mockedStaticUuid = mockStatic(UUID.class)) {
            mockedStaticUuid.when(UUID::randomUUID).thenReturn(mockedUuid);

            filter.doFilterInternal(request, response, filterChain);

            mockedStaticUuid.verify(UUID::randomUUID, times(1));
            verify(response).setHeader(HEADER_NAME, GENERATED_UUID);
        }

        verify(filterChain).doFilter(request, response);
        assertThat(MDC.get(MDC_KEY)).isNull();
    }

    @Test
    void doFilterInternal_setsMdc() throws ServletException, IOException {
        when(request.getHeader(HEADER_NAME)).thenReturn(VALID_UUID);
        doAnswer(_ -> {
            assertThat(MDC.get(MDC_KEY)).isEqualTo(VALID_UUID);
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get(MDC_KEY)).isNull();
    }
}
