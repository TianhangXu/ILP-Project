package ilp_submission_3.ilp_submission_image.Configuration;

import ilp_submission_3.ilp_submission_image.WebSocket.PathfindingProgressHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PathfindingProgressHandler pathfindingProgressHandler;

    public WebSocketConfig(PathfindingProgressHandler pathfindingProgressHandler) {
        this.pathfindingProgressHandler = pathfindingProgressHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pathfindingProgressHandler, "/ws/pathfinding-progress")
                .setAllowedOrigins("http://localhost:8080", "http://localhost:3000", "http://localhost:5173" )
                .withSockJS();
    }
}
