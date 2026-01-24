package ilp_submission_3.ilp_submission_image.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Drone(
        @JsonProperty("name") String name,

        @JsonProperty("id") String id,
        @JsonProperty("capability") Capability capability
) {
    public record Capability(
            @JsonProperty("cooling") Boolean cooling,
            @JsonProperty("heating") Boolean heating,
            @JsonProperty("capacity") Double capacity,
            @JsonProperty("maxMoves") Integer maxMoves,
            @JsonProperty("costPerMove") Double costPerMove,
            @JsonProperty("costInitial") Double costInitial,
            @JsonProperty("costFinal") Double costFinal
    ) {}
}
