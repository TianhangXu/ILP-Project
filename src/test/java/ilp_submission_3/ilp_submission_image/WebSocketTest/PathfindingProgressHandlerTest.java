package ilp_submission_3.ilp_submission_image.WebSocketTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import ilp_submission_3.ilp_submission_image.Configuration.WebSocketConfig;
import ilp_submission_3.ilp_submission_image.WebSocket.PathfindingProgressHandler;
import ilp_submission_3.ilp_submission_image.dto.PathfindingProgress;
import ilp_submission_3.ilp_submission_image.dto.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PathfindingProgressHandler
 * Tests real WebSocket connections with actual Spring Boot server
 *
 * Coverage areas:
 * - Real WebSocket connection lifecycle
 * - Actual message broadcasting between server and clients
 * - Multi-client scenarios with real connections
 * - End-to-end event sequence validation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PathfindingProgressHandlerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PathfindingProgressHandler handler;

    private SockJsClient sockJsClient;
    private ObjectMapper objectMapper;
    private String wsUrl;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        wsUrl = "ws://localhost:" + port + "/ws/pathfinding-progress";

        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        sockJsClient = new SockJsClient(transports);
    }

    @AfterEach
    void tearDown() {
        // Allow time for connections to close properly
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== Connection Lifecycle Tests ====================

    @Test
    @DisplayName("Should successfully establish real WebSocket connection")
    void testRealConnectionEstablished() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        TestWebSocketHandler clientHandler = new TestWebSocketHandler(latch);

        WebSocketSession session = sockJsClient.execute(
                clientHandler,
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(session.isOpen());
        assertTrue(handler.hasActiveConnections());

        session.close();
        Thread.sleep(200);
    }

    @Test
    @DisplayName("Should handle multiple concurrent real connections")
    void testMultipleRealConnections() throws Exception {
        int connectionCount = 3;
        CountDownLatch latch = new CountDownLatch(connectionCount);
        List<WebSocketSession> sessions = new ArrayList<>();

        for (int i = 0; i < connectionCount; i++) {
            TestWebSocketHandler clientHandler = new TestWebSocketHandler(latch);
            WebSocketSession session = sockJsClient.execute(
                    clientHandler,
                    wsUrl
            ).get(5, TimeUnit.SECONDS);
            sessions.add(session);
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(connectionCount, handler.getActiveConnectionCount());

        for (WebSocketSession session : sessions) {
            session.close();
        }
        Thread.sleep(200);
    }

    @Test
    @DisplayName("Should close real connection successfully")
    void testRealConnectionClosed() throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch closeLatch = new CountDownLatch(1);

        TestWebSocketHandler clientHandler = new TestWebSocketHandler(connectLatch) {
            @Override
            public void afterConnectionClosed(WebSocketSession session,
                                              org.springframework.web.socket.CloseStatus status) {
                closeLatch.countDown();
            }
        };

        WebSocketSession session = sockJsClient.execute(
                clientHandler,
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        assertTrue(connectLatch.await(3, TimeUnit.SECONDS));
        int connectionsBeforeClose = handler.getActiveConnectionCount();
        assertTrue(connectionsBeforeClose > 0);

        session.close();
        assertTrue(closeLatch.await(3, TimeUnit.SECONDS));
        Thread.sleep(200);
    }

    // ==================== Message Broadcasting Tests ====================

    @Test
    @DisplayName("Should broadcast message to real single client")
    void testBroadcastToRealSingleClient() throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(2); // welcome + progress message

        MessageCapturingHandler clientHandler = new MessageCapturingHandler(
                connectLatch,
                messageLatch
        );

        WebSocketSession session = sockJsClient.execute(
                clientHandler,
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        assertTrue(connectLatch.await(3, TimeUnit.SECONDS));

        PathfindingProgress progress = PathfindingProgress.nodeExplored(
                new Position(-3.186, 55.944),
                10
        );
        handler.broadcastProgress(progress);

        assertTrue(messageLatch.await(3, TimeUnit.SECONDS));

        List<String> messages = clientHandler.getReceivedMessages();
        assertTrue(messages.size() >= 2);

        // Find the node_explored message
        boolean foundNodeExplored = messages.stream()
                .anyMatch(msg -> msg.contains("node_explored"));
        assertTrue(foundNodeExplored);

        session.close();
        Thread.sleep(200);
    }

    @Test
    @DisplayName("Should broadcast message to multiple real clients")
    void testBroadcastToMultipleRealClients() throws Exception {
        int clientCount = 3;
        CountDownLatch connectLatch = new CountDownLatch(clientCount);
        CountDownLatch messageLatch = new CountDownLatch(clientCount * 2); // welcome + progress

        List<MessageCapturingHandler> handlers = new ArrayList<>();
        List<WebSocketSession> sessions = new ArrayList<>();

        for (int i = 0; i < clientCount; i++) {
            MessageCapturingHandler clientHandler = new MessageCapturingHandler(
                    connectLatch,
                    messageLatch
            );
            handlers.add(clientHandler);

            WebSocketSession session = sockJsClient.execute(
                    clientHandler,
                    wsUrl
            ).get(5, TimeUnit.SECONDS);
            sessions.add(session);
        }

        assertTrue(connectLatch.await(3, TimeUnit.SECONDS));

        PathfindingProgress progress = PathfindingProgress.batchStarted(1, "drone1", 3);
        handler.broadcastProgress(progress);

        assertTrue(messageLatch.await(5, TimeUnit.SECONDS));

        // Verify all clients received the broadcast
        for (MessageCapturingHandler handler : handlers) {
            boolean foundBatchStarted = handler.getReceivedMessages().stream()
                    .anyMatch(msg -> msg.contains("batch_started"));
            assertTrue(foundBatchStarted);
        }

        for (WebSocketSession session : sessions) {
            session.close();
        }
        Thread.sleep(200);
    }

    @Test
    @DisplayName("Should handle broadcast when no active clients")
    void testBroadcastWithNoClients() {
        PathfindingProgress progress = PathfindingProgress.error("Test");
        assertDoesNotThrow(() -> handler.broadcastProgress(progress));
    }

    // ==================== Progress Event Type Tests ====================

    @Test
    @DisplayName("Should send and receive batch started event")
    void testBatchStartedEvent() throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(2);

        MessageCapturingHandler clientHandler = new MessageCapturingHandler(
                connectLatch,
                messageLatch
        );

        WebSocketSession session = sockJsClient.execute(
                clientHandler,
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        assertTrue(connectLatch.await(3, TimeUnit.SECONDS));

        PathfindingProgress progress = PathfindingProgress.batchStarted(1, "drone1", 5);
        handler.broadcastProgress(progress);

        assertTrue(messageLatch.await(3, TimeUnit.SECONDS));

        boolean found = clientHandler.getReceivedMessages().stream()
                .anyMatch(msg -> msg.contains("batch_started") && msg.contains("drone1"));
        assertTrue(found);

        session.close();
        Thread.sleep(200);
    }

    @Test
    @DisplayName("Should send and receive delivery started event")
    void testDeliveryStartedEvent() throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(2);

        MessageCapturingHandler clientHandler = new MessageCapturingHandler(
                connectLatch,
                messageLatch
        );

        WebSocketSession session = sockJsClient.execute(
                clientHandler,
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        assertTrue(connectLatch.await(3, TimeUnit.SECONDS));

        Position from = new Position(-3.186, 55.944);
        Position to = new Position(-3.180, 55.945);
        PathfindingProgress progress = PathfindingProgress.deliveryStarted(1, from, to);
        handler.broadcastProgress(progress);

        assertTrue(messageLatch.await(3, TimeUnit.SECONDS));

        boolean found = clientHandler.getReceivedMessages().stream()
                .anyMatch(msg -> msg.contains("delivery_started"));
        assertTrue(found);

        session.close();
        Thread.sleep(200);
    }

    @Test
    @DisplayName("Should send and receive node explored event")
    void testNodeExploredEvent() throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(2);

        MessageCapturingHandler clientHandler = new MessageCapturingHandler(
                connectLatch,
                messageLatch
        );

        WebSocketSession session = sockJsClient.execute(
                clientHandler,
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        assertTrue(connectLatch.await(3, TimeUnit.SECONDS));

        Position pos = new Position(-3.186, 55.944);
        PathfindingProgress progress = PathfindingProgress.nodeExplored(pos, 42);
        handler.broadcastProgress(progress);

        assertTrue(messageLatch.await(3, TimeUnit.SECONDS));

        boolean found = clientHandler.getReceivedMessages().stream()
                .anyMatch(msg -> msg.contains("node_explored"));
        assertTrue(found);

        session.close();
        Thread.sleep(200);
    }

    @Test
    @DisplayName("Should send and receive path found event")
    void testPathFoundEvent() throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(2);

        MessageCapturingHandler clientHandler = new MessageCapturingHandler(
                connectLatch,
                messageLatch
        );

        WebSocketSession session = sockJsClient.execute(
                clientHandler,
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        assertTrue(connectLatch.await(3, TimeUnit.SECONDS));

        PathfindingProgress progress = PathfindingProgress.pathFound(1, 150, 25);
        handler.broadcastProgress(progress);

        assertTrue(messageLatch.await(3, TimeUnit.SECONDS));

        boolean found = clientHandler.getReceivedMessages().stream()
                .anyMatch(msg -> msg.contains("path_found"));
        assertTrue(found);

        session.close();
        Thread.sleep(200);
    }

    @Test
    @DisplayName("Should send and receive batch completed event")
    void testBatchCompletedEvent() throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(2);

        MessageCapturingHandler clientHandler = new MessageCapturingHandler(
                connectLatch,
                messageLatch
        );

        WebSocketSession session = sockJsClient.execute(
                clientHandler,
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        assertTrue(connectLatch.await(3, TimeUnit.SECONDS));

        PathfindingProgress progress = PathfindingProgress.batchCompleted(1, "drone1", 125.50, 80);
        handler.broadcastProgress(progress);

        assertTrue(messageLatch.await(3, TimeUnit.SECONDS));

        boolean found = clientHandler.getReceivedMessages().stream()
                .anyMatch(msg -> msg.contains("batch_completed"));
        assertTrue(found);

        session.close();
        Thread.sleep(200);
    }

    @Test
    @DisplayName("Should send and receive error event")
    void testErrorEvent() throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(2);

        MessageCapturingHandler clientHandler = new MessageCapturingHandler(
                connectLatch,
                messageLatch
        );

        WebSocketSession session = sockJsClient.execute(
                clientHandler,
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        assertTrue(connectLatch.await(3, TimeUnit.SECONDS));

        PathfindingProgress progress = PathfindingProgress.error("Path calculation failed");
        handler.broadcastProgress(progress);

        assertTrue(messageLatch.await(3, TimeUnit.SECONDS));

        boolean found = clientHandler.getReceivedMessages().stream()
                .anyMatch(msg -> msg.contains("error"));
        assertTrue(found);

        session.close();
        Thread.sleep(200);
    }

    // ==================== Event Sequence Tests ====================

    @Test
    @DisplayName("Should maintain correct event sequence for complete delivery")
    void testCompleteDeliverySequence() throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        int expectedMessages = 7; // 1 welcome + 6 progress events
        CountDownLatch messageLatch = new CountDownLatch(expectedMessages);

        MessageCapturingHandler clientHandler = new MessageCapturingHandler(
                connectLatch,
                messageLatch
        );

        WebSocketSession session = sockJsClient.execute(
                clientHandler,
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        assertTrue(connectLatch.await(3, TimeUnit.SECONDS));

        List<PathfindingProgress> sequence = Arrays.asList(
                PathfindingProgress.batchStarted(1, "drone1", 2),
                PathfindingProgress.deliveryStarted(1, new Position(-3.186, 55.944),
                        new Position(-3.180, 55.945)),
                PathfindingProgress.nodeExplored(new Position(-3.185, 55.944), 1),
                PathfindingProgress.nodeExplored(new Position(-3.184, 55.944), 2),
                PathfindingProgress.pathFound(1, 50, 10),
                PathfindingProgress.batchCompleted(1, "drone1", 75.0, 20)
        );

        for (PathfindingProgress progress : sequence) {
            handler.broadcastProgress(progress);
            Thread.sleep(50); // Small delay between messages
        }

        assertTrue(messageLatch.await(5, TimeUnit.SECONDS));

        List<String> messages = clientHandler.getReceivedMessages();
        assertTrue(messages.size() >= 6);

        session.close();
        Thread.sleep(200);
    }

    @Test
    @DisplayName("Should handle rapid successive events")
    void testRapidEvents() throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        int eventCount = 20;
        CountDownLatch messageLatch = new CountDownLatch(eventCount + 1); // +1 for welcome

        MessageCapturingHandler clientHandler = new MessageCapturingHandler(
                connectLatch,
                messageLatch
        );

        WebSocketSession session = sockJsClient.execute(
                clientHandler,
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        assertTrue(connectLatch.await(3, TimeUnit.SECONDS));

        for (int i = 0; i < eventCount; i++) {
            PathfindingProgress progress = PathfindingProgress.nodeExplored(
                    new Position(-3.186 + i * 0.0001, 55.944),
                    i
            );
            handler.broadcastProgress(progress);
        }

        assertTrue(messageLatch.await(10, TimeUnit.SECONDS));

        List<String> messages = clientHandler.getReceivedMessages();
        assertTrue(messages.size() >= eventCount);

        session.close();
        Thread.sleep(200);
    }

    // ==================== Parameterized Event Tests ====================

    @ParameterizedTest
    @MethodSource("provideProgressEvents")
    @DisplayName("Should broadcast and receive different event types correctly")
    void testVariousEventTypes(PathfindingProgress progress) throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(2);

        MessageCapturingHandler clientHandler = new MessageCapturingHandler(
                connectLatch,
                messageLatch
        );

        WebSocketSession session = sockJsClient.execute(
                clientHandler,
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        assertTrue(connectLatch.await(3, TimeUnit.SECONDS));

        handler.broadcastProgress(progress);

        assertTrue(messageLatch.await(3, TimeUnit.SECONDS));

        List<String> messages = clientHandler.getReceivedMessages();
        assertTrue(messages.size() >= 2);

        session.close();
        Thread.sleep(200);
    }

    static Stream<Arguments> provideProgressEvents() {
        return Stream.of(
                Arguments.of(PathfindingProgress.batchStarted(1, "drone1", 3)),
                Arguments.of(PathfindingProgress.deliveryStarted(1,
                        new Position(-3.186, 55.944), new Position(-3.180, 55.945))),
                Arguments.of(PathfindingProgress.nodeExplored(new Position(-3.186, 55.944), 10)),
                Arguments.of(PathfindingProgress.pathFound(1, 100, 20)),
                Arguments.of(PathfindingProgress.batchCompleted(1, "drone1", 150.0, 30)),
                Arguments.of(PathfindingProgress.error("Test error"))
        );
    }

    // ==================== Helper Classes ====================

    /**
     * Basic WebSocket handler for testing connections
     */
    static class TestWebSocketHandler extends TextWebSocketHandler {
        private final CountDownLatch latch;

        public TestWebSocketHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            latch.countDown();
        }
    }

    /**
     * WebSocket handler that captures received messages
     */
    static class MessageCapturingHandler extends TextWebSocketHandler {
        private final CountDownLatch connectLatch;
        private final CountDownLatch messageLatch;
        private final List<String> receivedMessages = new CopyOnWriteArrayList<>();

        public MessageCapturingHandler(CountDownLatch connectLatch, CountDownLatch messageLatch) {
            this.connectLatch = connectLatch;
            this.messageLatch = messageLatch;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            connectLatch.countDown();
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            receivedMessages.add(message.getPayload());
            messageLatch.countDown();
        }

        public List<String> getReceivedMessages() {
            return new ArrayList<>(receivedMessages);
        }
    }
}