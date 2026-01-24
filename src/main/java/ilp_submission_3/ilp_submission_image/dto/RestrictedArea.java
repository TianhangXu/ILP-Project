package ilp_submission_3.ilp_submission_image.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ilp_submission_3.ilp_submission_image.dto.Position;

import java.util.List;

public record RestrictedArea(
        @JsonProperty("name") String name,
        @JsonProperty("id") Integer id,
        @JsonProperty("limits") Limits limits,
        @JsonProperty("vertices") List<Position> vertices
) {
    public record Limits(
            @JsonProperty("lower") Double lower,
            @JsonProperty("upper") Double upper
    ) {}
}

