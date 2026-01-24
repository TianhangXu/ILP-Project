package ilp_submission_3.ilp_submission_image.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for pathfinding algorithm progress updates.
 * Sent via WebSocket to visualize algorithm execution in real-time.
 */
public record PathfindingProgress(
        /**
         * Type of progress event:
         * - "node_explored": A* algorithm explored a new node
         * - "path_found": Complete path found for a delivery
         * - "batch_started": Started processing a new batch of deliveries
         * - "batch_completed": Finished processing a batch
         * - "delivery_started": Started calculating path for a specific delivery
         * - "delivery_completed": Finished calculating path for a delivery
         * - "error": An error occurred during pathfinding
         */
        @JsonProperty("type") String type,

        /**
         * Current position being processed (for node_explored events)
         */
        @JsonProperty("position") Position position,

        /**
         * ID of the delivery being processed (if applicable)
         */
        @JsonProperty("deliveryId") Integer deliveryId,

        /**
         * Total number of nodes explored so far
         */
        @JsonProperty("totalNodes") Integer totalNodes,

        /**
         * Current batch number (1-indexed)
         */
        @JsonProperty("currentBatch") Integer currentBatch,

        /**
         * ID of the drone being used (if applicable)
         */
        @JsonProperty("droneId") String droneId,

        /**
         * Human-readable message describing the progress
         */
        @JsonProperty("message") String message
) {
    /**
     * Factory method for creating a node exploration event
     */
    public static PathfindingProgress nodeExplored(Position position, int totalNodes) {
        return new PathfindingProgress(
                "node_explored",
                position,
                null,
                totalNodes,
                null,
                null,
                "Exploring node at (" + position.lng() + ", " + position.lat() + ")"
        );
    }

    /**
     * Factory method for creating a path found event
     */
    public static PathfindingProgress pathFound(Integer deliveryId, int totalNodes, int pathLength) {
        return new PathfindingProgress(
                "path_found",
                null,
                deliveryId,
                totalNodes,
                null,
                null,
                "Path found for delivery " + deliveryId + " with " + pathLength + " moves"
        );
    }

    /**
     * Factory method for creating a batch started event
     */
    public static PathfindingProgress batchStarted(int batchNumber, String droneId, int deliveryCount) {
        return new PathfindingProgress(
                "batch_started",
                null,
                null,
                null,
                batchNumber,
                droneId,
                "Starting batch " + batchNumber + " with drone " + droneId +
                        " (" + deliveryCount + " deliveries)"
        );
    }

    /**
     * Factory method for creating a batch completed event
     */
    public static PathfindingProgress batchCompleted(int batchNumber, String droneId,
                                                     double cost, int moves) {
        return new PathfindingProgress(
                "batch_completed",
                null,
                null,
                null,
                batchNumber,
                droneId,
                "Completed batch " + batchNumber + " - Cost: " +
                        String.format("%.2f", cost) + ", Moves: " + moves
        );
    }

    /**
     * Factory method for creating a delivery started event
     */
    public static PathfindingProgress deliveryStarted(Integer deliveryId, Position from, Position to) {
        return new PathfindingProgress(
                "delivery_started",
                from,
                deliveryId,
                null,
                null,
                null,
                "Calculating path for delivery " + deliveryId
        );
    }

    /**
     * Factory method for creating an error event
     */
    public static PathfindingProgress error(String errorMessage) {
        return new PathfindingProgress(
                "error",
                null,
                null,
                null,
                null,
                null,
                "Error: " + errorMessage
        );
    }
}
