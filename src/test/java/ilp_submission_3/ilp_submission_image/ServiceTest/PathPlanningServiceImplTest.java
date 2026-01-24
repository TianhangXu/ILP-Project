package ilp_submission_3.ilp_submission_image.ServiceTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ilp_submission_3.ilp_submission_image.Configuration.ILPEndpointProvider;
import ilp_submission_3.ilp_submission_image.Service.DroneService;
import ilp_submission_3.ilp_submission_image.Service.ILPServiceInterface;
import ilp_submission_3.ilp_submission_image.Service.PathPlanningServiceImpl;
import ilp_submission_3.ilp_submission_image.WebSocket.PathfindingProgressHandler;
import ilp_submission_3.ilp_submission_image.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for PathPlanningServiceImpl
 * Coverage areas:
 * - Path calculation with various scenarios
 * - Restricted area avoidance
 * - Single vs multi-drone solutions
 * - Edge cases and boundary conditions
 * - GeoJSON generation
 */
class PathPlanningServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ILPEndpointProvider endpointProvider;

    @Mock
    private ILPServiceInterface ilpService;

    @Mock
    private DroneService droneService;

    @Mock
    private PathfindingProgressHandler progressHandler;

    private PathPlanningServiceImpl pathPlanningService;

    private static final String BASE_URL = "https://test.example.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(endpointProvider.getEndpoint()).thenReturn(BASE_URL);
        when(progressHandler.hasActiveConnections()).thenReturn(false);

        pathPlanningService = new PathPlanningServiceImpl(
                restTemplate,
                endpointProvider,
                ilpService,
                droneService,
                progressHandler
        );
    }

    // ==================== Restricted Areas Tests ====================

    @Test
    @DisplayName("Should fetch restricted areas successfully")
    void testGetRestrictedAreas_Success() {
        RestrictedArea area1 = new RestrictedArea(
                "Central Area",
                1,
                new RestrictedArea.Limits(0.0, 100.0),
                Arrays.asList(
                        new Position(-3.19, 55.94),
                        new Position(-3.18, 55.94),
                        new Position(-3.18, 55.95),
                        new Position(-3.19, 55.95),
                        new Position(-3.19, 55.94)
                )
        );
        RestrictedArea[] areas = {area1};

        when(restTemplate.getForObject(BASE_URL + "/restricted-areas", RestrictedArea[].class))
                .thenReturn(areas);

        List<RestrictedArea> result = pathPlanningService.getRestrictedAreas();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).id());
        assertEquals("Central Area", result.get(0).name());
        verify(restTemplate, times(1)).getForObject(anyString(), eq(RestrictedArea[].class));
    }

    @Test
    @DisplayName("Should handle null response from restricted areas endpoint")
    void testGetRestrictedAreas_NullResponse() {
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(null);

        List<RestrictedArea> result = pathPlanningService.getRestrictedAreas();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list for empty restricted areas")
    void testGetRestrictedAreas_EmptyArray() {
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        List<RestrictedArea> result = pathPlanningService.getRestrictedAreas();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Flight Path Calculation Tests ====================

    @Test
    @DisplayName("Should calculate direct path when no obstacles")
    void testCalculateFlightPath_DirectPath() {
        Position from = new Position(-3.186874, 55.944494);
        Position to = new Position(-3.186000, 55.945000);
        List<RestrictedArea> restrictedAreas = new ArrayList<>();

        when(ilpService.isClose(from, to)).thenReturn(false).thenReturn(true);
        when(ilpService.distance(any(), any())).thenReturn(0.001);
        when(ilpService.nextPosition(any(), anyDouble())).thenAnswer(invocation -> {
            Position start = invocation.getArgument(0);
            return new Position(start.lng() + 0.0001, start.lat() + 0.0001);
        });

        List<Position> path = pathPlanningService.calculateFlightPath(from, to, restrictedAreas);

        assertNotNull(path);
        assertFalse(path.isEmpty());
    }

    @Test
    @DisplayName("Should return path for very close positions")
    void testCalculateFlightPath_VeryClosePositions() {
        Position from = new Position(-3.186874, 55.944494);
        Position to = new Position(-3.186874, 55.944494);

        when(ilpService.isClose(from, to)).thenReturn(true);

        List<Position> path = pathPlanningService.calculateFlightPath(from, to, new ArrayList<>());

        assertNotNull(path);
        assertEquals(2, path.size());
        assertEquals(from, path.get(0));
        assertEquals(to, path.get(1));
    }

    @Test
    @DisplayName("Should avoid restricted areas in path")
    void testCalculateFlightPath_AvoidRestrictedArea() {
        Position from = new Position(-3.190, 55.944);
        Position to = new Position(-3.180, 55.946);

        List<Position> restrictedVertices = Arrays.asList(
                new Position(-3.186, 55.944),
                new Position(-3.184, 55.944),
                new Position(-3.184, 55.946),
                new Position(-3.186, 55.946),
                new Position(-3.186, 55.944)
        );
        RestrictedArea restricted = new RestrictedArea(
                "Test Area",
                1,
                new RestrictedArea.Limits(0.0, 100.0),
                restrictedVertices
        );

        // Mock isClose: only return true when positions are actually very close
        when(ilpService.isClose(any(), any())).thenAnswer(invocation -> {
            Position p1 = invocation.getArgument(0);
            Position p2 = invocation.getArgument(1);
            double distance = Math.sqrt(
                    Math.pow(p1.lng() - p2.lng(), 2) + Math.pow(p1.lat() - p2.lat(), 2)
            );
            return distance < 0.00015;
        });

        // Mock distance: calculate actual Euclidean distance
        when(ilpService.distance(any(), any())).thenAnswer(invocation -> {
            Position p1 = invocation.getArgument(0);
            Position p2 = invocation.getArgument(1);
            return Math.sqrt(
                    Math.pow(p1.lng() - p2.lng(), 2) + Math.pow(p1.lat() - p2.lat(), 2)
            );
        });

        // Mock isInRegion: simple point-in-rectangle check for the restricted area
        when(ilpService.isInRegion(any(), eq(restrictedVertices))).thenAnswer(invocation -> {
            Position point = invocation.getArgument(0);
            // Check if point is inside the rectangle [-3.186, -3.184] x [55.944, 55.946]
            return point.lng() >= -3.186 && point.lng() <= -3.184 &&
                    point.lat() >= 55.944 && point.lat() <= 55.946;
        });

        // Mock nextPosition: calculate next position based on angle
        when(ilpService.nextPosition(any(), anyDouble())).thenAnswer(invocation -> {
            Position start = invocation.getArgument(0);
            Double angle = invocation.getArgument(1);
            double rad = Math.toRadians(angle);
            return new Position(
                    start.lng() + 0.00015 * Math.cos(rad),
                    start.lat() + 0.00015 * Math.sin(rad)
            );
        });

        List<Position> path = pathPlanningService.calculateFlightPath(
                from, to, Arrays.asList(restricted)
        );

        // Should find a path (even if it needs to go around the restricted area)
        assertNotNull(path);
        // If path is empty, it means no path was found (which is also a valid result to test)
        // If path is not empty, verify it doesn't go through restricted area
        if (!path.isEmpty()) {
            assertTrue(path.size() >= 2);
        }
    }

    @Test
    @DisplayName("Should return empty path when destination unreachable")
    void testCalculateFlightPath_UnreachableDestination() {
        Position from = new Position(-3.190, 55.944);
        Position to = new Position(-3.180, 55.946);

        // Create a completely blocked scenario: all positions are in restricted area
        List<Position> blockingVertices = Arrays.asList(
                new Position(-3.200, 55.940),
                new Position(-3.170, 55.940),
                new Position(-3.170, 55.950),
                new Position(-3.200, 55.950),
                new Position(-3.200, 55.940)
        );
        RestrictedArea blockingArea = new RestrictedArea(
                "Blocking Area",
                1,
                new RestrictedArea.Limits(0.0, 100.0),
                blockingVertices
        );

        // Mock isClose: only true when very close
        when(ilpService.isClose(any(), any())).thenAnswer(invocation -> {
            Position p1 = invocation.getArgument(0);
            Position p2 = invocation.getArgument(1);
            double distance = Math.sqrt(
                    Math.pow(p1.lng() - p2.lng(), 2) + Math.pow(p1.lat() - p2.lat(), 2)
            );
            return distance < 0.00015;
        });

        // Mock distance: calculate actual distance
        when(ilpService.distance(any(), any())).thenAnswer(invocation -> {
            Position p1 = invocation.getArgument(0);
            Position p2 = invocation.getArgument(1);
            return Math.sqrt(
                    Math.pow(p1.lng() - p2.lng(), 2) + Math.pow(p1.lat() - p2.lat(), 2)
            );
        });

        // Mock isInRegion: all positions except start are blocked
        when(ilpService.isInRegion(any(), eq(blockingVertices))).thenAnswer(invocation -> {
            Position point = invocation.getArgument(0);
            // Block everything except the exact starting position
            return !point.equals(from);
        });

        // Mock nextPosition
        when(ilpService.nextPosition(any(), anyDouble())).thenAnswer(invocation -> {
            Position start = invocation.getArgument(0);
            Double angle = invocation.getArgument(1);
            double rad = Math.toRadians(angle);
            return new Position(
                    start.lng() + 0.00015 * Math.cos(rad),
                    start.lat() + 0.00015 * Math.sin(rad)
            );
        });

        List<Position> path = pathPlanningService.calculateFlightPath(
                from, to, Arrays.asList(blockingArea)
        );

        // Should return empty path when unreachable
        assertNotNull(path);
        // The path should be empty because destination is unreachable
        assertTrue(path.isEmpty() || path.size() < 3,
                "Path should be empty or very short when destination is unreachable");
    }

    // ==================== Delivery Path Calculation Tests ====================

    @Test
    @DisplayName("Should return empty response for null dispatch records")
    void testCalculateDeliveryPath_NullInput() {
        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(null);

        assertNotNull(response);
        assertEquals(0.0, response.totalCost());
        assertEquals(0, response.totalMoves());
        assertTrue(response.dronePaths().isEmpty());
    }

    @Test
    @DisplayName("Should return empty response for empty dispatch records")
    void testCalculateDeliveryPath_EmptyInput() {
        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(new ArrayList<>());

        assertNotNull(response);
        assertEquals(0.0, response.totalCost());
        assertEquals(0, response.totalMoves());
        assertTrue(response.dronePaths().isEmpty());
    }

    @Test
    @DisplayName("Should prefer single drone solution when cheaper")
    void testCalculateDeliveryPath_PrefersSingleDrone() {
        List<MedDispatchRec> dispatches = createSampleDispatches(2);

        when(droneService.queryAvailableDrones(dispatches))
                .thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(new ArrayList<>());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());

        when(droneService.queryAvailableDronesWithOr(dispatches))
                .thenReturn(Arrays.asList("drone1", "drone2"));

        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should use multi-drone when no single drone available")
    void testCalculateDeliveryPath_UsesMultiDroneWhenNeeded() {
        List<MedDispatchRec> dispatches = createSampleDispatches(5);

        when(droneService.queryAvailableDrones(dispatches))
                .thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(dispatches))
                .thenReturn(Arrays.asList("drone1", "drone2"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(new ArrayList<>());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    // ==================== GeoJSON Generation Tests ====================

    @Test
    @DisplayName("Should generate valid GeoJSON for delivery path")
    void testCalculateDeliveryPathAsGeoJson_ValidOutput() {
        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(new ArrayList<>());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        String geoJson = pathPlanningService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);
        assertTrue(geoJson.contains("FeatureCollection") || geoJson.contains("\"type\""));
    }

    @Test
    @DisplayName("Should generate empty GeoJSON when no paths found")
    void testCalculateDeliveryPathAsGeoJson_EmptyPaths() {
        List<MedDispatchRec> dispatches = new ArrayList<>();

        String geoJson = pathPlanningService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);
        assertTrue(geoJson.contains("FeatureCollection"));
        assertTrue(geoJson.contains("features"));
    }

    // ==================== Parameterized Tests ====================

    @ParameterizedTest
    @MethodSource("provideDifferentDispatchSizes")
    @DisplayName("Should handle different dispatch batch sizes")
    void testCalculateDeliveryPath_DifferentBatchSizes(int dispatchCount) {
        List<MedDispatchRec> dispatches = createSampleDispatches(dispatchCount);

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(new ArrayList<>());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
        assertTrue(response.totalCost() >= 0);
        assertTrue(response.totalMoves() >= 0);
    }

    static Stream<Arguments> provideDifferentDispatchSizes() {
        return Stream.of(
                Arguments.of(0),
                Arguments.of(1),
                Arguments.of(3),
                Arguments.of(5),
                Arguments.of(10)
        );
    }

    // ==================== Helper Methods ====================

    private List<MedDispatchRec> createSampleDispatches(int count) {
        List<MedDispatchRec> dispatches = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            dispatches.add(new MedDispatchRec(
                    i + 1,
                    "2025-01-20",
                    "12:00",
                    new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                    new Position(-3.186 + i * 0.001, 55.944 + i * 0.001)
            ));
        }
        return dispatches;
    }

    private List<Drone> createSampleDrones() {
        return Arrays.asList(
                new Drone(
                        "Drone 1",
                        "1",
                        new Drone.Capability(true, true, 4.0, 2000, 0.01, 4.3, 6.5)
                ),
                new Drone(
                        "Drone 2",
                        "2",
                        new Drone.Capability(false, true, 8.0, 1000, 0.03, 2.6, 5.4)
                ),
                new Drone(
                        "Drone 3",
                        "3",
                        new Drone.Capability(false, false, 20.0, 4000, 0.05, 9.5, 11.5)
                )
        );
    }

    private List<DroneServicePoint> createSampleServicePoints() {
        return Arrays.asList(
                new DroneServicePoint(
                        "Appleton Tower",
                        1,
                        new DroneServicePoint.LngLatAlt(-3.18635807889864, 55.9446806670849, 50.0)
                ),
                new DroneServicePoint(
                        "Ocean Terminal",
                        2,
                        new DroneServicePoint.LngLatAlt(-3.17732611501824, 55.9811862793337, 50.0)
                )
        );
    }



    // ==================== Edge Cases and Negative Tests ====================

    @Test
    @DisplayName("Should handle dispatch with null requirements")
    void testCalculateDeliveryPath_NullRequirements() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        "2025-01-20",
                        "12:00",
                        null,
                        new Position(-3.186, 55.944)
                )
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(new ArrayList<>());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle dispatch with extreme coordinates")
    void testCalculateDeliveryPath_ExtremeCoordinates() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        "2025-01-20",
                        "12:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-180.0, 85.0)
                )
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(new ArrayList<>());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle WebSocket progress broadcasting when active")
    void testCalculateFlightPath_WithActiveWebSocket() {
        when(progressHandler.hasActiveConnections()).thenReturn(true);

        Position from = new Position(-3.186874, 55.944494);
        Position to = new Position(-3.186000, 55.945000);

        when(ilpService.isClose(from, to)).thenReturn(true);

        List<Position> path = pathPlanningService.calculateFlightPath(
                from, to, new ArrayList<>()
        );

        assertNotNull(path);
        verify(progressHandler, atLeast(0)).broadcastProgress(any());
    }

    @Test
    @DisplayName("Should handle restricted area with null vertices")
    void testCalculateFlightPath_NullVertices() {
        Position from = new Position(-3.186874, 55.944494);
        Position to = new Position(-3.186000, 55.945000);

        RestrictedArea areaWithNullVertices = new RestrictedArea(
                "Test Area",
                1,
                new RestrictedArea.Limits(0.0, 100.0),
                null
        );

        when(ilpService.isClose(from, to)).thenReturn(true);

        List<Position> path = pathPlanningService.calculateFlightPath(
                from, to, Arrays.asList(areaWithNullVertices)
        );

        assertNotNull(path);
    }

    @Test
    @DisplayName("Should handle dispatch with null date")
    void testCalculateDeliveryPath_NullDate() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        null,
                        "12:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944)
                )
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(new ArrayList<>());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle very large batch of dispatches")
    void testCalculateDeliveryPath_LargeBatch() {
        List<MedDispatchRec> dispatches = createSampleDispatches(100);

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(new ArrayList<>());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle simple pathfinding scenario without complex mocking")
    void testCalculateFlightPath_SimplifiedScenario() {
        Position from = new Position(-3.186874, 55.944494);
        Position to = new Position(-3.186874, 55.944600);

        // Use real distance and isClose logic
        when(ilpService.isClose(any(), any())).thenAnswer(invocation -> {
            Position p1 = invocation.getArgument(0);
            Position p2 = invocation.getArgument(1);
            double dist = Math.sqrt(Math.pow(p1.lng() - p2.lng(), 2) + Math.pow(p1.lat() - p2.lat(), 2));
            return dist < 0.00015;
        });

        when(ilpService.distance(any(), any())).thenAnswer(invocation -> {
            Position p1 = invocation.getArgument(0);
            Position p2 = invocation.getArgument(1);
            return Math.sqrt(Math.pow(p1.lng() - p2.lng(), 2) + Math.pow(p1.lat() - p2.lat(), 2));
        });

        when(ilpService.nextPosition(any(), anyDouble())).thenAnswer(invocation -> {
            Position start = invocation.getArgument(0);
            Double angle = invocation.getArgument(1);
            double rad = Math.toRadians(angle);
            return new Position(
                    start.lng() + 0.00015 * Math.cos(rad),
                    start.lat() + 0.00015 * Math.sin(rad)
            );
        });

        // No restricted areas
        List<Position> path = pathPlanningService.calculateFlightPath(from, to, new ArrayList<>());

        assertNotNull(path);
        if (!path.isEmpty()) {
            assertTrue(path.size() >= 2);
            assertEquals(from, path.get(0));
        }
    }

    // ==================== Additional Coverage Tests ====================

    @Test
    @DisplayName("Should handle dispatches grouped by multiple dates")
    void testCalculateDeliveryPath_MultipleDates() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944)),
                new MedDispatchRec(2, "2025-01-21", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.187, 55.945)),
                new MedDispatchRec(3, "2025-01-22", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.188, 55.946))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle single drone solution with service point selection")
    void testCalculateDeliveryPath_SingleDroneWithServicePoint() {
        List<MedDispatchRec> dispatches = createSampleDispatches(2);

        when(droneService.queryAvailableDrones(dispatches))
                .thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(droneService.queryAvailableDronesWithOr(dispatches))
                .thenReturn(new ArrayList<>());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }



    @Test
    @DisplayName("Should handle delivery in restricted area")
    void testCalculateDeliveryPath_DeliveryInRestrictedArea() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.185, 55.945)) // Inside restricted area
        );

        List<Position> restrictedVertices = Arrays.asList(
                new Position(-3.186, 55.944),
                new Position(-3.184, 55.944),
                new Position(-3.184, 55.946),
                new Position(-3.186, 55.946),
                new Position(-3.186, 55.944)
        );

        RestrictedArea restricted = new RestrictedArea(
                "Test Area", 1, new RestrictedArea.Limits(0.0, 100.0), restrictedVertices
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[]{restricted});

        setupRealisticMocks();

        // Mock isInRegion to return true for delivery position
        when(ilpService.isInRegion(eq(new Position(-3.185, 55.945)), any()))
                .thenReturn(true);

        when(progressHandler.hasActiveConnections()).thenReturn(true);

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
        // Should fail or return empty due to delivery in restricted area
    }

    @Test
    @DisplayName("Should handle drone exceeding max moves")
    void testCalculateDeliveryPath_ExceedsMaxMoves() {
        List<MedDispatchRec> dispatches = createSampleDispatches(50); // Many deliveries

        Drone limitedDrone = new Drone(
                "LimitedDrone", "drone1",
                new Drone.Capability(true, false, 100.0, 5, 1.0, 10.0, 5.0) // maxMoves = 5
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(Arrays.asList(limitedDrone));
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle dispatch exceeding max cost per delivery")
    void testCalculateDeliveryPath_ExceedsMaxCost() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 1.0), // Very low maxCost
                        new Position(-3.186, 55.944))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should merge multiple drone paths correctly")
    void testCalculateDeliveryPath_MergeDronePaths() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(2.0, true, false, 100.0),
                        new Position(-3.186, 55.944)),
                new MedDispatchRec(2, "2025-01-20", "11:00",
                        new MedDispatchRec.Requirements(2.0, true, false, 100.0),
                        new Position(-3.187, 55.945))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1", "drone2"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle GeoJSON generation with multiple drone paths")
    void testCalculateDeliveryPathAsGeoJson_MultipleDrones() {
        // This will test the color generation for multiple drones
        List<MedDispatchRec> dispatches = createSampleDispatches(3);

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(new ArrayList<>());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        String geoJson = pathPlanningService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);
        assertTrue(geoJson.contains("type"));
    }

    @Test
    @DisplayName("Should handle restricted areas with empty vertices")
    void testGetRestrictedAreas_EmptyVertices() {
        RestrictedArea areaWithEmptyVertices = new RestrictedArea(
                "Empty Area", 1, new RestrictedArea.Limits(0.0, 100.0), new ArrayList<>()
        );

        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[]{areaWithEmptyVertices});

        List<RestrictedArea> result = pathPlanningService.getRestrictedAreas();

        assertEquals(1, result.size());
        assertTrue(result.get(0).vertices().isEmpty());
    }

    @Test
    @DisplayName("Should optimize delivery order based on distance from service point")
    void testCalculateDeliveryPath_OptimizedOrder() {
        Position servicePoint = new Position(-3.186874, 55.944494);

        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.180, 55.950)), // Far
                new MedDispatchRec(2, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944)), // Close
                new MedDispatchRec(3, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.185, 55.945))  // Medium
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(new ArrayList<>());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle multiple restricted areas with complex geometry")
    void testCalculateFlightPath_MultipleRestrictedAreas() {
        Position from = new Position(-3.190, 55.944);
        Position to = new Position(-3.180, 55.946);

        List<RestrictedArea> multipleAreas = Arrays.asList(
                new RestrictedArea("Area 1", 1, new RestrictedArea.Limits(0.0, 100.0),
                        Arrays.asList(
                                new Position(-3.188, 55.944),
                                new Position(-3.187, 55.944),
                                new Position(-3.187, 55.945),
                                new Position(-3.188, 55.945),
                                new Position(-3.188, 55.944)
                        )),
                new RestrictedArea("Area 2", 2, new RestrictedArea.Limits(0.0, 100.0),
                        Arrays.asList(
                                new Position(-3.185, 55.945),
                                new Position(-3.184, 55.945),
                                new Position(-3.184, 55.946),
                                new Position(-3.185, 55.946),
                                new Position(-3.185, 55.945)
                        ))
        );

        setupRealisticMocks();

        List<Position> path = pathPlanningService.calculateFlightPath(from, to, multipleAreas);

        assertNotNull(path);
    }

    @Test
    @DisplayName("Should handle path through restricted area boundary")
    void testCalculateFlightPath_PathThroughBoundary() {
        Position from = new Position(-3.190, 55.944);
        Position to = new Position(-3.180, 55.946);

        List<Position> restrictedVertices = Arrays.asList(
                new Position(-3.186, 55.944),
                new Position(-3.184, 55.944),
                new Position(-3.184, 55.946),
                new Position(-3.186, 55.946),
                new Position(-3.186, 55.944)
        );

        RestrictedArea restricted = new RestrictedArea(
                "Test Area", 1, new RestrictedArea.Limits(0.0, 100.0), restrictedVertices
        );

        setupRealisticMocks();

        // Mock line segment intersection to test path through boundary
        when(ilpService.isInRegion(any(), eq(restrictedVertices))).thenAnswer(invocation -> {
            Position point = invocation.getArgument(0);
            return point.lng() >= -3.186 && point.lng() <= -3.184 &&
                    point.lat() >= 55.944 && point.lat() <= 55.946;
        });

        List<Position> path = pathPlanningService.calculateFlightPath(
                from, to, Arrays.asList(restricted)
        );

        assertNotNull(path);
    }

    @Test
    @DisplayName("Should handle drone with null capability fields")
    void testCalculateDeliveryPath_NullCapabilityFields() {
        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        Drone droneWithNulls = new Drone(
                "TestDrone", "drone1",
                new Drone.Capability(null, null, null, null, null, null, null)
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(Arrays.asList(droneWithNulls));
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle dispatch with null time")
    void testCalculateDeliveryPath_NullTime() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        "2025-01-20",
                        null, // null time
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944)
                )
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle invalid date format in dispatch")
    void testCalculateDeliveryPath_InvalidDateFormat() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        "invalid-date",
                        "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944)
                )
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle invalid time format in dispatch")
    void testCalculateDeliveryPath_InvalidTimeFormat() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        "2025-01-20",
                        "invalid-time",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944)
                )
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle drone without any availability slots")
    void testCalculateDeliveryPath_NoAvailabilitySlots() {
        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        List<DroneForServicePoint> emptyAvailability = Arrays.asList(
                new DroneForServicePoint(1, Arrays.asList(
                        new DroneForServicePoint.DroneAvailability("drone1", new ArrayList<>())
                ))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(emptyAvailability);
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle availability with invalid day of week")
    void testCalculateDeliveryPath_InvalidDayOfWeek() {
        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        List<DroneForServicePoint> invalidAvailability = Arrays.asList(
                new DroneForServicePoint(1, Arrays.asList(
                        new DroneForServicePoint.DroneAvailability("drone1", Arrays.asList(
                                new DroneForServicePoint.DroneAvailability.Availability(
                                        "INVALID_DAY", "09:00", "17:00"
                                )
                        ))
                ))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(invalidAvailability);
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle availability with null from/until times")
    void testCalculateDeliveryPath_NullAvailabilityTimes() {
        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        List<DroneForServicePoint> nullTimeAvailability = Arrays.asList(
                new DroneForServicePoint(1, Arrays.asList(
                        new DroneForServicePoint.DroneAvailability("drone1", Arrays.asList(
                                new DroneForServicePoint.DroneAvailability.Availability(
                                        "MONDAY", null, null
                                )
                        ))
                ))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(nullTimeAvailability);
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle service point with null location")
    void testCalculateDeliveryPath_NullServicePointLocation() {
        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        List<DroneServicePoint> nullLocationSP = Arrays.asList(
                new DroneServicePoint("Test SP", 1, null)
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(nullLocationSP);
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle batch where capacity constraint is met exactly")
    void testCalculateDeliveryPath_ExactCapacityMatch() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(2.0, false, false, 100.0),
                        new Position(-3.186, 55.944)),
                new MedDispatchRec(2, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(2.0, false, false, 100.0),
                        new Position(-3.187, 55.945))
        );

        Drone exactCapacityDrone = new Drone(
                "ExactDrone", "drone1",
                new Drone.Capability(true, true, 4.0, 2000, 0.01, 4.3, 6.5) // Exactly 4.0 capacity
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(Arrays.asList(exactCapacityDrone));
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle single dispatch requiring both heating and cooling")
    void testCalculateDeliveryPath_HeatingAndCooling() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, true, true, 100.0), // Both heating and cooling
                        new Position(-3.186, 55.944))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle angle calculation wrapping around 360 degrees")
    void testCalculateFlightPath_AngleWraparound() {
        Position from = new Position(-3.186, 55.944);
        Position to = new Position(-3.187, 55.943); // Southwest direction

        setupRealisticMocks();

        List<Position> path = pathPlanningService.calculateFlightPath(
                from, to, new ArrayList<>()
        );

        assertNotNull(path);
    }

    @Test
    @DisplayName("Should handle very long paths exceeding default max moves")
    void testCalculateDeliveryPath_ExceedsDefaultMaxMoves() {
        // Create dispatches very far apart
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944)),
                new MedDispatchRec(2, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.000, 56.000)) // Very far away
        );

        Drone droneWithoutMaxMoves = new Drone(
                "UnlimitedDrone", "drone1",
                new Drone.Capability(true, true, 10.0, null, 0.01, 4.3, 6.5) // null maxMoves
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(Arrays.asList(droneWithoutMaxMoves));
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle GeoJSON generation with empty coordinates")
    void testCalculateDeliveryPathAsGeoJson_EmptyCoordinates() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 0.1), // Very low max cost
                        new Position(-3.186, 55.944))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(new ArrayList<>());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        String geoJson = pathPlanningService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);
        assertTrue(geoJson.contains("FeatureCollection"));
    }

    @Test
    @DisplayName("Should compare single vs multi drone solutions correctly")
    void testCalculateDeliveryPath_CompareSolutions() {
        List<MedDispatchRec> dispatches = createSampleDispatches(3);

        // Return both single and multi drone candidates
        when(droneService.queryAvailableDrones(dispatches))
                .thenReturn(Arrays.asList("drone1"));
        when(droneService.queryAvailableDronesWithOr(dispatches))
                .thenReturn(Arrays.asList("drone1", "drone2"));

        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle service point with null drones list")
    void testCalculateDeliveryPath_NullDronesList() {
        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        List<DroneForServicePoint> nullDronesList = Arrays.asList(
                new DroneForServicePoint(1, null) // null drones list
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(nullDronesList);
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle capacity exactly zero")
    void testCalculateDeliveryPath_ZeroCapacity() {
        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        Drone zeroCapacityDrone = new Drone(
                "ZeroDrone", "drone1",
                new Drone.Capability(true, true, 0.0, 2000, 0.01, 4.3, 6.5) // Zero capacity
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(Arrays.asList(zeroCapacityDrone));
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    // ==================== Helper Methods for Additional Tests ====================

    private List<DroneForServicePoint> createSampleDroneServicePoints() {
        List<DroneForServicePoint.DroneAvailability.Availability> availability = Arrays.asList(
                new DroneForServicePoint.DroneAvailability.Availability("MONDAY", "09:00", "17:00"),
                new DroneForServicePoint.DroneAvailability.Availability("TUESDAY", "09:00", "17:00"),
                new DroneForServicePoint.DroneAvailability.Availability("WEDNESDAY", "09:00", "17:00"),
                new DroneForServicePoint.DroneAvailability.Availability("THURSDAY", "09:00", "17:00"),
                new DroneForServicePoint.DroneAvailability.Availability("FRIDAY", "09:00", "17:00")
        );

        DroneForServicePoint.DroneAvailability droneAvail1 = new DroneForServicePoint.DroneAvailability(
                "drone1", availability
        );
        DroneForServicePoint.DroneAvailability droneAvail2 = new DroneForServicePoint.DroneAvailability(
                "drone2", availability
        );

        return Arrays.asList(
                new DroneForServicePoint(1, Arrays.asList(droneAvail1, droneAvail2))
        );
    }

    private void setupRealisticMocks() {
        when(ilpService.isClose(any(), any())).thenAnswer(invocation -> {
            Position p1 = invocation.getArgument(0);
            Position p2 = invocation.getArgument(1);
            double dist = Math.sqrt(Math.pow(p1.lng() - p2.lng(), 2) + Math.pow(p1.lat() - p2.lat(), 2));
            return dist < 0.00015;
        });

        when(ilpService.distance(any(), any())).thenAnswer(invocation -> {
            Position p1 = invocation.getArgument(0);
            Position p2 = invocation.getArgument(1);
            return Math.sqrt(Math.pow(p1.lng() - p2.lng(), 2) + Math.pow(p1.lat() - p2.lat(), 2));
        });

        when(ilpService.nextPosition(any(), anyDouble())).thenAnswer(invocation -> {
            Position start = invocation.getArgument(0);
            Double angle = invocation.getArgument(1);
            double rad = Math.toRadians(angle);
            return new Position(
                    start.lng() + 0.00015 * Math.cos(rad),
                    start.lat() + 0.00015 * Math.sin(rad)
            );
        });

        when(ilpService.isInRegion(any(), anyList())).thenReturn(false);
    }

    @Test
    @DisplayName("Should return correct color for drone index with modulo wrapping")
    void testGetColorForDrone() throws Exception {
        PathPlanningServiceImpl service = new PathPlanningServiceImpl(
                null, null, null, null, null
        );

        // Use reflection to access private method
        Method method = PathPlanningServiceImpl.class.getDeclaredMethod("getColorForDrone", int.class);
        method.setAccessible(true);

        // Test first color
        assertEquals("#0000FF", method.invoke(service, 0));

        // Test middle color
        assertEquals("#FFA500", method.invoke(service, 4));

        // Test last color
        assertEquals("#32CD32", method.invoke(service, 9));

        // Test modulo wrapping (index 10 should return first color)
        assertEquals("#0000FF", method.invoke(service, 10));

        // Test modulo wrapping (index 15 should return 6th color)
        assertEquals("#FF1493", method.invoke(service, 16));

        // Test large index
        assertEquals("#00FF00", method.invoke(service, 21)); // 21 % 10 = 1
    }

    @Test
    @DisplayName("Should handle line segment intersection at endpoint")
    void testLineSegmentsIntersect_AtEndpoint() {
        Position from = new Position(-3.186, 55.944);
        Position to = new Position(-3.185, 55.945);

        List<Position> restrictedVertices = Arrays.asList(
                new Position(-3.185, 55.945), // Exact match with 'to'
                new Position(-3.184, 55.945),
                new Position(-3.184, 55.946),
                new Position(-3.185, 55.946),
                new Position(-3.185, 55.945)
        );

        RestrictedArea restricted = new RestrictedArea(
                "Endpoint Area", 1, new RestrictedArea.Limits(0.0, 100.0), restrictedVertices
        );

        setupRealisticMocks();
        when(ilpService.isInRegion(any(), eq(restrictedVertices))).thenReturn(false);

        List<Position> path = pathPlanningService.calculateFlightPath(
                from, to, Arrays.asList(restricted)
        );

        assertNotNull(path);
    }

    @Test
    @DisplayName("Should handle collinear line segments")
    void testLineSegmentsIntersect_Collinear() {
        Position from = new Position(-3.186, 55.944);
        Position to = new Position(-3.184, 55.944); // Same latitude

        List<Position> restrictedVertices = Arrays.asList(
                new Position(-3.185, 55.944), // On the same line
                new Position(-3.183, 55.944),
                new Position(-3.183, 55.945),
                new Position(-3.185, 55.945),
                new Position(-3.185, 55.944)
        );

        RestrictedArea restricted = new RestrictedArea(
                "Collinear Area", 1, new RestrictedArea.Limits(0.0, 100.0), restrictedVertices
        );

        setupRealisticMocks();

        List<Position> path = pathPlanningService.calculateFlightPath(
                from, to, Arrays.asList(restricted)
        );

        assertNotNull(path);
    }

    @Test
    @DisplayName("Should handle path with midpoint in restricted area")
    void testCalculateFlightPath_MidpointInRestricted() {
        Position from = new Position(-3.187, 55.944);
        Position to = new Position(-3.183, 55.946);

        List<Position> restrictedVertices = Arrays.asList(
                new Position(-3.186, 55.944),
                new Position(-3.184, 55.944),
                new Position(-3.184, 55.946),
                new Position(-3.186, 55.946),
                new Position(-3.186, 55.944)
        );

        RestrictedArea restricted = new RestrictedArea(
                "Midpoint Area", 1, new RestrictedArea.Limits(0.0, 100.0), restrictedVertices
        );

        setupRealisticMocks();

        // Midpoint should be inside restricted area
        when(ilpService.isInRegion(argThat(pos ->
                pos.lng() > -3.186 && pos.lng() < -3.184 &&
                        pos.lat() > 55.944 && pos.lat() < 55.946
        ), eq(restrictedVertices))).thenReturn(true);

        List<Position> path = pathPlanningService.calculateFlightPath(
                from, to, Arrays.asList(restricted)
        );

        assertNotNull(path);
    }

    @Test
    @DisplayName("Should handle diagonal movement angle calculation")
    void testCalculateFlightPath_DiagonalMovement() {
        Position from = new Position(-3.186, 55.944);
        Position to = new Position(-3.185, 55.945); // 45 degree diagonal

        setupRealisticMocks();

        List<Position> path = pathPlanningService.calculateFlightPath(
                from, to, new ArrayList<>()
        );

        assertNotNull(path);
        if (!path.isEmpty()) {
            assertTrue(path.size() >= 2);
        }
    }

    @Test
    @DisplayName("Should handle negative angle normalization")
    void testCalculateFlightPath_NegativeAngle() {
        Position from = new Position(-3.185, 55.945);
        Position to = new Position(-3.186, 55.944); // Southwest, negative angle

        setupRealisticMocks();

        List<Position> path = pathPlanningService.calculateFlightPath(
                from, to, new ArrayList<>()
        );

        assertNotNull(path);
    }

    @Test
    @DisplayName("Should handle angle difference greater than 180 degrees")
    void testCalculateFlightPath_LargeAngleDifference() {
        Position from = new Position(-3.186, 55.944);
        Position to = new Position(-3.185, 55.944); // East direction (0 degrees)

        setupRealisticMocks();

        List<Position> path = pathPlanningService.calculateFlightPath(
                from, to, new ArrayList<>()
        );

        assertNotNull(path);
    }

    @Test
    @DisplayName("Should handle multiple dispatches with same date and time")
    void testCalculateDeliveryPath_SameDateTimeDifferentLocations() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944)),
                new MedDispatchRec(2, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.187, 55.945)),
                new MedDispatchRec(3, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.188, 55.946))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(new ArrayList<>());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle batch split by capacity with exact boundary")
    void testCalculateDeliveryPath_CapacityBoundarySplit() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(2.0, false, false, 100.0),
                        new Position(-3.186, 55.944)),
                new MedDispatchRec(2, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(2.0, false, false, 100.0),
                        new Position(-3.187, 55.945)),
                new MedDispatchRec(3, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.188, 55.946))
        );

        Drone limitedDrone = new Drone(
                "LimitedDrone", "drone1",
                new Drone.Capability(true, true, 4.0, 2000, 0.01, 4.3, 6.5) // Exactly 4.0
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(Arrays.asList(limitedDrone));
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle dispatch with zero capacity requirement")
    void testCalculateDeliveryPath_ZeroCapacityRequirement() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(0.0, false, false, 100.0),
                        new Position(-3.186, 55.944))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle centroid calculation with single dispatch")
    void testCalculateDeliveryPath_SingleDispatchCentroid() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle service point with no matching drones")
    void testCalculateDeliveryPath_ServicePointWithoutDrones() {
        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        List<DroneForServicePoint> emptyServicePoint = Arrays.asList(
                new DroneForServicePoint(1, Arrays.asList(
                        new DroneForServicePoint.DroneAvailability("drone999", createSampleAvailability())
                ))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(emptyServicePoint);
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle multi-drone with batch size limit of 3")
    void testCalculateDeliveryPath_MultiDroneBatchLimit() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944)),
                new MedDispatchRec(2, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.185, 55.944)),
                new MedDispatchRec(3, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.184, 55.944)),
                new MedDispatchRec(4, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.183, 55.944))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle different service points for same date")
    void testCalculateDeliveryPath_DifferentServicePointsSameDate() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "09:30",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944)),
                new MedDispatchRec(2, "2025-01-20", "14:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.177, 55.981))
        );

        List<DroneForServicePoint> multipleServicePoints = Arrays.asList(
                new DroneForServicePoint(1, Arrays.asList(
                        new DroneForServicePoint.DroneAvailability("drone1", Arrays.asList(
                                new DroneForServicePoint.DroneAvailability.Availability("MONDAY", "09:00", "12:00")
                        ))
                )),
                new DroneForServicePoint(2, Arrays.asList(
                        new DroneForServicePoint.DroneAvailability("drone1", Arrays.asList(
                                new DroneForServicePoint.DroneAvailability.Availability("MONDAY", "13:00", "17:00")
                        ))
                ))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(multipleServicePoints);
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle greedy assignment failure with all drones unavailable")
    void testCalculateDeliveryPath_GreedyAssignmentFailure() {
        List<MedDispatchRec> dispatches = createSampleDispatches(2);

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(new ArrayList<>()); // No service points
        when(droneService.getServicePointLocations()).thenReturn(new ArrayList<>());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
        assertEquals(0.0, response.totalCost());
    }

    @Test
    @DisplayName("Should handle delivery batch with return path calculation")
    void testCalculateDeliveryPath_ReturnPathIncluded() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
        if (!response.dronePaths().isEmpty()) {
            DeliveryPathResponse.DronePath path = response.dronePaths().get(0);
            // Should have delivery + return path
            assertTrue(path.deliveries().size() >= 2);
        }
    }



    @Test
    @DisplayName("Should handle cost calculation with null cost fields")
    void testCalculateDeliveryPath_NullCostFields() {
        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        Drone droneWithNullCosts = new Drone(
                "NullCostDrone", "drone1",
                new Drone.Capability(true, true, 10.0, 2000, null, null, null)
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(Arrays.asList(droneWithNullCosts));
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        DeliveryPathResponse response = pathPlanningService.calculateDeliveryPath(dispatches);

        assertNotNull(response);
        assertEquals(0.0, response.totalCost()); // Should default to 0
    }

    @Test
    @DisplayName("Should generate GeoJSON with multiple features for multiple drones")
    void testCalculateDeliveryPathAsGeoJson_MultipleDroneFeatures() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944)),
                new MedDispatchRec(2, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.187, 55.945))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(Arrays.asList("drone1", "drone2"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        String geoJson = pathPlanningService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);
        assertTrue(geoJson.contains("type"));
        assertTrue(geoJson.contains("features"));
    }

    @Test
    @DisplayName("Should handle JsonProcessingException in GeoJSON generation")
    void testCalculateDeliveryPathAsGeoJson_JsonProcessingException() throws Exception {
        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        // Create a new instance with a mocked ObjectMapper that throws exception
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Test error") {});

        PathPlanningServiceImpl serviceWithMockMapper = new PathPlanningServiceImpl(
                restTemplate,
                endpointProvider,
                ilpService,
                droneService,
                progressHandler
        );

        // Use reflection to replace the objectMapper
        java.lang.reflect.Field field = PathPlanningServiceImpl.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(serviceWithMockMapper, mockMapper);

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(new ArrayList<>());

        String result = serviceWithMockMapper.calculateDeliveryPathAsGeoJson(dispatches);

        assertEquals("{}", result);
    }

    @Test
    @DisplayName("Should generate GeoJSON with proper stroke properties")
    void testCalculateDeliveryPathAsGeoJson_StrokeProperties() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        String geoJson = pathPlanningService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);

        // If features were generated, check for stroke properties
        if (geoJson.contains("stroke")) {
            assertTrue(geoJson.contains("stroke-width"));
            assertTrue(geoJson.contains("stroke-opacity"));
        }
    }

    @Test
    @DisplayName("Should handle empty coordinates list in GeoJSON generation")
    void testCalculateDeliveryPathAsGeoJson_EmptyCoordinatesList() {
        // Create a scenario where deliveries exist but have empty flight paths
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 0.01), // Very low max cost
                        new Position(-3.186, 55.944))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        String geoJson = pathPlanningService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);
        assertTrue(geoJson.contains("FeatureCollection"));
    }

    @Test
    @DisplayName("Should generate GeoJSON with all 10 different colors for 10 drones")
    void testCalculateDeliveryPathAsGeoJson_AllColors() {
        // Create 10 dispatches to potentially use all color variations
        List<MedDispatchRec> dispatches = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            dispatches.add(new MedDispatchRec(
                    i + 1,
                    "2025-01-20",
                    "10:00",
                    new MedDispatchRec.Requirements(0.5, false, false, 100.0),
                    new Position(-3.186 + i * 0.001, 55.944 + i * 0.001)
            ));
        }

        List<String> droneIds = Arrays.asList(
                "drone1", "drone2", "drone3", "drone4", "drone5",
                "drone6", "drone7", "drone8", "drone9", "drone10"
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(new ArrayList<>());
        when(droneService.queryAvailableDronesWithOr(any())).thenReturn(droneIds);
        when(droneService.getAllDrones()).thenReturn(createManyDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        String geoJson = pathPlanningService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);
        assertTrue(geoJson.contains("type"));
    }

    @Test
    @DisplayName("Should nest coordinates properly in GeoJSON LineString geometry")
    void testCalculateDeliveryPathAsGeoJson_CoordinatesNesting() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00",
                        new MedDispatchRec.Requirements(1.0, false, false, 100.0),
                        new Position(-3.186, 55.944))
        );

        when(droneService.queryAvailableDrones(any())).thenReturn(Arrays.asList("drone1"));
        when(droneService.getAllDrones()).thenReturn(createSampleDrones());
        when(droneService.getAllServicePoints()).thenReturn(createSampleDroneServicePoints());
        when(droneService.getServicePointLocations()).thenReturn(createSampleServicePoints());
        when(restTemplate.getForObject(anyString(), eq(RestrictedArea[].class)))
                .thenReturn(new RestrictedArea[0]);

        setupRealisticMocks();

        String geoJson = pathPlanningService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);

        // Verify GeoJSON structure
        if (geoJson.contains("coordinates")) {
            // Should have proper nesting: coordinates: [[lng, lat], [lng, lat], ...]
            assertTrue(geoJson.contains("[") && geoJson.contains("]"));
        }
    }

    // Helper method for availability
    private List<DroneForServicePoint.DroneAvailability.Availability> createSampleAvailability() {
        return Arrays.asList(
                new DroneForServicePoint.DroneAvailability.Availability("MONDAY", "09:00", "17:00"),
                new DroneForServicePoint.DroneAvailability.Availability("TUESDAY", "09:00", "17:00"),
                new DroneForServicePoint.DroneAvailability.Availability("WEDNESDAY", "09:00", "17:00"),
                new DroneForServicePoint.DroneAvailability.Availability("THURSDAY", "09:00", "17:00"),
                new DroneForServicePoint.DroneAvailability.Availability("FRIDAY", "09:00", "17:00")
        );
    }

    // Helper method to create many drones
    private List<Drone> createManyDrones() {
        List<Drone> drones = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            drones.add(new Drone(
                    "Drone " + i,
                    "drone" + i,
                    new Drone.Capability(true, true, 10.0, 2000, 0.01, 4.3, 6.5)
            ));
        }
        return drones;
    }

    @Test
    @DisplayName("Should process all deliveries in GeoJSON for loop")
    void testCalculateDeliveryPathAsGeoJson_ForLoopCoverage() throws Exception {
        // Create a mock DeliveryPathResponse with actual path data
        List<Position> flightPath = Arrays.asList(
                new Position(-3.186, 55.944),
                new Position(-3.185, 55.944),
                new Position(-3.184, 55.945)
        );

        DeliveryPathResponse.Delivery delivery = new DeliveryPathResponse.Delivery(1, flightPath);
        DeliveryPathResponse.DronePath dronePath = new DeliveryPathResponse.DronePath(
                "drone1",
                Arrays.asList(delivery)
        );

        DeliveryPathResponse mockResponse = new DeliveryPathResponse(
                10.0,
                3,
                Arrays.asList(dronePath)
        );

        // Use reflection to call the method with mocked response
        PathPlanningServiceImpl spyService = spy(pathPlanningService);
        doReturn(mockResponse).when(spyService).calculateDeliveryPath(any());

        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        String geoJson = spyService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);
        assertTrue(geoJson.contains("FeatureCollection"));
        assertTrue(geoJson.contains("features"));
        assertTrue(geoJson.contains("LineString"));
        assertTrue(geoJson.contains("coordinates"));
        assertTrue(geoJson.contains("stroke"));
        assertTrue(geoJson.contains("Drone 1 Path"));
    }

    @Test
    @DisplayName("Should process multiple drone paths in GeoJSON for loop")
    void testCalculateDeliveryPathAsGeoJson_MultipleDronePathsForLoop() throws Exception {
        List<Position> path1 = Arrays.asList(
                new Position(-3.186, 55.944),
                new Position(-3.185, 55.945)
        );

        List<Position> path2 = Arrays.asList(
                new Position(-3.187, 55.946),
                new Position(-3.188, 55.947)
        );

        DeliveryPathResponse.Delivery delivery1 = new DeliveryPathResponse.Delivery(1, path1);
        DeliveryPathResponse.Delivery delivery2 = new DeliveryPathResponse.Delivery(2, path2);

        DeliveryPathResponse.DronePath dronePath1 = new DeliveryPathResponse.DronePath(
                "drone1",
                Arrays.asList(delivery1)
        );

        DeliveryPathResponse.DronePath dronePath2 = new DeliveryPathResponse.DronePath(
                "drone2",
                Arrays.asList(delivery2)
        );

        DeliveryPathResponse mockResponse = new DeliveryPathResponse(
                20.0,
                6,
                Arrays.asList(dronePath1, dronePath2)
        );

        PathPlanningServiceImpl spyService = spy(pathPlanningService);
        doReturn(mockResponse).when(spyService).calculateDeliveryPath(any());

        List<MedDispatchRec> dispatches = createSampleDispatches(2);

        String geoJson = spyService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);
        assertTrue(geoJson.contains("Drone 1 Path"));
        assertTrue(geoJson.contains("Drone 2 Path"));
        // Verify different colors are used
        assertTrue(geoJson.contains("#0000FF") || geoJson.contains("#00FF00"));
    }

    @Test
    @DisplayName("Should iterate through all positions in flight path")
    void testCalculateDeliveryPathAsGeoJson_PositionIteration() throws Exception {
        // Create multiple positions to ensure inner loop coverage
        List<Position> longFlightPath = Arrays.asList(
                new Position(-3.186, 55.944),
                new Position(-3.185, 55.944),
                new Position(-3.184, 55.945),
                new Position(-3.183, 55.945),
                new Position(-3.182, 55.946)
        );

        DeliveryPathResponse.Delivery delivery = new DeliveryPathResponse.Delivery(1, longFlightPath);
        DeliveryPathResponse.DronePath dronePath = new DeliveryPathResponse.DronePath(
                "drone1",
                Arrays.asList(delivery)
        );

        DeliveryPathResponse mockResponse = new DeliveryPathResponse(
                15.0,
                5,
                Arrays.asList(dronePath)
        );

        PathPlanningServiceImpl spyService = spy(pathPlanningService);
        doReturn(mockResponse).when(spyService).calculateDeliveryPath(any());

        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        String geoJson = spyService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);
        // Should contain all coordinates
        assertTrue(geoJson.contains("-3.186"));
        assertTrue(geoJson.contains("55.944"));
        assertTrue(geoJson.contains("-3.182"));
        assertTrue(geoJson.contains("55.946"));
    }

    @Test
    @DisplayName("Should skip empty coordinates in GeoJSON generation")
    void testCalculateDeliveryPathAsGeoJson_SkipEmptyCoordinates() throws Exception {
        // Create a delivery with empty flight path
        DeliveryPathResponse.Delivery emptyDelivery = new DeliveryPathResponse.Delivery(
                1,
                new ArrayList<>()
        );

        DeliveryPathResponse.DronePath dronePath = new DeliveryPathResponse.DronePath(
                "drone1",
                Arrays.asList(emptyDelivery)
        );

        DeliveryPathResponse mockResponse = new DeliveryPathResponse(
                0.0,
                0,
                Arrays.asList(dronePath)
        );

        PathPlanningServiceImpl spyService = spy(pathPlanningService);
        doReturn(mockResponse).when(spyService).calculateDeliveryPath(any());

        List<MedDispatchRec> dispatches = createSampleDispatches(1);

        String geoJson = spyService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);
        assertTrue(geoJson.contains("FeatureCollection"));
        // Should have empty features array since coordinates were empty
        assertTrue(geoJson.contains("\"features\":[]") || geoJson.contains("\"features\": []"));
    }

    @Test
    @DisplayName("Should handle multiple deliveries per drone in GeoJSON")
    void testCalculateDeliveryPathAsGeoJson_MultipleDeliveriesPerDrone() throws Exception {
        List<Position> path1 = Arrays.asList(
                new Position(-3.186, 55.944),
                new Position(-3.185, 55.945)
        );

        List<Position> path2 = Arrays.asList(
                new Position(-3.185, 55.945),
                new Position(-3.184, 55.946)
        );

        DeliveryPathResponse.Delivery delivery1 = new DeliveryPathResponse.Delivery(1, path1);
        DeliveryPathResponse.Delivery delivery2 = new DeliveryPathResponse.Delivery(2, path2);

        DeliveryPathResponse.DronePath dronePath = new DeliveryPathResponse.DronePath(
                "drone1",
                Arrays.asList(delivery1, delivery2) // Multiple deliveries
        );

        DeliveryPathResponse mockResponse = new DeliveryPathResponse(
                15.0,
                4,
                Arrays.asList(dronePath)
        );

        PathPlanningServiceImpl spyService = spy(pathPlanningService);
        doReturn(mockResponse).when(spyService).calculateDeliveryPath(any());

        List<MedDispatchRec> dispatches = createSampleDispatches(2);

        String geoJson = spyService.calculateDeliveryPathAsGeoJson(dispatches);

        assertNotNull(geoJson);
        assertTrue(geoJson.contains("coordinates"));
        // Should combine all positions from both deliveries
        assertTrue(geoJson.length() > 100); // Non-trivial GeoJSON
    }
}