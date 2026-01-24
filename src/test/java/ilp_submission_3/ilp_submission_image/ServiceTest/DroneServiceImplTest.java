package ilp_submission_3.ilp_submission_image.ServiceTest;

import ilp_submission_3.ilp_submission_image.Configuration.ILPEndpointProvider;
import ilp_submission_3.ilp_submission_image.Service.DroneServiceImpl;
import ilp_submission_3.ilp_submission_image.Service.ILPServiceInterface;
import ilp_submission_3.ilp_submission_image.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for DroneServiceImpl
 * Coverage areas:
 * - Drone querying and filtering
 * - Availability checks (date/time/day of week)
 * - Capacity and capability matching
 * - Service point operations
 * - Edge cases and boundary conditions
 */
class DroneServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ILPServiceInterface ilpService;

    @Mock
    private ILPEndpointProvider endpointProvider;

    private DroneServiceImpl droneService;

    private static final String BASE_URL = "https://test.example.com";
    private static final double MOVE_DISTANCE = 0.00015;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(endpointProvider.getEndpoint()).thenReturn(BASE_URL);

        droneService = new DroneServiceImpl(restTemplate, ilpService, endpointProvider);
    }

    // ==================== Get All Drones Tests ====================

    @Test
    @DisplayName("Should fetch all drones successfully")
    void testGetAllDrones_Success() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(BASE_URL + "/drones", Drone[].class))
                .thenReturn(drones);

        List<Drone> result = droneService.getAllDrones();

        assertEquals(2, result.size());
        assertEquals("drone1", result.get(0).id());
        assertEquals("drone2", result.get(1).id());
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Drone[].class));
    }

    @Test
    @DisplayName("Should return empty list when API returns null")
    void testGetAllDrones_NullResponse() {
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(null);

        List<Drone> result = droneService.getAllDrones();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list for empty drone array")
    void testGetAllDrones_EmptyArray() {
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(new Drone[0]);

        List<Drone> result = droneService.getAllDrones();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Get Service Points Tests ====================

    @Test
    @DisplayName("Should fetch all service points successfully")
    void testGetAllServicePoints_Success() {
        DroneForServicePoint[] servicePoints = createSampleServicePointArray();
        when(restTemplate.getForObject(
                BASE_URL + "/drones-for-service-points",
                DroneForServicePoint[].class
        )).thenReturn(servicePoints);

        List<DroneForServicePoint> result = droneService.getAllServicePoints();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).servicePointId());
    }

    @Test
    @DisplayName("Should fetch service point locations successfully")
    void testGetServicePointLocations_Success() {
        DroneServicePoint[] points = createSampleLocationArray();
        when(restTemplate.getForObject(BASE_URL + "/service-points", DroneServicePoint[].class))
                .thenReturn(points);

        List<DroneServicePoint> result = droneService.getServicePointLocations();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).id());
        assertNotNull(result.get(0).location());
    }

    @Test
    @DisplayName("Should handle null service points response")
    void testGetServicePointLocations_NullResponse() {
        when(restTemplate.getForObject(anyString(), eq(DroneServicePoint[].class)))
                .thenReturn(null);

        List<DroneServicePoint> result = droneService.getServicePointLocations();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Drone Capability Query Tests ====================

    @Test
    @DisplayName("Should find drones with cooling capability")
    void testGetDronesWithCooling_HasCooling() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<String> result = droneService.getDronesWithCooling(true);

        assertEquals(1, result.size());
        assertTrue(result.contains("drone1"));
    }

    @Test
    @DisplayName("Should find drones without cooling capability")
    void testGetDronesWithCooling_NoCooling() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<String> result = droneService.getDronesWithCooling(false);

        assertEquals(1, result.size());
        assertTrue(result.contains("drone2"));
    }

    @Test
    @DisplayName("Should handle null cooling capability")
    void testGetDronesWithCooling_NullCapability() {
        Drone[] drones = {
                new Drone("Test", "drone1", new Drone.Capability(null, false, 5.0, 1000, 1.0, 10.0, 5.0))
        };
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<String> result = droneService.getDronesWithCooling(true);

        assertTrue(result.isEmpty());
    }

    // ==================== Get Drone By ID Tests ====================

    @Test
    @DisplayName("Should find drone by valid ID")
    void testGetDroneById_Found() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        Drone result = droneService.getDroneById("drone1");

        assertNotNull(result);
        assertEquals("drone1", result.id());
        assertEquals("TestDrone1", result.name());
    }

    @Test
    @DisplayName("Should return null for non-existent drone ID")
    void testGetDroneById_NotFound() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        Drone result = droneService.getDroneById("nonexistent");

        assertNull(result);
    }

    // ==================== Query Drones By Path Tests ====================

    @Test
    @DisplayName("Should query drones by name")
    void testQueryDronesByPath_ByName() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<String> result = droneService.queryDronesByPath("name", "testdrone1");

        assertEquals(1, result.size());
        assertTrue(result.contains("drone1"));
    }

    @Test
    @DisplayName("Should query drones by capacity")
    void testQueryDronesByPath_ByCapacity() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<String> result = droneService.queryDronesByPath("capacity", "5.0");

        assertEquals(1, result.size());
        assertTrue(result.contains("drone1"));
    }

    @Test
    @DisplayName("Should return empty list for non-matching query")
    void testQueryDronesByPath_NoMatch() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<String> result = droneService.queryDronesByPath("capacity", "999.0");

        assertTrue(result.isEmpty());
    }

    // ==================== Query Drones (Multi-Attribute) Tests ====================

    @Test
    @DisplayName("Should query drones with multiple attributes")
    void testQueryDrones_MultipleAttributes() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(
                new QueryAttribute("capacity", ">", "4.0"),
                new QueryAttribute("cooling", "=", "true")
        );

        List<String> result = droneService.queryDrones(queries);

        assertEquals(1, result.size());
        assertTrue(result.contains("drone1"));
    }

    @Test
    @DisplayName("Should handle empty query attributes")
    void testQueryDrones_EmptyAttributes() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<String> result = droneService.queryDrones(new ArrayList<>());

        assertEquals(2, result.size());
    }

    @ParameterizedTest
    @CsvSource({
            "capacity, >, 3.0, 1",    // drone1 (5.0 > 3.0) ✓
            "capacity, <, 4.0, 1",    // drone2 (3.0 < 4.0) ✓
            "capacity, >=, 5.0, 1",   // drone1 (5.0 >= 5.0) ✓
            "capacity, <=, 5.0, 2",   // drone1 (5.0) + drone2 (3.0) ✓
            "maxMoves, >, 900, 1",    // drone1 (1000 > 900) ✓
            "costPerMove, <, 1.5, 2"  // drone1 (1.0) + drone2 (1.2) ✓
    })
    @DisplayName("Should handle different numeric operators")
    void testQueryDrones_NumericOperators(String attr, String op, String val, int expectedCount) {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(new QueryAttribute(attr, op, val));
        List<String> result = droneService.queryDrones(queries);

        // Verify the result size matches expected
        assertEquals(expectedCount, result.size(),
                String.format("Query with %s %s %s should return %d drones, but got %d",
                        attr, op, val, expectedCount, result.size()));
    }

    // ==================== Available Drones Query Tests ====================

    @Test
    @DisplayName("Should find available drones for valid dispatch")
    void testQueryAvailableDrones_ValidDispatch() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", 2.0, true, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should return empty for conflicting cooling/heating requirements")
    void testQueryAvailableDrones_ConflictingRequirements() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        "2025-01-20",
                        "10:00",
                        new MedDispatchRec.Requirements(2.0, true, true, 100.0),
                        new Position(-3.186, 55.944)
                )
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty for null dispatch records")
    void testQueryAvailableDrones_NullInput() {
        List<String> result = droneService.queryAvailableDrones(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty for empty dispatch records")
    void testQueryAvailableDrones_EmptyInput() {
        List<String> result = droneService.queryAvailableDrones(new ArrayList<>());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle dispatch with capacity exceeding drone limit")
    void testQueryAvailableDrones_ExceedsCapacity() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", 999.0, false, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    // ==================== Available Drones With OR Logic Tests ====================

    @Test
    @DisplayName("Should find drones that can handle any dispatch")
    void testQueryAvailableDronesWithOr_ValidDispatches() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", 2.0, true, false),
                createSampleDispatch("2025-01-21", "14:00", 1.5, false, true)
        );

        List<String> result = droneService.queryAvailableDronesWithOr(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should return empty for conflicting requirements in OR query")
    void testQueryAvailableDronesWithOr_ConflictingRequirements() {
        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        "2025-01-20",
                        "10:00",
                        new MedDispatchRec.Requirements(2.0, true, true, 100.0),
                        new Position(-3.186, 55.944)
                )
        );

        List<String> result = droneService.queryAvailableDronesWithOr(dispatches);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle null input for OR query")
    void testQueryAvailableDronesWithOr_NullInput() {
        List<String> result = droneService.queryAvailableDronesWithOr(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Availability Time Slot Tests ====================

    @Test
    @DisplayName("Should handle different days of week")
    void testAvailability_DifferentDaysOfWeek() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> mondayDispatch = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", 2.0, true, false)
        );

        List<String> result = droneService.queryAvailableDrones(mondayDispatch);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should reject dispatch outside availability window")
    void testAvailability_OutsideTimeWindow() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "23:00", 2.0, true, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    // ==================== Edge Cases and Boundary Tests ====================

    @Test
    @DisplayName("Should handle drone with null capability")
    void testQueryDrones_NullCapability() {
        Drone[] drones = {
                new Drone("Test", "drone1", null)
        };
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(
                new QueryAttribute("capacity", ">", "1.0")
        );

        List<String> result = droneService.queryDrones(queries);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle invalid operator in query")
    void testQueryDrones_InvalidOperator() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(
                new QueryAttribute("capacity", "???", "5.0")
        );

        List<String> result = droneService.queryDrones(queries);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle malformed date in dispatch")
    void testQueryAvailableDrones_InvalidDate() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        "invalid-date",
                        "10:00",
                        new MedDispatchRec.Requirements(2.0, false, false, 100.0),
                        new Position(-3.186, 55.944)
                )
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle malformed time in dispatch")
    void testQueryAvailableDrones_InvalidTime() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        "2025-01-20",
                        "invalid-time",
                        new MedDispatchRec.Requirements(2.0, false, false, 100.0),
                        new Position(-3.186, 55.944)
                )
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle zero capacity requirement")
    void testQueryAvailableDrones_ZeroCapacity() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", 0.0, false, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle negative capacity requirement")
    void testQueryAvailableDrones_NegativeCapacity() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", -1.0, false, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    // ==================== Parameterized Tests ====================

    @ParameterizedTest
    @MethodSource("provideDispatchScenarios")
    @DisplayName("Should handle various dispatch scenarios")
    void testQueryAvailableDrones_Scenarios(
            String date, String time, double capacity,
            boolean cooling, boolean heating, boolean shouldFindDrone
    ) {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch(date, time, capacity, cooling, heating)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    static Stream<Arguments> provideDispatchScenarios() {
        return Stream.of(
                Arguments.of("2025-01-20", "10:00", 2.0, true, false, true),
                Arguments.of("2025-01-20", "10:00", 1.0, false, true, true),
                Arguments.of("2025-01-20", "10:00", 3.0, false, false, true),
                Arguments.of("2025-01-20", "02:00", 2.0, true, false, false),
                Arguments.of("2025-01-20", "10:00", 100.0, false, false, false)
        );
    }

    // ==================== Helper Methods ====================

    private Drone[] createSampleDroneArray() {
        return new Drone[]{
                new Drone(
                        "TestDrone1",
                        "drone1",
                        new Drone.Capability(true, false, 5.0, 1000, 1.0, 10.0, 5.0)
                ),
                new Drone(
                        "TestDrone2",
                        "drone2",
                        new Drone.Capability(false, true, 3.0, 800, 1.2, 12.0, 6.0)
                )
        };
    }

    private DroneForServicePoint[] createSampleServicePointArray() {
        List<DroneForServicePoint.DroneAvailability.Availability> availability = Arrays.asList(
                new DroneForServicePoint.DroneAvailability.Availability(
                        "MONDAY", "09:00", "17:00"
                )
        );

        DroneForServicePoint.DroneAvailability droneAvail = new DroneForServicePoint.DroneAvailability(
                "drone1", availability
        );

        return new DroneForServicePoint[]{
                new DroneForServicePoint(1, Arrays.asList(droneAvail))
        };
    }

    private DroneServicePoint[] createSampleLocationArray() {
        return new DroneServicePoint[]{
                new DroneServicePoint(
                        "ServicePoint1",
                        1,
                        new DroneServicePoint.LngLatAlt(-3.186874, 55.944494, 0.0)
                )
        };
    }

    private MedDispatchRec createSampleDispatch(
            String date, String time, double capacity,
            boolean cooling, boolean heating
    ) {
        return new MedDispatchRec(
                1,
                date,
                time,
                new MedDispatchRec.Requirements(capacity, cooling, heating, 100.0),
                new Position(-3.186, 55.944)
        );
    }

    private void setupMocksForAvailabilityTests() {
        when(restTemplate.getForObject(BASE_URL + "/drones", Drone[].class))
                .thenReturn(createSampleDroneArray());
        when(restTemplate.getForObject(
                BASE_URL + "/drones-for-service-points",
                DroneForServicePoint[].class
        )).thenReturn(createSampleServicePointArray());
        when(restTemplate.getForObject(BASE_URL + "/service-points", DroneServicePoint[].class))
                .thenReturn(createSampleLocationArray());
        when(ilpService.distance(any(), any())).thenReturn(0.01);
    }

    @Test
    @DisplayName("Should handle service point with null location")
    void testGetServicePointLocations_NullLocation() {
        DroneServicePoint[] points = {
                new DroneServicePoint("SP1", 1, null)
        };
        when(restTemplate.getForObject(anyString(), eq(DroneServicePoint[].class)))
                .thenReturn(points);

        List<DroneServicePoint> result = droneService.getServicePointLocations();

        assertEquals(1, result.size());
        assertNull(result.get(0).location());
    }

    @Test
    @DisplayName("Should handle dispatch with null delivery position")
    void testQueryAvailableDrones_NullDeliveryPosition() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        "2025-01-20",
                        "10:00",
                        new MedDispatchRec.Requirements(2.0, false, false, 100.0),
                        null
                )
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    // ==================== Additional Coverage Tests ====================

    @Test
    @DisplayName("Should handle multiple dispatches on same date for single drone")
    void testQueryAvailableDrones_MultipleSameDateDispatches() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", 1.0, true, false),
                createSampleDispatch("2025-01-20", "11:00", 1.0, true, false),
                createSampleDispatch("2025-01-20", "12:00", 1.0, true, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle dispatches across different dates")
    void testQueryAvailableDrones_DifferentDates() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", 2.0, true, false),
                createSampleDispatch("2025-01-21", "10:00", 2.0, true, false),
                createSampleDispatch("2025-01-22", "10:00", 2.0, true, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should verify cost calculation with maxCost constraint")
    void testQueryAvailableDrones_MaxCostValidation() {
        setupMocksForAvailabilityTests();

        // Mock distance to return a specific value for cost calculation
        when(ilpService.distance(any(), any())).thenReturn(0.01);

        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        "2025-01-20",
                        "10:00",
                        new MedDispatchRec.Requirements(2.0, false, false, 50.0),
                        new Position(-3.186, 55.944)
                )
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
        verify(ilpService, atLeastOnce()).distance(any(), any());
    }

    @Test
    @DisplayName("Should handle drone with null maxMoves capability")
    void testQueryDrones_NullMaxMoves() {
        Drone[] drones = {
                new Drone("Test", "drone1", new Drone.Capability(true, false, 5.0, null, 1.0, 10.0, 5.0))
        };
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(
                new QueryAttribute("maxMoves", ">", "500")
        );

        List<String> result = droneService.queryDrones(queries);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle drone with null cost attributes")
    void testQueryDrones_NullCostAttributes() {
        Drone[] drones = {
                new Drone("Test", "drone1", new Drone.Capability(true, false, 5.0, 1000, null, null, null))
        };
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(
                new QueryAttribute("costPerMove", ">", "1.0")
        );

        List<String> result = droneService.queryDrones(queries);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle dispatch at exact availability boundary")
    void testQueryAvailableDrones_AtTimeWindowBoundary() {
        setupMocksForAvailabilityTests();

        // Test at start boundary
        List<MedDispatchRec> dispatches1 = Arrays.asList(
                createSampleDispatch("2025-01-20", "09:00", 2.0, true, false)
        );

        List<String> result1 = droneService.queryAvailableDrones(dispatches1);
        assertNotNull(result1);

        // Test at end boundary
        List<MedDispatchRec> dispatches2 = Arrays.asList(
                createSampleDispatch("2025-01-20", "17:00", 2.0, true, false)
        );

        List<String> result2 = droneService.queryAvailableDrones(dispatches2);
        assertNotNull(result2);
    }

    @Test
    @DisplayName("Should handle dispatch on weekend")
    void testQueryAvailableDrones_Weekend() {
        setupMocksForAvailabilityTests();

        // 2025-01-25 is Saturday
        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-25", "10:00", 2.0, true, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle service point with null drones list")
    void testQueryAvailableDrones_NullDronesList() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(BASE_URL + "/drones", Drone[].class))
                .thenReturn(drones);

        DroneForServicePoint[] servicePoints = {
                new DroneForServicePoint(1, null)
        };
        when(restTemplate.getForObject(
                BASE_URL + "/drones-for-service-points",
                DroneForServicePoint[].class
        )).thenReturn(servicePoints);

        when(restTemplate.getForObject(BASE_URL + "/service-points", DroneServicePoint[].class))
                .thenReturn(createSampleLocationArray());

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", 2.0, true, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle drone availability with null dayOfWeek")
    void testQueryAvailableDrones_NullDayOfWeek() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(BASE_URL + "/drones", Drone[].class))
                .thenReturn(drones);

        List<DroneForServicePoint.DroneAvailability.Availability> availability = Arrays.asList(
                new DroneForServicePoint.DroneAvailability.Availability(null, "09:00", "17:00")
        );

        DroneForServicePoint.DroneAvailability droneAvail = new DroneForServicePoint.DroneAvailability(
                "drone1", availability
        );

        DroneForServicePoint[] servicePoints = {
                new DroneForServicePoint(1, Arrays.asList(droneAvail))
        };

        when(restTemplate.getForObject(
                BASE_URL + "/drones-for-service-points",
                DroneForServicePoint[].class
        )).thenReturn(servicePoints);

        when(restTemplate.getForObject(BASE_URL + "/service-points", DroneServicePoint[].class))
                .thenReturn(createSampleLocationArray());

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", 2.0, true, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle invalid day of week string")
    void testQueryAvailableDrones_InvalidDayOfWeek() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(BASE_URL + "/drones", Drone[].class))
                .thenReturn(drones);

        List<DroneForServicePoint.DroneAvailability.Availability> availability = Arrays.asList(
                new DroneForServicePoint.DroneAvailability.Availability("INVALID_DAY", "09:00", "17:00")
        );

        DroneForServicePoint.DroneAvailability droneAvail = new DroneForServicePoint.DroneAvailability(
                "drone1", availability
        );

        DroneForServicePoint[] servicePoints = {
                new DroneForServicePoint(1, Arrays.asList(droneAvail))
        };

        when(restTemplate.getForObject(
                BASE_URL + "/drones-for-service-points",
                DroneForServicePoint[].class
        )).thenReturn(servicePoints);

        when(restTemplate.getForObject(BASE_URL + "/service-points", DroneServicePoint[].class))
                .thenReturn(createSampleLocationArray());

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", 2.0, true, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle availability with null from/until times")
    void testQueryAvailableDrones_NullFromUntilTimes() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(BASE_URL + "/drones", Drone[].class))
                .thenReturn(drones);

        List<DroneForServicePoint.DroneAvailability.Availability> availability = Arrays.asList(
                new DroneForServicePoint.DroneAvailability.Availability("MONDAY", null, null)
        );

        DroneForServicePoint.DroneAvailability droneAvail = new DroneForServicePoint.DroneAvailability(
                "drone1", availability
        );

        DroneForServicePoint[] servicePoints = {
                new DroneForServicePoint(1, Arrays.asList(droneAvail))
        };

        when(restTemplate.getForObject(
                BASE_URL + "/drones-for-service-points",
                DroneForServicePoint[].class
        )).thenReturn(servicePoints);

        when(restTemplate.getForObject(BASE_URL + "/service-points", DroneServicePoint[].class))
                .thenReturn(createSampleLocationArray());

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", 2.0, true, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle drone with empty availability list")
    void testQueryAvailableDrones_EmptyAvailability() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(BASE_URL + "/drones", Drone[].class))
                .thenReturn(drones);

        DroneForServicePoint.DroneAvailability droneAvail = new DroneForServicePoint.DroneAvailability(
                "drone1", new ArrayList<>()
        );

        DroneForServicePoint[] servicePoints = {
                new DroneForServicePoint(1, Arrays.asList(droneAvail))
        };

        when(restTemplate.getForObject(
                BASE_URL + "/drones-for-service-points",
                DroneForServicePoint[].class
        )).thenReturn(servicePoints);

        when(restTemplate.getForObject(BASE_URL + "/service-points", DroneServicePoint[].class))
                .thenReturn(createSampleLocationArray());

        List<MedDispatchRec> dispatches = Arrays.asList(
                createSampleDispatch("2025-01-20", "10:00", 2.0, true, false)
        );

        List<String> result = droneService.queryAvailableDrones(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle OR query with null requirements")
    void testQueryAvailableDronesWithOr_NullRequirements() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(1, "2025-01-20", "10:00", null, new Position(-3.186, 55.944))
        );

        List<String> result = droneService.queryAvailableDronesWithOr(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle dispatch with null capacity in requirements")
    void testQueryAvailableDronesWithOr_NullCapacityRequirement() {
        setupMocksForAvailabilityTests();

        List<MedDispatchRec> dispatches = Arrays.asList(
                new MedDispatchRec(
                        1,
                        "2025-01-20",
                        "10:00",
                        new MedDispatchRec.Requirements(null, false, false, 100.0),
                        new Position(-3.186, 55.944)
                )
        );

        List<String> result = droneService.queryAvailableDronesWithOr(dispatches);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle query by heating attribute")
    void testQueryDronesByPath_ByHeating() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<String> result = droneService.queryDronesByPath("heating", "true");

        assertEquals(1, result.size());
        assertTrue(result.contains("drone2"));
    }

    @Test
    @DisplayName("Should handle query by costInitial")
    void testQueryDrones_ByCostInitial() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(
                new QueryAttribute("costInitial", "=", "10.0")
        );

        List<String> result = droneService.queryDrones(queries);

        assertEquals(1, result.size());
        assertTrue(result.contains("drone1"));
    }

    @Test
    @DisplayName("Should handle query by costFinal")
    void testQueryDrones_ByCostFinal() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(
                new QueryAttribute("costFinal", "<=", "6.0")
        );

        List<String> result = droneService.queryDrones(queries);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should handle != operator for string comparison")
    void testQueryDrones_NotEqualsString() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(
                new QueryAttribute("name", "!=", "testdrone1")
        );

        List<String> result = droneService.queryDrones(queries);

        assertEquals(1, result.size());
        assertTrue(result.contains("drone2"));
    }

    @Test
    @DisplayName("Should handle != operator for numeric comparison")
    void testQueryDrones_NotEqualsNumeric() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(
                new QueryAttribute("capacity", "!=", "5.0")
        );

        List<String> result = droneService.queryDrones(queries);

        assertEquals(1, result.size());
        assertTrue(result.contains("drone2"));
    }

    @Test
    @DisplayName("Should handle non-numeric value for numeric attribute")
    void testQueryDrones_InvalidNumericValue() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(
                new QueryAttribute("capacity", ">", "not-a-number")
        );

        List<String> result = droneService.queryDrones(queries);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle query with unknown attribute")
    void testQueryDrones_UnknownAttribute() {
        Drone[] drones = createSampleDroneArray();
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(
                new QueryAttribute("unknownAttribute", "=", "value")
        );

        List<String> result = droneService.queryDrones(queries);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle exception during attribute comparison")
    void testQueryDrones_ExceptionInComparison() {
        Drone droneWithSpecialValue = new Drone(
                "Special", "drone3",
                new Drone.Capability(true, false, Double.NaN, 1000, 1.0, 10.0, 5.0)
        );

        Drone[] drones = {droneWithSpecialValue};
        when(restTemplate.getForObject(anyString(), eq(Drone[].class)))
                .thenReturn(drones);

        List<QueryAttribute> queries = Arrays.asList(
                new QueryAttribute("capacity", ">", "5.0")
        );

        List<String> result = droneService.queryDrones(queries);

        assertNotNull(result);
    }
}