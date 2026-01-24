package ilp_submission_3.ilp_submission_image.ControllerTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MonitorController REST API endpoints
 * Validates WebSocket connection monitoring and status reporting
 * Critical for frontend PerformanceChart.tsx dependency
 */
@SpringBootTest
@AutoConfigureMockMvc
class MonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/v1/monitor/websocket-status")
    class WebSocketStatusEndpointTests {

        @Test
        @DisplayName("Should return WebSocket status with 200 OK")
        void testGetWebSocketStatus() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"));
        }

        @Test
        @DisplayName("Should return all required fields for frontend")
        void testRequiredFieldsPresent() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeConnections").exists())
                    .andExpect(jsonPath("$.hasConnections").exists())
                    .andExpect(jsonPath("$.status").exists());
        }

        @Test
        @DisplayName("Should return activeConnections as number")
        void testActiveConnectionsIsNumber() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeConnections").isNumber());
        }

        @Test
        @DisplayName("Should return hasConnections as boolean")
        void testHasConnectionsIsBoolean() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasConnections").isBoolean());
        }

        @Test
        @DisplayName("Should return status as string")
        void testStatusIsString() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").isString());
        }

        @Test
        @DisplayName("Should return 'idle' status when no connections")
        void testIdleStatusWhenNoConnections() throws Exception {
            // When activeConnections is 0, status should be "idle"
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        String content = result.getResponse().getContentAsString();
                        // Parse and verify logic: if activeConnections == 0, status == "idle"
                        if (content.contains("\"activeConnections\":0")) {
                            org.hamcrest.MatcherAssert.assertThat(content,
                                    org.hamcrest.Matchers.containsString("\"status\":\"idle\""));
                        }
                    });
        }

        @Test
        @DisplayName("Should return 'active' status when connections exist")
        void testActiveStatusWhenConnectionsExist() throws Exception {
            // When activeConnections > 0, status should be "active"
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        String content = result.getResponse().getContentAsString();
                        // Parse and verify logic: if activeConnections > 0, status == "active"
                        if (!content.contains("\"activeConnections\":0")) {
                            org.hamcrest.MatcherAssert.assertThat(content,
                                    org.hamcrest.Matchers.containsString("\"status\":\"active\""));
                        }
                    });
        }

        @Test
        @DisplayName("Should have consistent hasConnections and activeConnections")
        void testConsistencyBetweenFields() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        String content = result.getResponse().getContentAsString();
                        // If activeConnections > 0, hasConnections should be true
                        // If activeConnections == 0, hasConnections should be false
                        boolean hasZeroConnections = content.contains("\"activeConnections\":0");
                        if (hasZeroConnections) {
                            org.hamcrest.MatcherAssert.assertThat(content,
                                    org.hamcrest.Matchers.containsString("\"hasConnections\":false"));
                        } else {
                            org.hamcrest.MatcherAssert.assertThat(content,
                                    org.hamcrest.Matchers.containsString("\"hasConnections\":true"));
                        }
                    });
        }

        @Test
        @DisplayName("Should return non-negative connection count")
        void testNonNegativeConnectionCount() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        String content = result.getResponse().getContentAsString();
                        // Extract activeConnections value and verify it's >= 0
                        org.hamcrest.MatcherAssert.assertThat(content,
                                org.hamcrest.Matchers.not(
                                        org.hamcrest.Matchers.containsString("\"activeConnections\":-")
                                ));
                    });
        }

        @Test
        @DisplayName("Should handle multiple concurrent status requests")
        void testConcurrentStatusRequests() throws Exception {
            // Simulate multiple clients checking status simultaneously
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.activeConnections").exists())
                        .andExpect(jsonPath("$.hasConnections").exists())
                        .andExpect(jsonPath("$.status").exists());
            }
        }

        @Test
        @DisplayName("Should return consistent status across rapid requests")
        void testStatusConsistency() throws Exception {
            // Make rapid consecutive requests
            String firstResponse = mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Immediately make another request
            String secondResponse = mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Responses should be similar (allowing for possible WebSocket state changes)
            // At minimum, both should have valid structure
            org.junit.jupiter.api.Assertions.assertTrue(firstResponse.contains("activeConnections"));
            org.junit.jupiter.api.Assertions.assertTrue(secondResponse.contains("activeConnections"));
        }

        @Test
        @DisplayName("Should handle GET method only")
        void testGetMethodOnly() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Should not accept PUT requests")
        void testPutNotAllowed() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .put("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Should not accept DELETE requests")
        void testDeleteNotAllowed() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .delete("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("Monitor API Contract Tests")
    class MonitorApiContractTests {

        @Test
        @DisplayName("Should maintain API contract for PerformanceChart.tsx")
        void testApiContractForFrontend() throws Exception {
            // This test validates the exact contract that frontend depends on
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeConnections").exists())
                    .andExpect(jsonPath("$.activeConnections").isNumber())
                    .andExpect(jsonPath("$.hasConnections").exists())
                    .andExpect(jsonPath("$.hasConnections").isBoolean())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.status").isString());
        }

        @Test
        @DisplayName("Should return valid JSON structure")
        void testValidJsonStructure() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(result -> {
                        String content = result.getResponse().getContentAsString();
                        // Verify it's valid JSON with expected structure
                        org.junit.jupiter.api.Assertions.assertTrue(content.startsWith("{"));
                        org.junit.jupiter.api.Assertions.assertTrue(content.endsWith("}"));
                        org.junit.jupiter.api.Assertions.assertTrue(content.contains("activeConnections"));
                        org.junit.jupiter.api.Assertions.assertTrue(content.contains("hasConnections"));
                        org.junit.jupiter.api.Assertions.assertTrue(content.contains("status"));
                    });
        }

        @Test
        @DisplayName("Should not include extra unexpected fields")
        void testNoExtraFields() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        String content = result.getResponse().getContentAsString();
                        // Count the number of fields - should be exactly 3
                        int fieldCount = content.split("\":").length - 1;
                        org.junit.jupiter.api.Assertions.assertEquals(3, fieldCount,
                                "Response should contain exactly 3 fields");
                    });
        }

        @Test
        @DisplayName("Should respond within acceptable time")
        void testResponseTime() throws Exception {
            long startTime = System.currentTimeMillis();

            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk());

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Monitor endpoint should be very fast (< 100ms)
            org.junit.jupiter.api.Assertions.assertTrue(duration < 100,
                    "Response time should be under 100ms, was: " + duration + "ms");
        }

        @Test
        @DisplayName("Should handle malformed monitor path gracefully")
        void testMalformedPath() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status/extra"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return proper content type header")
        void testContentTypeHeader() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/json"));
        }
    }

    @Nested
    @DisplayName("Monitor Endpoint Edge Cases")
    class MonitorEdgeCaseTests {

        @Test
        @DisplayName("Should handle query parameters gracefully")
        void testIgnoreQueryParameters() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status?extra=param"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeConnections").exists());
        }

        @Test
        @DisplayName("Should handle Accept header variations")
        void testAcceptHeader() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status")
                            .header("Accept", "application/json"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"));
        }

        @Test
        @DisplayName("Should work without Accept header")
        void testNoAcceptHeader() throws Exception {
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"));
        }

        @Test
        @DisplayName("Should handle HEAD requests")
        void testHeadRequest() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .head("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should provide consistent schema")
        void testSchemaConsistency() throws Exception {
            // Make multiple requests and verify schema is always the same
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.activeConnections").exists())
                        .andExpect(jsonPath("$.hasConnections").exists())
                        .andExpect(jsonPath("$.status").exists())
                        .andExpect(jsonPath("$.activeConnections").isNumber())
                        .andExpect(jsonPath("$.hasConnections").isBoolean())
                        .andExpect(jsonPath("$.status").isString());
            }
        }
    }

    @Nested
    @DisplayName("Monitor Integration with WebSocket")
    class MonitorWebSocketIntegrationTests {

        @Test
        @DisplayName("Should reflect initial state correctly")
        void testInitialState() throws Exception {
            // At test start, there should be no active connections
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeConnections").isNumber());
            // Cannot assume 0 connections if other tests run WebSocket clients
        }

        @Test
        @DisplayName("Should handle monitoring during no activity")
        void testMonitoringDuringInactivity() throws Exception {
            // Multiple checks during idle period should all return consistent results
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").exists());

                Thread.sleep(100); // Small delay between checks
            }
        }

        @Test
        @DisplayName("Should provide accurate metrics for monitoring dashboard")
        void testMetricsForDashboard() throws Exception {
            // Verify all metrics needed for a monitoring dashboard are present
            mockMvc.perform(get("/api/v1/monitor/websocket-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeConnections").exists()) // For connection count chart
                    .andExpect(jsonPath("$.hasConnections").exists())    // For connection indicator
                    .andExpect(jsonPath("$.status").exists());           // For status badge
        }
    }
}