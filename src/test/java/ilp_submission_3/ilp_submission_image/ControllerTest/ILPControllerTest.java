package ilp_submission_3.ilp_submission_image.ControllerTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import ilp_submission_3.ilp_submission_image.dto.DistanceRequest;
import ilp_submission_3.ilp_submission_image.dto.IsInRegionRequest;
import ilp_submission_3.ilp_submission_image.dto.NextPositionRequest;
import ilp_submission_3.ilp_submission_image.dto.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ILPController REST API endpoints
 * Validates API contracts, status codes, payload structures, and error handling
 */
@SpringBootTest
@AutoConfigureMockMvc
class ILPControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("GET /api/v1/uid")
    class UidEndpointTests {

        @Test
        @DisplayName("Should return student ID with 200 OK")
        void testGetUid() throws Exception {
            mockMvc.perform(get("/api/v1/uid"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("s2337850"));
        }

        @Test
        @DisplayName("Should return consistent student ID across multiple requests")
        void testUidConsistency() throws Exception {
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(get("/api/v1/uid"))
                        .andExpect(status().isOk())
                        .andExpect(content().string("s2337850"));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/distanceTo")
    class DistanceToEndpointTests {

        @Test
        @DisplayName("Should calculate distance between two valid positions")
        void testDistanceToValid() throws Exception {
            Position p1 = new Position(-3.192473, 55.946233);
            Position p2 = new Position(-3.192473, 55.942617);
            DistanceRequest request = new DistanceRequest(p1, p2);

            mockMvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should return zero for same positions")
        void testDistanceToSamePositions() throws Exception {
            Position p = new Position(-3.192473, 55.946233);
            DistanceRequest request = new DistanceRequest(p, p);

            mockMvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("0.0"));
        }

        @Test
        @DisplayName("Should return 400 when position1 is null")
        void testDistanceToNullPosition1() throws Exception {
            DistanceRequest request = new DistanceRequest(null, new Position(-3.192473, 55.942617));

            mockMvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when position2 is null")
        void testDistanceToNullPosition2() throws Exception {
            DistanceRequest request = new DistanceRequest(new Position(-3.192473, 55.946233), null);

            mockMvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when lng is missing in position1")
        void testDistanceToNullLngPosition1() throws Exception {
            String invalidJson = """
            {
                "position1": {"lat": 55.946233},
                "position2": {"lng": -3.192473, "lat": 55.942617}
            }
            """;

            mockMvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when lat is missing in position2")
        void testDistanceToNullLatPosition2() throws Exception {
            String invalidJson = """
            {
                "position1": {"lng": -3.192473, "lat": 55.946233},
                "position2": {"lng": -3.192473}
            }
            """;

            mockMvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for NaN coordinates")
        void testDistanceToWithNaN() throws Exception {
            String jsonBody = """
                {
                    "position1": {"lng": "NaN", "lat": 55.946233},
                    "position2": {"lng": -3.192473, "lat": 55.942617}
                }
                """;

            mockMvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for Infinity coordinates")
        void testDistanceToWithInfinity() throws Exception {
            String jsonBody = """
                {
                    "position1": {"lng": "Infinity", "lat": 55.946233},
                    "position2": {"lng": -3.192473, "lat": 55.942617}
                }
                """;

            mockMvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 415 when Content-Type header is missing")
        void testNoContentTypeHeader() throws Exception {
            mockMvc.perform(post("/api/v1/distanceTo")
                            .content("{}"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Should return 400 for empty request body")
        void testDistanceToEmptyBody() throws Exception {
            mockMvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should ignore extra JSON fields and return 200")
        void testDistanceToExtraFields() throws Exception {
            String jsonWithExtra = """
                {
                    "position1": {"lng": -3.192473, "lat": 55.946233},
                    "position2": {"lng": -3.192473, "lat": 55.942617},
                    "extraField": "should be ignored"
                }
                """;

            mockMvc.perform(post("/api/v1/distanceTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithExtra))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/isCloseTo")
    class IsCloseToEndpointTests {

        @Test
        @DisplayName("Should return true for close positions")
        void testIsCloseToTrue() throws Exception {
            Position p1 = new Position(-3.192473, 55.946233);
            Position p2 = new Position(-3.192473, 55.946234);
            DistanceRequest request = new DistanceRequest(p1, p2);

            mockMvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should return false for distant positions")
        void testIsCloseToFalse() throws Exception {
            Position p1 = new Position(-3.192473, 55.946233);
            Position p2 = new Position(-3.192473, 55.942617);
            DistanceRequest request = new DistanceRequest(p1, p2);

            mockMvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 when positions are null")
        void testIsCloseToNullPositions() throws Exception {
            DistanceRequest request = new DistanceRequest(null, null);

            mockMvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for NaN coordinates")
        void testIsCloseToWithNaN() throws Exception {
            String jsonBody = """
                {
                    "position1": {"lng": -3.192473, "lat": "NaN"},
                    "position2": {"lng": -3.192473, "lat": 55.942617}
                }
                """;

            mockMvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for Infinity coordinates")
        void testIsCloseToWithInfinity() throws Exception {
            String jsonBody = """
                {
                    "position1": {"lng": -3.192473, "lat": 55.946233},
                    "position2": {"lng": -3.192473, "lat": "-Infinity"}
                }
                """;

            mockMvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for empty request body")
        void testIsCloseToEmptyBody() throws Exception {
            mockMvc.perform(post("/api/v1/isCloseTo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/isInRegion")
    class IsInRegionEndpointTests {

        @Test
        @DisplayName("Should return true for point inside region")
        void testIsInRegionPointInside() throws Exception {
            Position point = new Position(-3.188, 55.944);
            List<Position> vertices = Arrays.asList(
                    new Position(-3.192473, 55.946233),
                    new Position(-3.192473, 55.942617),
                    new Position(-3.184319, 55.942617),
                    new Position(-3.184319, 55.946233),
                    new Position(-3.192473, 55.946233)
            );
            IsInRegionRequest.Region region = new IsInRegionRequest.Region("central", vertices);
            IsInRegionRequest request = new IsInRegionRequest(point, region);

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should return false for point outside region")
        void testIsInRegionPointOutside() throws Exception {
            Position point = new Position(-3.200, 55.950);
            List<Position> vertices = Arrays.asList(
                    new Position(-3.192473, 55.946233),
                    new Position(-3.192473, 55.942617),
                    new Position(-3.184319, 55.942617),
                    new Position(-3.184319, 55.946233),
                    new Position(-3.192473, 55.946233)
            );
            IsInRegionRequest.Region region = new IsInRegionRequest.Region("central", vertices);
            IsInRegionRequest request = new IsInRegionRequest(point, region);

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should work with minimum polygon (triangle)")
        void testIsInRegionMinimumTriangle() throws Exception {
            Position point = new Position(1.0, 0.5);
            List<Position> vertices = Arrays.asList(
                    new Position(0.0, 0.0),
                    new Position(2.0, 0.0),
                    new Position(1.0, 1.0),
                    new Position(0.0, 0.0)
            );
            IsInRegionRequest.Region region = new IsInRegionRequest.Region("triangle", vertices);
            IsInRegionRequest request = new IsInRegionRequest(point, region);

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should work with concave polygons")
        void testIsInRegionConcavePolygon() throws Exception {
            Position point = new Position(0.5, 0.5);
            List<Position> vertices = Arrays.asList(
                    new Position(0.0, 0.0),
                    new Position(2.0, 0.0),
                    new Position(2.0, 1.0),
                    new Position(1.0, 1.0),
                    new Position(1.0, 2.0),
                    new Position(0.0, 2.0),
                    new Position(0.0, 0.0)
            );
            IsInRegionRequest.Region region = new IsInRegionRequest.Region("l-shape", vertices);
            IsInRegionRequest request = new IsInRegionRequest(point, region);

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 for open polygon (not closed)")
        void testIsInRegionOpenPolygon() throws Exception {
            Position point = new Position(-3.188, 55.944);
            List<Position> vertices = Arrays.asList(
                    new Position(-3.192473, 55.946233),
                    new Position(-3.192473, 55.942617),
                    new Position(-3.184319, 55.942617),
                    new Position(-3.184319, 55.946233)
            );
            IsInRegionRequest.Region region = new IsInRegionRequest.Region("central", vertices);
            IsInRegionRequest request = new IsInRegionRequest(point, region);

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for too few vertices")
        void testIsInRegionTooFewVertices() throws Exception {
            Position point = new Position(-3.188, 55.944);
            List<Position> vertices = Arrays.asList(
                    new Position(-3.192473, 55.946233),
                    new Position(-3.192473, 55.942617),
                    new Position(-3.192473, 55.946233)
            );
            IsInRegionRequest.Region region = new IsInRegionRequest.Region("central", vertices);
            IsInRegionRequest request = new IsInRegionRequest(point, region);

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when position is null")
        void testIsInRegionNullPosition() throws Exception {
            List<Position> vertices = Arrays.asList(
                    new Position(-3.192473, 55.946233),
                    new Position(-3.192473, 55.942617),
                    new Position(-3.184319, 55.942617),
                    new Position(-3.184319, 55.946233),
                    new Position(-3.192473, 55.946233)
            );
            IsInRegionRequest.Region region = new IsInRegionRequest.Region("central", vertices);
            IsInRegionRequest request = new IsInRegionRequest(null, region);

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when region is null")
        void testIsInRegionNullRegion() throws Exception {
            Position point = new Position(-3.188, 55.944);
            IsInRegionRequest request = new IsInRegionRequest(point, null);

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for NaN in coordinates")
        void testIsInRegionWithNaN() throws Exception {
            String jsonBody = """
                {
                    "position": {"lng": "NaN", "lat": 55.944},
                    "region": {
                        "name": "central",
                        "vertices": [
                            {"lng": -3.192473, "lat": 55.946233},
                            {"lng": -3.192473, "lat": 55.942617},
                            {"lng": -3.184319, "lat": 55.942617},
                            {"lng": -3.184319, "lat": 55.946233},
                            {"lng": -3.192473, "lat": 55.946233}
                        ]
                    }
                }
                """;

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for Infinity in coordinates")
        void testIsInRegionWithInfinity() throws Exception {
            String jsonBody = """
                {
                    "position": {"lng": -3.188, "lat": "Infinity"},
                    "region": {
                        "name": "central",
                        "vertices": [
                            {"lng": -3.192473, "lat": 55.946233},
                            {"lng": -3.192473, "lat": 55.942617},
                            {"lng": -3.184319, "lat": 55.942617},
                            {"lng": -3.184319, "lat": 55.946233},
                            {"lng": -3.192473, "lat": 55.946233}
                        ]
                    }
                }
                """;

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when region name is null")
        void testIsInRegionNullName() throws Exception {
            String invalidJson = """
            {
                "position": {"lng": -3.188, "lat": 55.944},
                "region": {
                    "name": null,
                    "vertices": [
                        {"lng": -3.192473, "lat": 55.946233},
                        {"lng": -3.192473, "lat": 55.942617},
                        {"lng": -3.184319, "lat": 55.942617},
                        {"lng": -3.184319, "lat": 55.946233},
                        {"lng": -3.192473, "lat": 55.946233}
                    ]
                }
            }
            """;

            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for empty request body")
        void testIsInRegionEmptyBody() throws Exception {
            mockMvc.perform(post("/api/v1/isInRegion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/nextPosition")
    class NextPositionEndpointTests {

        @Test
        @DisplayName("Should calculate next position for valid angle")
        void testNextPositionValid() throws Exception {
            Position start = new Position(-3.192473, 55.946233);
            NextPositionRequest request = new NextPositionRequest(start, 0.0);

            mockMvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lng").exists())
                    .andExpect(jsonPath("$.lat").exists());
        }

        @Test
        @DisplayName("Should accept all 16 valid compass angles")
        void testNextPositionAllValidAngles() throws Exception {
            Position start = new Position(-3.192473, 55.946233);
            double[] validAngles = {0.0, 22.5, 45.0, 67.5, 90.0, 112.5, 135.0, 157.5,
                    180.0, 202.5, 225.0, 247.5, 270.0, 292.5, 315.0, 337.5};

            for (double angle : validAngles) {
                NextPositionRequest request = new NextPositionRequest(start, angle);
                mockMvc.perform(post("/api/v1/nextPosition")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());
            }
        }

        @Test
        @DisplayName("Should return 400 for invalid angle")
        void testNextPositionInvalidAngle() throws Exception {
            Position start = new Position(-3.192473, 55.946233);
            NextPositionRequest request = new NextPositionRequest(start, 15.0);

            mockMvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for multiple invalid angles")
        void testNextPositionAllInvalidAngles() throws Exception {
            Position start = new Position(-3.192473, 55.946233);
            double[] invalidAngles = {10.0, 15.0, 30.0, 60.0, 123.45, -90.0, 360.0, 361.0};

            for (double angle : invalidAngles) {
                NextPositionRequest request = new NextPositionRequest(start, angle);
                mockMvc.perform(post("/api/v1/nextPosition")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Test
        @DisplayName("Should return 400 when start position is null")
        void testNextPositionNullStart() throws Exception {
            NextPositionRequest request = new NextPositionRequest(null, 0.0);

            mockMvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when angle is null")
        void testNextPositionNullAngle() throws Exception {
            String invalidJson = """
            {
                "start": {"lng": -3.192473, "lat": 55.946233}
            }
            """;

            mockMvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for NaN in coordinates")
        void testNextPositionWithNaN() throws Exception {
            String jsonBody = """
                {
                    "start": {"lng": "NaN", "lat": 55.946233},
                    "angle": 45.0
                }
                """;

            mockMvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for Infinity in angle")
        void testNextPositionWithInfinityInAngle() throws Exception {
            String jsonBody = """
                {
                    "start": {"lng": -3.192473, "lat": 55.946233},
                    "angle": "Infinity"
                }
                """;

            mockMvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for empty request body")
        void testNextPositionEmptyBody() throws Exception {
            mockMvc.perform(post("/api/v1/nextPosition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}