package ilp_submission_3.ilp_submission_image.dto;

/**
 * Request DTO for distance and proximity calculations between two positions.
 *
 * @param position1 the first position
 * @param position2 the second position
 */
public record DistanceRequest(Position position1, Position position2) {}

