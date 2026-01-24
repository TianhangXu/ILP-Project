package ilp_submission_3.ilp_submission_image.dto;

import java.util.List;

/**
 * Request DTO for checking if a position is inside or on the border of a polygon region.
 *
 * @param position the position to check
 * @param region the polygon region to check against
 */
public record IsInRegionRequest(
        Position position,
        Region region
) {

    /**
     * Represents a named polygon region with vertices.
     *
     * @param name the name of the region
     * @param vertices the list of vertices defining the polygon (first and last must be the same for a closed polygon)
     */
    public record Region(String name, List<Position> vertices) {}
}

