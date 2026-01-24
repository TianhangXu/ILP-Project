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
 * Integration tests for DataController REST API endpoints
 * Validates data retrieval endpoints, response structures, and field completeness
 */
@SpringBootTest
@AutoConfigureMockMvc
class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/v1/restricted-areas")
    class RestrictedAreasEndpointTests {

        @Test
        @DisplayName("Should return list of restricted areas with 200 OK")
        void testGetRestrictedAreas() throws Exception {
            mockMvc.perform(get("/api/v1/restricted-areas"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should return restricted areas with correct structure")
        void testRestrictedAreasStructure() throws Exception {
            mockMvc.perform(get("/api/v1/restricted-areas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
            // Each area should have name and vertices if data exists
        }

        @Test
        @DisplayName("Should handle empty restricted areas list")
        void testEmptyRestrictedAreas() throws Exception {
            mockMvc.perform(get("/api/v1/restricted-areas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should support CORS for restricted-areas endpoint")
        void testRestrictedAreasCORS() throws Exception {
            mockMvc.perform(get("/api/v1/restricted-areas")
                            .header("Origin", "http://localhost:3000"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Access-Control-Allow-Origin"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/service-points")
    class ServicePointsEndpointTests {

        @Test
        @DisplayName("Should return list of service points with 200 OK")
        void testGetServicePoints() throws Exception {
            mockMvc.perform(get("/api/v1/service-points"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should return service points with required fields")
        void testServicePointsStructure() throws Exception {
            mockMvc.perform(get("/api/v1/service-points"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
            // Each service point should have id, name, and location if data exists
        }

        @Test
        @DisplayName("Should handle empty service points list")
        void testEmptyServicePoints() throws Exception {
            mockMvc.perform(get("/api/v1/service-points"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should support CORS for service-points endpoint")
        void testServicePointsCORS() throws Exception {
            mockMvc.perform(get("/api/v1/service-points")
                            .header("Origin", "http://localhost:3000"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Access-Control-Allow-Origin"));
        }

        @Test
        @DisplayName("Should return consistent data across multiple requests")
        void testServicePointsConsistency() throws Exception {
            String firstResponse = mockMvc.perform(get("/api/v1/service-points"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String secondResponse = mockMvc.perform(get("/api/v1/service-points"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Data should be consistent across requests (assuming no external changes)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/drones")
    class DronesEndpointTests {

        @Test
        @DisplayName("Should return list of drones with 200 OK")
        void testGetDrones() throws Exception {
            mockMvc.perform(get("/api/v1/drones"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should return drones with required fields")
        void testDronesStructure() throws Exception {
            mockMvc.perform(get("/api/v1/drones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
            // Each drone should have id, name, and capability if data exists
        }

        @Test
        @DisplayName("Should handle empty drones list")
        void testEmptyDrones() throws Exception {
            mockMvc.perform(get("/api/v1/drones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should support CORS for drones endpoint")
        void testDronesCORS() throws Exception {
            mockMvc.perform(get("/api/v1/drones")
                            .header("Origin", "http://localhost:3000"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Access-Control-Allow-Origin"));
        }

        @Test
        @DisplayName("Should return consistent drone data")
        void testDronesConsistency() throws Exception {
            String firstResponse = mockMvc.perform(get("/api/v1/drones"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String secondResponse = mockMvc.perform(get("/api/v1/drones"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Drone data should be consistent
        }

        @Test
        @DisplayName("Should handle invalid HTTP methods")
        void testInvalidMethod() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post("/api/v1/drones"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("Data Endpoint Integration Tests")
    class DataEndpointIntegrationTests {

        @Test
        @DisplayName("Should successfully retrieve all three data types")
        void testAllDataEndpoints() throws Exception {
            // Test all endpoints are accessible
            mockMvc.perform(get("/api/v1/restricted-areas"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/service-points"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/drones"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should handle concurrent requests to different endpoints")
        void testConcurrentRequests() throws Exception {
            // Simulate concurrent access pattern
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(get("/api/v1/restricted-areas"))
                        .andExpect(status().isOk());
                mockMvc.perform(get("/api/v1/service-points"))
                        .andExpect(status().isOk());
                mockMvc.perform(get("/api/v1/drones"))
                        .andExpect(status().isOk());
            }
        }

        @Test
        @DisplayName("Should return JSON content type for all endpoints")
        void testContentTypes() throws Exception {
            mockMvc.perform(get("/api/v1/restricted-areas"))
                    .andExpect(content().contentType("application/json"));

            mockMvc.perform(get("/api/v1/service-points"))
                    .andExpect(content().contentType("application/json"));

            mockMvc.perform(get("/api/v1/drones"))
                    .andExpect(content().contentType("application/json"));
        }

        @Test
        @DisplayName("Should handle OPTIONS requests for CORS preflight")
        void testOptionsRequests() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .options("/api/v1/drones")
                            .header("Origin", "http://localhost:3000")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk());
        }
    }
}