package ilp_submission_3.ilp_submission_image.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QueryAttribute(
        @JsonProperty("attribute") String attribute,
        @JsonProperty("operator") String operator,
        @JsonProperty("value") String value
) {}
