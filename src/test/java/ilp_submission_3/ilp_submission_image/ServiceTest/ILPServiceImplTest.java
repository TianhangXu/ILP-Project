package ilp_submission_3.ilp_submission_image.ServiceTest;

import ilp_submission_3.ilp_submission_image.Service.ILPServiceImpl;
import ilp_submission_3.ilp_submission_image.Service.ILPServiceInterface;
import ilp_submission_3.ilp_submission_image.dto.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ILPServiceImpl
 * Coverage areas:
 * - Distance calculations
 * - Proximity checks (isClose)
 * - Region containment checks (isInRegion)
 * - Position calculations (nextPosition)
 * - Edge cases and boundary conditions
 */
class ILPServiceImplTest {

    private ILPServiceInterface ilpService;

    @BeforeEach
    void setUp() {
        ilpService = new ILPServiceImpl();
    }

    // ==================== Distance Calculation Tests ====================

    @Test
    @DisplayName("Distance between same points should be zero")
    void testDistanceSamePoints() {
        Position p1 = new Position(-3.192473, 55.946233);
        Position p2 = new Position(-3.192473, 55.946233);

        double distance = ilpService.distance(p1, p2);

        assertEquals(0.0, distance, 1e-10);
    }

    @Test
    @DisplayName("Distance calculation should be symmetric")
    void testDistanceSymmetric() {
        Position p1 = new Position(-3.192473, 55.946233);
        Position p2 = new Position(-3.192473, 55.942617);

        double dist1 = ilpService.distance(p1, p2);
        double dist2 = ilpService.distance(p2, p1);

        assertEquals(dist1, dist2, 1e-10);
    }

    @Test
    @DisplayName("Distance should calculate correctly for known values")
    void testDistanceKnownValues() {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(3.0, 4.0);

        double distance = ilpService.distance(p1, p2);

        assertEquals(5.0, distance, 1e-10);
    }

    @Test
    @DisplayName("Distance with very small values should handle precision")
    void testDistanceSmallValues() {
        Position p1 = new Position(0.000001, 0.000001);
        Position p2 = new Position(0.000002, 0.000002);

        double distance = ilpService.distance(p1, p2);
        double expected = Math.sqrt(2) * 0.000001;

        assertEquals(expected, distance, 1e-15);
    }

    @Test
    @DisplayName("Distance with negative coordinates (Edinburgh coordinates)")
    void testDistanceNegativeCoordinates() {
        Position p1 = new Position(-3.192473, 55.946233);
        Position p2 = new Position(-3.184319, 55.942617);

        double distance = ilpService.distance(p1, p2);
        assertEquals(0.00892, distance, 0.0001);
    }

    @Test
    @DisplayName("Distance with floating point precision issues")
    void testDistancePrecisionIssues() {
        Position p1 = new Position(-3.1924729999999998, 55.946233);
        Position p2 = new Position(-3.1924730000000001, 55.946233);

        double distance = ilpService.distance(p1, p2);

        assertEquals(0.0, distance, 1e-10);
    }

    // ==================== IsClose Tests ====================

    @Test
    @DisplayName("IsClose should return true when distance less than 0.00015")
    void testIsCloseTrue() {
        Position p1 = new Position(-3.192473, 55.946233);
        Position p2 = new Position(-3.192473, 55.946233);

        assertTrue(ilpService.isClose(p1, p2));
    }

    @Test
    @DisplayName("IsClose should return false when distance greater than 0.00015")
    void testIsCloseFalse() {
        Position p1 = new Position(-3.192473, 55.946233);
        Position p2 = new Position(-3.192473, 55.942617);

        assertFalse(ilpService.isClose(p1, p2));
    }

    @Test
    @DisplayName("IsClose should return true for points exactly at threshold")
    void testIsCloseAtThreshold() {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(0.00014, 0.0);

        assertTrue(ilpService.isClose(p1, p2));
    }

    @Test
    @DisplayName("IsClose with distance exactly at 0.00015 threshold should return false")
    void testIsCloseExactThreshold() {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(0.00015, 0.0);

        assertFalse(ilpService.isClose(p1, p2));
    }

    @Test
    @DisplayName("IsClose with distance just below threshold should return true")
    void testIsCloseJustBelowThreshold() {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(0.00014, 0.0);

        assertTrue(ilpService.isClose(p1, p2));
    }

    @Test
    @DisplayName("IsClose with distance just above threshold should return false")
    void testIsCloseJustAboveThreshold() {
        Position p1 = new Position(0.0, 0.0);
        Position p2 = new Position(0.00016, 0.0);

        assertFalse(ilpService.isClose(p1, p2));
    }

    // ==================== IsInRegion Tests ====================

    @Test
    @DisplayName("Point inside rectangle should return true")
    void testIsInRegionPointInside() {
        Position point = new Position(-3.188, 55.944);
        List<Position> vertices = Arrays.asList(
                new Position(-3.192473, 55.946233),
                new Position(-3.192473, 55.942617),
                new Position(-3.184319, 55.942617),
                new Position(-3.184319, 55.946233),
                new Position(-3.192473, 55.946233)
        );

        assertTrue(ilpService.isInRegion(point, vertices));
    }

