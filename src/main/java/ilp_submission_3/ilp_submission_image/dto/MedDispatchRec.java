package ilp_submission_3.ilp_submission_image.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MedDispatchRec(
        @JsonProperty("id") Integer id,
        @JsonProperty("date") String date,
        @JsonProperty("time") String time,
        @JsonProperty("requirements") Requirements requirements,
        @JsonProperty("delivery") Position delivery

) {
    public record Requirements(
            @JsonProperty("capacity") Double capacity,
            @JsonProperty("cooling") Boolean cooling,
            @JsonProperty("heating") Boolean heating,
            @JsonProperty("maxCost") Double maxCost
    ) {}
}

