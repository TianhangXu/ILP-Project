package ilp_submission_3.ilp_submission_image.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DroneServicePoint(
        @JsonProperty("name") String name,
        @JsonProperty("id") Integer id,
        @JsonProperty("location") LngLatAlt location
) {
    public static record LngLatAlt(
            @JsonProperty("lng") Double lng,
            @JsonProperty("lat") Double lat,
            @JsonProperty("alt") Double alt
    ) {}
}

