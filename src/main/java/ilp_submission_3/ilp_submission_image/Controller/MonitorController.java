package ilp_submission_3.ilp_submission_image.Controller;

import ilp_submission_3.ilp_submission_image.WebSocket.PathfindingProgressHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for monitoring WebSocket connections and system status.
 */
@RestController
@RequestMapping("/api/v1/monitor")
public class MonitorController {

    private final PathfindingProgressHandler progressHandler;

    public MonitorController(PathfindingProgressHandler progressHandler) {
        this.progressHandler = progressHandler;
    }

    /**
     * Get current WebSocket connection statistics.
     *
     * @return Map containing connection count and status
     */
    @GetMapping("/websocket-status")
    public ResponseEntity<Map<String, Object>> getWebSocketStatus() {
        int activeConnections = progressHandler.getActiveConnectionCount();
        boolean hasConnections = progressHandler.hasActiveConnections();

        return ResponseEntity.ok(Map.of(
                "activeConnections", activeConnections,
                "hasConnections", hasConnections,
                "status", hasConnections ? "active" : "idle"
        ));
    }
}