    @Test
    @DisplayName("Point outside rectangle should return false")
    void testIsInRegionPointOutside() {
        Position point = new Position(-3.200, 55.950);
        List<Position> vertices = Arrays.asList(
                new Position(-3.192473, 55.946233),
                new Position(-3.192473, 55.942617),
                new Position(-3.184319, 55.942617),
                new Position(-3.184319, 55.946233),
                new Position(-3.192473, 55.946233)
        );

        assertFalse(ilpService.isInRegion(point, vertices));
    }

    @Test
    @DisplayName("Point on border should return true")
    void testIsInRegionPointOnBorder() {
        Position point = new Position(-3.192473, 55.944);
        List<Position> vertices = Arrays.asList(
                new Position(-3.192473, 55.946233),
                new Position(-3.192473, 55.942617),
                new Position(-3.184319, 55.942617),
                new Position(-3.184319, 55.946233),
                new Position(-3.192473, 55.946233)
        );

        assertTrue(ilpService.isInRegion(point, vertices));
    }

    @Test
    @DisplayName("Point at vertex should return true")
    void testIsInRegionPointAtVertex() {
        Position point = new Position(-3.192473, 55.946233);
        List<Position> vertices = Arrays.asList(
                new Position(-3.192473, 55.946233),
                new Position(-3.192473, 55.942617),
                new Position(-3.184319, 55.942617),
                new Position(-3.184319, 55.946233),
                new Position(-3.192473, 55.946233)
        );

        assertTrue(ilpService.isInRegion(point, vertices));
    }

    @Test
    @DisplayName("Triangle polygon should work correctly")
    void testIsInRegionTriangle() {
        Position pointInside = new Position(1.0, 1.0);
        Position pointOutside = new Position(5.0, 5.0);

        List<Position> triangle = Arrays.asList(
                new Position(0.0, 0.0),
                new Position(3.0, 0.0),
                new Position(1.5, 3.0),
                new Position(0.0, 0.0)
        );

        assertTrue(ilpService.isInRegion(pointInside, triangle));
        assertFalse(ilpService.isInRegion(pointOutside, triangle));
    }

    @Test
    @DisplayName("Complex polygon should handle edge cases")
    void testIsInRegionComplexPolygon() {
        List<Position> pentagon = Arrays.asList(
                new Position(0.0, 2.0),
                new Position(2.0, 0.0),
                new Position(3.0, 2.0),
                new Position(1.5, 4.0),
                new Position(-0.5, 3.0),
                new Position(0.0, 2.0)
        );

        Position center = new Position(1.5, 2.0);
        assertTrue(ilpService.isInRegion(center, pentagon));
    }

    @Test
    @DisplayName("IsInRegion with point on corner vertex")
    void testIsInRegionPointOnCorner() {
        Position corner = new Position(-3.192473, 55.946233);
        List<Position> vertices = Arrays.asList(
                new Position(-3.192473, 55.946233),
                new Position(-3.192473, 55.942617),
                new Position(-3.184319, 55.942617),
                new Position(-3.184319, 55.946233),
                new Position(-3.192473, 55.946233)
        );

        assertTrue(ilpService.isInRegion(corner, vertices));
    }

    @Test
    @DisplayName("IsInRegion with minimum closed polygon (triangle)")
    void testIsInRegionMinimumTriangle() {
        Position pointInside = new Position(1.0, 0.5);

        List<Position> triangle = Arrays.asList(
                new Position(0.0, 0.0),
                new Position(2.0, 0.0),
                new Position(1.0, 1.0),
                new Position(0.0, 0.0)
        );

        assertTrue(ilpService.isInRegion(pointInside, triangle));
    }

    @Test
    @DisplayName("IsInRegion with concave polygon")
    void testIsInRegionConcavePolygon() {
        List<Position> lShape = Arrays.asList(
                new Position(0.0, 0.0),
                new Position(2.0, 0.0),
                new Position(2.0, 1.0),
                new Position(1.0, 1.0),
                new Position(1.0, 2.0),
                new Position(0.0, 2.0),
                new Position(0.0, 0.0)
        );

        Position insideCorner = new Position(0.5, 0.5);
        Position outsideCorner = new Position(1.5, 1.5);

        assertTrue(ilpService.isInRegion(insideCorner, lShape));
        assertFalse(ilpService.isInRegion(outsideCorner, lShape));
    }

    @Test
    @DisplayName("IsInRegion with very thin polygon")
    void testIsInRegionThinPolygon() {
        List<Position> thinRect = Arrays.asList(
                new Position(0.0, 0.0),
                new Position(0.00015, 0.0),
                new Position(0.00015, 1.0),
                new Position(0.0, 1.0),
                new Position(0.0, 0.0)
        );

        Position inside = new Position(0.000075, 0.5);
        assertTrue(ilpService.isInRegion(inside, thinRect));
    }

