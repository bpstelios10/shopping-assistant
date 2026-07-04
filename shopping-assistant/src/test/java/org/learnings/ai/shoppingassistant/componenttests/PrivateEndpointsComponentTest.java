package org.learnings.ai.shoppingassistant.componenttests;

import org.junit.jupiter.api.Test;
import org.learnings.ai.shoppingassistant.services.products.ProductClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("component-test-actuator")
public class PrivateEndpointsComponentTest {

    @Autowired
    private MockMvc mockMvc;
    // Mock the product backend so the context doesn't need the real Go service.
    @MockitoBean
    private ProductClient productClient;

    @Test
    void getActuatorLinks() throws Exception {
        mockMvc.perform(get("/shopping-assistant/private").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['_links']").isNotEmpty());
    }

    @Test
    void getActuatorMetrics() throws Exception {
        mockMvc.perform(get("/shopping-assistant/private/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("application_started_time_seconds{main_application_class=\"org.learnings.ai.shoppingassistant.componenttests.PrivateEndpointsComponentTest\"} ")));
    }

    @Test
    void getActuatorConfigProps() throws Exception {
        mockMvc.perform(get("/shopping-assistant/private/configprops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts.shopping-assistant.beans").isNotEmpty());
    }

    @Test
    void getActuatorEnv() throws Exception {
        mockMvc.perform(get("/shopping-assistant/private/env"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeProfiles").value("component-test-actuator"));
    }

    @Test
    void getActuatorHeapdump() throws Exception {
        mockMvc.perform(get("/shopping-assistant/private/heapdump"))
                .andExpect(status().isOk());
    }

    @Test
    void getActuatorHealth() throws Exception {
        mockMvc.perform(get("/shopping-assistant/private/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['status']").value("UP"))
                .andExpect(content().string(containsString("liveness")))
                .andExpect(content().string(containsString("readiness")));
    }

    @Test
    void getActuatorLivenessCheck() throws Exception {
        mockMvc.perform(get("/shopping-assistant/private/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['status']").value("UP"));
    }

    @Test
    void getActuatorReadinessCheck() throws Exception {
        mockMvc.perform(get("/shopping-assistant/private/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['status']").value("UP"));
    }
}
