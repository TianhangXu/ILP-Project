package ilp_submission_3.ilp_submission_image.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


public record DroneForServicePoint(
        @JsonProperty("servicePointId") Integer servicePointId,
        @JsonProperty("drones") List<DroneAvailability> drones
) {
    public record DroneAvailability(
            @JsonProperty("id") String id,
            @JsonProperty("availability") List<Availability> availability
    ) {
        public record Availability(
                @JsonProperty("dayOfWeek") String dayOfWeek,
                @JsonProperty("from") String from,
                @JsonProperty("until") String until
        ) {}
    }
}
