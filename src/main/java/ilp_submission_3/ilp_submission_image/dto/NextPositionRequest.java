package ilp_submission_3.ilp_submission_image.dto;

/**
 * Request DTO for calculating the next position from a starting point and compass angle.
 *
 * @param start the starting position
 * @param angle the compass angle in degrees (must be one of 16 valid directions)
 */
public record NextPositionRequest(Position start, Double angle) {}
