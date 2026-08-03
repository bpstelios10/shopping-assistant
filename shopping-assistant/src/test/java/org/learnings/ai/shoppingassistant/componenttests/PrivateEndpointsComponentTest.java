package org.learnings.ai.shoppingassistant.componenttests;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("component-test-actuator")
public class PrivateEndpointsComponentTest extends AbstractComponentTestWithMockedExternals {

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

    // Uncomment when add test containers. no point to do healthcheck for mocked dependencies
//    @Test
//    void getActuatorHealth() throws Exception {
//        ]mockMvc.perform(get("/shopping-assistant/private/health"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$['status']").value("UP"))
//                .andExpect(content().string(containsString("liveness")))
//                .andExpect(content().string(containsString("readiness")));
//    }

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
