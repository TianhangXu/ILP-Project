package ilp_submission_3.ilp_submission_image.Service;

import ilp_submission_3.ilp_submission_image.dto.Position;
import java.util.List;

/**
 * Service interface for geometric calculations in the ILP REST service.
 * Provides methods for distance calculations, proximity checks, position movements,
 * and region containment detection.
 */
public interface ILPServiceInterface {

    /**
     * Calculates the Euclidean distance between two positions.
     *
     * @param p1 the first position
     * @param p2 the second position
     * @return the Euclidean distance between the two positions
     */
    double distance(Position p1, Position p2);

    /**
     * Determines if two positions are close to each other.
     * Positions are considered close if the distance between them is less than 0.00015.
     *
     * @param p1 the first position
     * @param p2 the second position
     * @return true if the positions are close, false otherwise
     */
    boolean isClose(Position p1, Position p2);

    /**
     * Calculates the next position from a starting position given a compass angle.
     * Moves 0.00015 units in the direction specified by the angle.
     *
     * @param start the starting position
     * @param angle the compass angle in degrees (must be one of 16 valid directions)
     * @return the next position after moving in the specified direction
     */
    Position nextPosition(Position start, double angle);

    /**
     * Determines if a point is inside or on the border of a polygon region.
     * Uses ray casting algorithm for interior detection and checks if the point lies on any edge.
     *
     * @param point the point to check
     * @param vertices the vertices of the polygon (first and last vertices should be the same for a closed polygon)
     * @return true if the point is inside the polygon or on its border, false otherwise
     */
    boolean isInRegion(Position point, List<Position> vertices);
}