    @Test
    @DisplayName("IsInRegion with point very close to border")
    void testIsInRegionVeryCloseToBorder() {
        List<Position> rect = Arrays.asList(
                new Position(0.0, 0.0),
                new Position(1.0, 0.0),
                new Position(1.0, 1.0),
                new Position(0.0, 1.0),
                new Position(0.0, 0.0)
        );

        Position almostOnBorder = new Position(0.0000000001, 0.5);
        assertTrue(ilpService.isInRegion(almostOnBorder, rect));
    }

    // ==================== NextPosition Tests ====================

    @Test
    @DisplayName("NextPosition should move East (0 degrees)")
    void testNextPositionEast() {
        Position start = new Position(-3.192473, 55.946233);
        Position next = ilpService.nextPosition(start, 0);

        assertEquals(start.lng() + 0.00015, next.lng(), 1e-10);
        assertEquals(start.lat(), next.lat(), 1e-10);
    }

    @Test
    @DisplayName("NextPosition should move North (90 degrees)")
    void testNextPositionNorth() {
        Position start = new Position(-3.192473, 55.946233);
        Position next = ilpService.nextPosition(start, 90);

        assertEquals(start.lng(), next.lng(), 1e-10);
        assertEquals(start.lat() + 0.00015, next.lat(), 1e-10);
    }

    @Test
    @DisplayName("NextPosition should move West (180 degrees)")
    void testNextPositionWest() {
        Position start = new Position(-3.192473, 55.946233);
        Position next = ilpService.nextPosition(start, 180);

        assertEquals(start.lng() - 0.00015, next.lng(), 1e-10);
        assertEquals(start.lat(), next.lat(), 1e-10);
    }

    @Test
    @DisplayName("NextPosition should move South (270 degrees)")
    void testNextPositionSouth() {
        Position start = new Position(-3.192473, 55.946233);
        Position next = ilpService.nextPosition(start, 270);

        assertEquals(start.lng(), next.lng(), 1e-10);
        assertEquals(start.lat() - 0.00015, next.lat(), 1e-10);
    }

    @Test
    @DisplayName("NextPosition should move Northeast (45 degrees)")
    void testNextPositionNortheast() {
        Position start = new Position(0.0, 0.0);
        Position next = ilpService.nextPosition(start, 45);

        double expected = 0.00015 / Math.sqrt(2);
        assertEquals(expected, next.lng(), 1e-10);
        assertEquals(expected, next.lat(), 1e-10);
    }

    @Test
    @DisplayName("NextPosition all secondary compass directions")
    void testNextPositionSecondaryDirections() {
        Position start = new Position(0.0, 0.0);

        Position ne = ilpService.nextPosition(start, 45);
        Position nw = ilpService.nextPosition(start, 135);
        Position sw = ilpService.nextPosition(start, 225);
        Position se = ilpService.nextPosition(start, 315);

        double expected = 0.00015 / Math.sqrt(2);

        assertEquals(expected, ne.lng(), 1e-10);
        assertEquals(expected, ne.lat(), 1e-10);

        assertEquals(-expected, nw.lng(), 1e-10);
        assertEquals(expected, nw.lat(), 1e-10);

        assertEquals(-expected, sw.lng(), 1e-10);
        assertEquals(-expected, sw.lat(), 1e-10);

        assertEquals(expected, se.lng(), 1e-10);
        assertEquals(-expected, se.lat(), 1e-10);
    }

    @Test
    @DisplayName("NextPosition all tertiary compass directions")
    void testNextPositionTertiaryDirections() {
        Position start = new Position(0.0, 0.0);

        double[] angles = {22.5, 67.5, 112.5, 157.5, 202.5, 247.5, 292.5, 337.5};

        for (double angle : angles) {
            Position next = ilpService.nextPosition(start, angle);
            double dist = ilpService.distance(start, next);

            assertEquals(0.00015, dist, 1e-10);
        }
    }

    @Test
    @DisplayName("NextPosition from negative coordinates")
    void testNextPositionFromNegativeCoordinates() {
        Position start = new Position(-3.192473, 55.946233);
        Position next = ilpService.nextPosition(start, 90);

        assertEquals(-3.192473, next.lng(), 1e-10);
        assertEquals(55.946233 + 0.00015, next.lat(), 1e-10);
    }

    @Test
    @DisplayName("NextPosition move distance should always be 0.00015")
    void testNextPositionMoveDistanceConstant() {
        Position start = new Position(-3.192473, 55.946233);
        double[] allAngles = {0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5,
                180, 202.5, 225, 247.5, 270, 292.5, 315, 337.5};

        for (double angle : allAngles) {
            Position next = ilpService.nextPosition(start, angle);
            double distance = ilpService.distance(start, next);

            assertEquals(0.00015, distance, 1e-10,
                    "Move at angle " + angle + " should be exactly 0.00015 degrees");
        }
    }
}