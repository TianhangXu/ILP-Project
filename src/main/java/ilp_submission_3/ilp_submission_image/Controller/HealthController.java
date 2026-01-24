package ilp_submission_3.ilp_submission_image.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller providing health check endpoint for the ILP service.
 * This endpoint is used to verify the service is running correctly.
 */
@RestController
public class HealthController {

    /**
     * Health check endpoint that returns the current status of the service.
     *
     * @return ResponseEntity containing a map with the service status "UP"
     */
    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
