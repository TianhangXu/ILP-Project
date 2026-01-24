package ilp_submission_3.ilp_submission_image.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DeliveryPathResponse(
        @JsonProperty("totalCost") Double totalCost,
        @JsonProperty("totalMoves") Integer totalMoves,
        @JsonProperty("dronePaths") List<DronePath> dronePaths
) {
    public record DronePath(
            @JsonProperty("droneId") String droneId,
            @JsonProperty("deliveries") List<Delivery> deliveries
    ) {}

    public record Delivery(
            @JsonProperty("deliveryId") Integer deliveryId,
            @JsonProperty("flightPath") List<Position> flightPath
    ) {}
}

