package ilp_submission_3.ilp_submission_image.WebSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import ilp_submission_3.ilp_submission_image.dto.PathfindingProgress;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class PathfindingProgressHandler extends TextWebSocketHandler {

    private  final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("✅ New WebSocket connection: " + session.getId());
        System.out.println("📊 Total active connections: " + sessions.size());

        // Send initial connection confirmation
        PathfindingProgress welcomeMsg = new PathfindingProgress(
                "connection_established",
                null,
                null,
                null,
                null,
                null,
                "WebSocket connection established"
        );
        sendToSession(session, welcomeMsg);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("❌ WebSocket connection closed: " + session.getId());
        System.out.println("📊 Total active connections: " + sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("⚠️ WebSocket error for session " + session.getId() + ": " + exception.getMessage());
        sessions.remove(session);
    }

    public void broadcastProgress(PathfindingProgress progress) {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(progress);
            TextMessage message = new TextMessage(json);

            // Send to all active sessions
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(message);
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to send message to session " + session.getId());
                        sessions.remove(session);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error broadcasting progress: " + e.getMessage());
        }
    }

    private void sendToSession(WebSocketSession session, PathfindingProgress progress) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(progress);
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to send message to session " + session.getId());
        }
    }

    public int getActiveConnectionCount() {
        return sessions.size();
    }

    public boolean hasActiveConnections() {
        return !sessions.isEmpty();
    }
}
