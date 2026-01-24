package ilp_submission_3.ilp_submission_image.Service;

import ilp_submission_3.ilp_submission_image.dto.Position;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Implementation of ILPServiceInterface providing geometric calculations for the ILP REST service.
 * Handles distance calculations, proximity checks, position movements, and region containment.
 */
@Service
public class ILPServiceImpl implements ILPServiceInterface {
    private static final BigDecimal MOVE_STEP = new BigDecimal("0.00015");

    @Override
    public double distance(Position p1, Position p2) {
        // Convert positions to BigDecimal for precise arithmetic
        BigDecimal lng1 = BigDecimal.valueOf(p1.lng());
        BigDecimal lat1 = BigDecimal.valueOf(p1.lat());
        BigDecimal lng2 = BigDecimal.valueOf(p2.lng());
        BigDecimal lat2 = BigDecimal.valueOf(p2.lat());

        // Calculate coordinate differences
        BigDecimal dx = lng1.subtract(lng2);
        BigDecimal dy = lat1.subtract(lat2);

        // Square the differences
        BigDecimal dxSquared = dx.multiply(dx);
        BigDecimal dySquared = dy.multiply(dy);

        // Sum of squares
        BigDecimal sumSquares = dxSquared.add(dySquared);

        // Take square root and return as double
        return Math.sqrt(sumSquares.doubleValue());
    }

    @Override
    public boolean isClose(Position p1, Position p2) {
        return distance(p1, p2) < 0.00015;
    }

    @Override
    public Position nextPosition(Position start, double angle) {
        BigDecimal startLng = BigDecimal.valueOf(start.lng());
        BigDecimal startLat = BigDecimal.valueOf(start.lat());

        // Convert angle to radians for trigonometric functions
        // Uses mathematical angle system: 0° = East, 90° = North, 180° = West, 270° = South (counterclockwise)
        double rad = Math.toRadians(angle);

        // Calculate movement components using trigonometry
        BigDecimal cos = BigDecimal.valueOf(Math.cos(rad));
        BigDecimal sin = BigDecimal.valueOf(Math.sin(rad));

        // Calculate deltas (how much to move in each direction)
        BigDecimal deltaLng = MOVE_STEP.multiply(cos);
        BigDecimal deltaLat = MOVE_STEP.multiply(sin);

        // Calculate new position
        BigDecimal newLng = startLng.add(deltaLng);
        BigDecimal newLat = startLat.add(deltaLat);

        return new Position(newLng.doubleValue(), newLat.doubleValue());
    }

    @Override
    public boolean isInRegion(Position point, List<Position> vertices) {
        BigDecimal pointLng = BigDecimal.valueOf(point.lng());
        BigDecimal pointLat = BigDecimal.valueOf(point.lat());

        // Phase 1: Border detection
        // Check if point lies on any edge of the polygon
        for (int i = 0; i < vertices.size() - 1; i++) {
            Position v1 = vertices.get(i);
            Position v2 = vertices.get(i + 1);

            if (isPointOnSegment(point, v1, v2)) {
                return true;
            }
        }

        // Phase 2: Interior detection using ray casting algorithm
        // Cast a horizontal ray from the point to the right (positive x direction)
        // and count how many polygon edges it crosses
        boolean inside = false;
        int j = vertices.size() - 1;

        for (int i = 0; i < vertices.size(); i++) {
            Position vi = vertices.get(i);
            Position vj = vertices.get(j);


            BigDecimal viLng = BigDecimal.valueOf(vi.lng());
            BigDecimal viLat = BigDecimal.valueOf(vi.lat());
            BigDecimal vjLng = BigDecimal.valueOf(vj.lng());
            BigDecimal vjLat = BigDecimal.valueOf(vj.lat());


            // Check if the edge between vj and vi straddles the point's latitude
            // (one endpoint above, one below - this is when a horizontal ray can cross)
            boolean latCondition = (viLat.compareTo(pointLat) > 0) != (vjLat.compareTo(pointLat) > 0);

            if (latCondition) {
                BigDecimal lngDiff = vjLng.subtract(viLng);
                BigDecimal latDiff = vjLat.subtract(viLat);
                BigDecimal pointLatDiff = pointLat.subtract(viLat);

                if (latDiff.compareTo(BigDecimal.ZERO) != 0) {
                    // Calculate x-coordinate where edge intersects the horizontal ray
                    // Formula: intersectionX = viLng + (lngDiff × pointLatDiff / latDiff)
                    BigDecimal intersectionX = lngDiff
                            .multiply(pointLatDiff)
                            .divide(latDiff, 15, RoundingMode.HALF_UP)
                            .add(viLng);

                    if (pointLng.compareTo(intersectionX) < 0) {
                        inside = !inside;
                    }
                }
            }
            j = i;
        }
        return inside;
    }

    /**
     * Checks if a point lies on a line segment between two vertices.
     *
     * @param point the point to check
     * @param v1 first vertex of the segment
     * @param v2 second vertex of the segment
     * @return true if point is on the segment, false otherwise
     */
    private boolean isPointOnSegment(Position point, Position v1, Position v2) {
        BigDecimal px = BigDecimal.valueOf(point.lng());
        BigDecimal py = BigDecimal.valueOf(point.lat());
        BigDecimal v1x = BigDecimal.valueOf(v1.lng());
        BigDecimal v1y = BigDecimal.valueOf(v1.lat());
        BigDecimal v2x = BigDecimal.valueOf(v2.lng());
        BigDecimal v2y = BigDecimal.valueOf(v2.lat());

        // Step 1: Bounding box check (quick rejection test)
        // Point must be within the rectangle defined by segment endpoints
        BigDecimal minX = v1x.min(v2x);
        BigDecimal maxX = v1x.max(v2x);
        BigDecimal minY = v1y.min(v2y);
        BigDecimal maxY = v1y.max(v2y);

        if (px.compareTo(minX) < 0 || px.compareTo(maxX) > 0 ||
                py.compareTo(minY) < 0 || py.compareTo(maxY) > 0) {
            return false;
        }

        // Step 2: Collinearity check using cross product
        // Calculate vectors
        BigDecimal dx1 = px.subtract(v1x);
        BigDecimal dy1 = py.subtract(v1y);
        BigDecimal dx2 = v2x.subtract(v1x);
        BigDecimal dy2 = v2y.subtract(v1y);

        // Cross product: if zero, vectors are parallel (point is on the line)
        // crossProduct = dx1 × dy2 - dy1 × dx2
        BigDecimal crossProduct = dx1.multiply(dy2).subtract(dy1.multiply(dx2));

        // Check if cross product is essentially zero (within tolerance)
        BigDecimal tolerance = new BigDecimal("1E-9");
        return crossProduct.abs().compareTo(tolerance) < 0;
    }
}
