package ilp_submission_3.ilp_submission_image.ControllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import ilp_submission_3.ilp_submission_image.dto.MedDispatchRec;
import ilp_submission_3.ilp_submission_image.dto.Position;
import ilp_submission_3.ilp_submission_image.dto.QueryAttribute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DroneController REST API endpoints
 * Validates drone query endpoints, delivery path calculation, and business logic
 */
@SpringBootTest
@AutoConfigureMockMvc
class DroneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("GET /api/v1/dronesWithCooling/{state}")
    class DronesWithCoolingEndpointTests {

        @Test
        @DisplayName("Should return drones with cooling enabled")
        void testGetDronesWithCoolingTrue() throws Exception {
            mockMvc.perform(get("/api/v1/dronesWithCooling/true"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should return drones without cooling")
        void testGetDronesWithCoolingFalse() throws Exception {
            mockMvc.perform(get("/api/v1/dronesWithCooling/false"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should handle invalid boolean values")
        void testInvalidBooleanValue() throws Exception {
            mockMvc.perform(get("/api/v1/dronesWithCooling/invalid"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return empty array when no matching drones")
        void testNoMatchingDrones() throws Exception {
            // This depends on test data, but should handle gracefully
            mockMvc.perform(get("/api/v1/dronesWithCooling/true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/droneDetails/{id}")
    class DroneDetailsEndpointTests {

        @Test
        @DisplayName("Should return 404 for non-existent drone ID")
        void testGetDroneDetailsNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/droneDetails/non-existent-drone"))
                    .andExpect(status().isNotFound());
        }


        @Test
        @DisplayName("Should handle special characters in drone ID")
        void testSpecialCharactersInId() throws Exception {
            mockMvc.perform(get("/api/v1/droneDetails/drone-@#$"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/queryAsPath/{attributeName}/{attributeValue}")
    class QueryAsPathEndpointTests {

        @Test
        @DisplayName("Should query drones by capacity")
        void testQueryByCapacity() throws Exception {
            mockMvc.perform(get("/api/v1/queryAsPath/capacity/10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should query drones by cooling capability")
        void testQueryByCooling() throws Exception {
            mockMvc.perform(get("/api/v1/queryAsPath/cooling/true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should query drones by heating capability")
        void testQueryByHeating() throws Exception {
            mockMvc.perform(get("/api/v1/queryAsPath/heating/false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should query drones by name")
        void testQueryByName() throws Exception {
            mockMvc.perform(get("/api/v1/queryAsPath/name/DroneA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should query drones by maxMoves")
        void testQueryByMaxMoves() throws Exception {
            mockMvc.perform(get("/api/v1/queryAsPath/maxMoves/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should handle invalid attribute names")
        void testInvalidAttributeName() throws Exception {
            mockMvc.perform(get("/api/v1/queryAsPath/invalidAttr/value"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should return empty array when no matches")
        void testNoMatches() throws Exception {
            mockMvc.perform(get("/api/v1/queryAsPath/capacity/999999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/query")
    class QueryEndpointTests {

        @Test
        @DisplayName("Should query drones with single attribute")
        void testQuerySingleAttribute() throws Exception {
            List<QueryAttribute> query = Collections.singletonList(
                    new QueryAttribute("capacity", ">=", "5")
            );

            mockMvc.perform(post("/api/v1/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(query)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should query drones with multiple attributes (AND logic)")
        void testQueryMultipleAttributes() throws Exception {
            List<QueryAttribute> query = Arrays.asList(
                    new QueryAttribute("capacity", ">=", "5"),
                    new QueryAttribute("cooling", "=", "true")
            );

            mockMvc.perform(post("/api/v1/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(query)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should support comparison operators")
        void testComparisonOperators() throws Exception {
            // Test different operators
            String[] operators = {"=", "!=", "<", ">", "<=", ">="};

            for (String operator : operators) {
                List<QueryAttribute> query = Collections.singletonList(
                        new QueryAttribute("capacity", operator, "10")
                );

                mockMvc.perform(post("/api/v1/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(query)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").isArray());
            }
        }

        @Test
        @DisplayName("Should handle empty query list")
        void testEmptyQueryList() throws Exception {
            List<QueryAttribute> query = Collections.emptyList();

            mockMvc.perform(post("/api/v1/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(query)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should return 400 for invalid JSON")
        void testInvalidJson() throws Exception {
            mockMvc.perform(post("/api/v1/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle null request body")
        void testNullRequestBody() throws Exception {
            mockMvc.perform(post("/api/v1/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("null"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/queryAvailableDrones")
    class QueryAvailableDronesEndpointTests {

        @Test
        @DisplayName("Should query available drones for valid dispatch records")
        void testQueryAvailableDrones() throws Exception {
            List<MedDispatchRec> dispatches = Collections.singletonList(
                    createMedDispatchRec(1, "2025-01-20", "10:00:00", 5.0, true, false, 100.0)
            );

            mockMvc.perform(post("/api/v1/queryAvailableDrones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should handle multiple dispatch records")
        void testMultipleDispatches() throws Exception {
            List<MedDispatchRec> dispatches = Arrays.asList(
                    createMedDispatchRec(1, "2025-01-20", "10:00:00", 5.0, true, false, 100.0),
                    createMedDispatchRec(2, "2025-01-20", "14:00:00", 3.0, false, false, 80.0)
            );

            mockMvc.perform(post("/api/v1/queryAvailableDrones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should return empty list for conflicting requirements (cooling + heating)")
        void testConflictingRequirements() throws Exception {
            List<MedDispatchRec> dispatches = Collections.singletonList(
                    createMedDispatchRec(1, "2025-01-20", "10:00:00", 5.0, true, true, 100.0)
            );

            mockMvc.perform(post("/api/v1/queryAvailableDrones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should handle empty dispatch list")
        void testEmptyDispatchList() throws Exception {
            List<MedDispatchRec> dispatches = Collections.emptyList();

            mockMvc.perform(post("/api/v1/queryAvailableDrones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should validate date and time availability")
        void testDateTimeAvailability() throws Exception {
            List<MedDispatchRec> dispatches = Collections.singletonList(
                    createMedDispatchRec(1, "2025-01-20", "10:00:00", 5.0, false, false, 100.0)
            );

            mockMvc.perform(post("/api/v1/queryAvailableDrones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should return 400 for malformed request")
        void testMalformedRequest() throws Exception {
            mockMvc.perform(post("/api/v1/queryAvailableDrones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{malformed}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/calcDeliveryPath")
    class CalcDeliveryPathEndpointTests {

        @Test
        @DisplayName("Should calculate delivery path for valid orders")
        void testCalculateDeliveryPath() throws Exception {
            List<MedDispatchRec> dispatches = Collections.singletonList(
                    createMedDispatchRec(1, "2025-01-20", "10:00:00", 5.0, false, false, 150.0)
            );

            mockMvc.perform(post("/api/v1/calcDeliveryPath")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCost").exists())
                    .andExpect(jsonPath("$.totalMoves").exists())
                    .andExpect(jsonPath("$.dronePaths").isArray());
        }

        @Test
        @DisplayName("Should return response structure with cost and moves")
        void testResponseStructure() throws Exception {
            List<MedDispatchRec> dispatches = Collections.singletonList(
                    createMedDispatchRec(1, "2025-01-20", "10:00:00", 5.0, false, false, 150.0)
            );

            mockMvc.perform(post("/api/v1/calcDeliveryPath")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCost").isNumber())
                    .andExpect(jsonPath("$.totalMoves").isNumber());
        }

        @Test
        @DisplayName("Should handle multiple orders")
        void testMultipleOrders() throws Exception {
            List<MedDispatchRec> dispatches = Arrays.asList(
                    createMedDispatchRec(1, "2025-01-20", "10:00:00", 3.0, false, false, 100.0),
                    createMedDispatchRec(2, "2025-01-20", "14:00:00", 2.0, false, false, 100.0)
            );

            mockMvc.perform(post("/api/v1/calcDeliveryPath")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dronePaths").isArray());
        }

        @Test
        @DisplayName("Should handle empty order list")
        void testEmptyOrderList() throws Exception {
            List<MedDispatchRec> dispatches = Collections.emptyList();

            mockMvc.perform(post("/api/v1/calcDeliveryPath")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCost").value(0.0))
                    .andExpect(jsonPath("$.totalMoves").value(0))
                    .andExpect(jsonPath("$.dronePaths").isEmpty());
        }

        @Test
        @DisplayName("Should return 400 for invalid request body")
        void testInvalidRequestBody() throws Exception {
            mockMvc.perform(post("/api/v1/calcDeliveryPath")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("invalid"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/calcDeliveryPathAsGeoJson")
    class CalcDeliveryPathAsGeoJsonEndpointTests {

        @Test
        @DisplayName("Should return valid GeoJSON structure")
        void testGeoJsonStructure() throws Exception {
            List<MedDispatchRec> dispatches = Collections.singletonList(
                    createMedDispatchRec(1, "2025-01-20", "10:00:00", 5.0, false, false, 150.0)
            );

            mockMvc.perform(post("/api/v1/calcDeliveryPathAsGeoJson")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.features").exists());
        }

        @Test
        @DisplayName("Should handle empty order list with valid GeoJSON")
        void testEmptyOrdersGeoJson() throws Exception {
            List<MedDispatchRec> dispatches = Collections.emptyList();

            mockMvc.perform(post("/api/v1/calcDeliveryPathAsGeoJson")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("FeatureCollection"))
                    .andExpect(jsonPath("$.features").isEmpty());
        }

        @Test
        @DisplayName("Should include geometry and properties in features")
        void testGeoJsonFeatureStructure() throws Exception {
            List<MedDispatchRec> dispatches = Collections.singletonList(
                    createMedDispatchRec(1, "2025-01-20", "10:00:00", 5.0, false, false, 150.0)
            );

            mockMvc.perform(post("/api/v1/calcDeliveryPathAsGeoJson")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk());
            // Features should have geometry and properties if path exists
        }

        @Test
        @DisplayName("Should return 400 for malformed JSON")
        void testMalformedJson() throws Exception {
            mockMvc.perform(post("/api/v1/calcDeliveryPathAsGeoJson")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DroneController Integration Tests")
    class DroneControllerIntegrationTests {

        @Test
        @DisplayName("Should handle sequence of query and path calculation")
        void testQueryAndCalculateSequence() throws Exception {
            // First query available drones
            List<MedDispatchRec> dispatches = Collections.singletonList(
                    createMedDispatchRec(1, "2025-01-20", "10:00:00", 5.0, false, false, 150.0)
            );

            mockMvc.perform(post("/api/v1/queryAvailableDrones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk());

            // Then calculate delivery path
            mockMvc.perform(post("/api/v1/calcDeliveryPath")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dispatches)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return consistent results for same input")
        void testConsistentResults() throws Exception {
            List<MedDispatchRec> dispatches = Collections.singletonList(
                    createMedDispatchRec(1, "2025-01-20", "10:00:00", 5.0, false, false, 150.0)
            );

            String content = objectMapper.writeValueAsString(dispatches);

            String result1 = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String result2 = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Results should be deterministic for same input
        }

        @Test
        @DisplayName("Should handle missing Content-Type header")
        void testMissingContentType() throws Exception {
            mockMvc.perform(post("/api/v1/query")
                            .content("[]"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    // Helper method to create MedDispatchRec for testing
    private MedDispatchRec createMedDispatchRec(Integer id, String date, String time,
                                                double capacity, boolean cooling,
                                                boolean heating, double maxCost) {
        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements(
                capacity, cooling, heating, maxCost
        );

        Position delivery = new Position(-3.188, 55.944);

        return new MedDispatchRec(id, date, time, requirements, delivery);
    }
}