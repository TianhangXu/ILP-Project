package ilp_submission_3.ilp_submission_image.Controller;


import ilp_submission_3.ilp_submission_image.dto.DistanceRequest;
import ilp_submission_3.ilp_submission_image.Service.ILPServiceInterface;
import ilp_submission_3.ilp_submission_image.dto.DistanceRequest;
import ilp_submission_3.ilp_submission_image.dto.IsInRegionRequest;
import ilp_submission_3.ilp_submission_image.dto.NextPositionRequest;
import ilp_submission_3.ilp_submission_image.dto.Position;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
/**
 * REST controller providing geographic calculation endpoints for the ILP service.
 * All endpoints are prefixed with /api/v1.
 */
@RestController
@RequestMapping("/api/v1")
public class ILPController {
    private final ILPServiceInterface ilpService;

    public ILPController(ILPServiceInterface ilpService) {
        this.ilpService = ilpService;
    }

    private static final Set<Double> VALID_ANGLES = Set.of(
            0.0, 22.5, 45.0, 67.5, 90.0, 112.5, 135.0, 157.5,
            180.0, 202.5, 225.0, 247.5, 270.0, 292.5, 315.0, 337.5
    );

    /**
     * Checks if a number value is invalid.
     *
     * @param value the Double value to check
     * @return true if the value is null, NaN, or Infinity
     */
    private boolean isInValidNumber(Double value) {
        return value == null || !Double.isFinite(value);
    }

    /**
     * Checks if a position has valid longitude and latitude values.
     *
     * @param position the position to validate
     * @return true if the position is valid, false otherwise
     */
    private boolean isInValidPosition(Position position) {
        if (position == null) {
            return true;
        }

        // Check if coordinates are valid numbers
        if (isInValidNumber(position.lng()) || isInValidNumber(position.lat())) {
            return true;
        }

        // Check longitude range: -180 to +180
        if (position.lng() < -180.0 || position.lng() > 180.0) {
            return true;
        }

        // Check latitude range: -90 to +90
        if (position.lat() < -90.0 || position.lat() > 90.0) {
            return true;
        }

        return false;
    }

    /**
     * Returns the student ID.
     *
     * @return ResponseEntity containing the student ID as a string
     */
    @GetMapping("/uid")
    public ResponseEntity<String> getUid() {
        return ResponseEntity.ok("s2337850");
    }

    /**
     * Calculates the Euclidean distance between two positions.
     *
     * @param request contains position1 and position2
     * @return ResponseEntity with the distance as a double, or 400 if invalid
     */
    @PostMapping("/distanceTo")
    public ResponseEntity<Double> distance(@RequestBody DistanceRequest request) {
        // Validate both positions
        if (isInValidPosition(request.position1()) || isInValidPosition(request.position2())) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(ilpService.distance(request.position1(), request.position2()));
    }

    /**
     * Checks if two positions are close to each other (distance less than 0.00015).
     *
     * @param request contains position1 and position2
     * @return ResponseEntity with true if close, false otherwise, or 400 if invalid
     */
    @PostMapping("/isCloseTo")
    public ResponseEntity<Boolean> isClose(@RequestBody DistanceRequest request) {
        // Validate both positions
        if (isInValidPosition(request.position1()) || isInValidPosition(request.position2())) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(ilpService.isClose(request.position1(), request.position2()));
    }

    /**
     * Calculates the next position from a starting position given a compass angle.
     *
     * @param request contains start position and angle
     * @return ResponseEntity with the next position, or 400 if invalid
     */
    @PostMapping("/nextPosition")
    public ResponseEntity<Position> nextPosition(@RequestBody NextPositionRequest request) {
        // Validate start position
        if (isInValidPosition(request.start())) {
            return ResponseEntity.badRequest().build();
        }

        // Validate angle is present and is a valid number
        if (isInValidNumber(request.angle())) {
            return ResponseEntity.badRequest().build();
        }

        // Validate angle is one of the 16 valid compass directions
        if (!VALID_ANGLES.contains(request.angle())) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(ilpService.nextPosition(request.start(), request.angle()));
    }

    /**
     * Checks if a point is inside or on the border of a polygon region.
     *
     * @param request contains position and region with vertices
     * @return ResponseEntity with true if inside or on border, false otherwise, or 400 if invalid
     */
    @PostMapping("/isInRegion")
    public ResponseEntity<Boolean> isInRegion(@RequestBody IsInRegionRequest request) {
        // Validate required objects are present
        if (request.position() == null || request.region() == null ||
                request.region().vertices() == null || request.region().name() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Validate position
        if (isInValidPosition(request.position())) {
            return ResponseEntity.badRequest().build();
        }

        // Validate vertices list is not empty
        if (request.region().vertices().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Validate each vertex
        for (Position vertex : request.region().vertices()) {
            if (isInValidPosition(vertex)) {
                return ResponseEntity.badRequest().build();
            }
        }

        // Validate polygon is closed (first vertex equals last vertex)
        int vertexCount = request.region().vertices().size();
        Position firstVertex = request.region().vertices().get(0);
        Position lastVertex = request.region().vertices().get(vertexCount - 1);

        if (!firstVertex.equals(lastVertex) || vertexCount < 4) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(ilpService.isInRegion(request.position(), request.region().vertices()));
    }
}
