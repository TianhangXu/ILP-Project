package ilp_submission_3.ilp_submission_image.Configuration;

import org.springframework.stereotype.Component;

@Component
public class ILPEndpointProvider {

    private static final String DEFAULT_ENDPOINT =
            "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net";

    public String getEndpoint() {
        String endpoint = System.getenv("ILP_ENDPOINT");
        if (endpoint == null || endpoint.isEmpty()) {
            return DEFAULT_ENDPOINT;
        }
        return endpoint;
    }
}
