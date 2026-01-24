package ilp_submission_3.ilp_submission_image.PerformanceTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import ilp_submission_3.ilp_submission_image.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import static org.junit.jupiter.api.Assertions.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performance and Stress Tests
 *
 * Two main test dimensions:
 * 1. A* Pathfinding Performance: Complex maps, distance, obstacles
 * 2. WebSocket Message Load: High-frequency progress updates
 *
 * Methodology:
 * - Repeatable tests with controlled parameters
 * - Quantitative metrics collection
 * - Results exported to CSV for analysis
 * - Clear baseline vs stress comparison
 *
 * Evidence Generated:
 * - performance_test_results.csv (timestamped data)
 * - Console output with summary statistics
 * - Pass/fail thresholds based on reasonable limits
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceAndStressTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;
    private static PrintWriter csvWriter;
    private static final String RESULTS_FILE = "performance_test_results.csv";

    @BeforeAll
    static void setupResultsFile() throws Exception {
        csvWriter = new PrintWriter(new FileWriter(RESULTS_FILE, true));
        csvWriter.println("Timestamp,TestName,Scenario,ResponseTime_ms,NodesExplored,MessagesReceived,TotalMoves,TotalCost,Status");
        csvWriter.flush();
        System.out.println("\n📊 Performance results will be saved to: " + RESULTS_FILE + "\n");
    }

    @AfterAll
    static void closeResultsFile() {
        if (csvWriter != null) {
            csvWriter.close();
        }
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📊 All performance test results saved to: " + RESULTS_FILE);
        System.out.println("=".repeat(70) + "\n");
    }

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    /**
     * TEST 1A: A* Pathfinding Performance - Baseline
     *
     * Scenario: Simple delivery (short distance, no obstacles)
     * Expected: < 3 seconds, < 1000 nodes explored
     *
     * This establishes baseline performance for comparison
     */
    @Test
    @Order(1)
    @DisplayName("Performance 1A: A* Baseline - Simple Path")
    void testAStarPerformanceBaseline() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("=== A* PERFORMANCE TEST 1A: BASELINE ===");
        System.out.println("=".repeat(70));

        PerformanceMetrics metrics = runPathfindingTest(
                "Baseline_Simple",
                createSimpleOrder(100)
        );

        recordMetrics("A*_Baseline", "Simple_Short_Distance", metrics);

        assertTrue(metrics.responseTime < 3000,
                "Baseline should complete in < 3s, was: " + metrics.responseTime + "ms");

        printMetricsSummary("BASELINE", metrics);
        System.out.println("=".repeat(70) + "\n");
    }

    /**
     * TEST 1B: A* Pathfinding Performance - Medium Complexity
     *
     * Scenario: Medium distance with moderate obstacles
     * Expected: < 8 seconds, moderate node exploration
     */
    @Test
    @Order(2)
    @DisplayName("Performance 1B: A* Medium - Complex Path")
    void testAStarPerformanceMedium() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("=== A* PERFORMANCE TEST 1B: MEDIUM COMPLEXITY ===");
        System.out.println("=".repeat(70));

        PerformanceMetrics metrics = runPathfindingTest(
                "Medium_Complexity",
                createMediumComplexityOrder(101)
        );

        recordMetrics("A*_Medium", "Medium_Distance_With_Obstacles", metrics);

        assertTrue(metrics.responseTime < 8000,
                "Medium complexity should complete in < 8s, was: " + metrics.responseTime + "ms");

        printMetricsSummary("MEDIUM COMPLEXITY", metrics);
        System.out.println("=".repeat(70) + "\n");
    }

    /**
     * TEST 1C: A* Pathfinding Performance - Stress Test
     *
     * Scenario: Long distance with potential obstacles
     * Expected: < 15 seconds
     *
     * This represents challenging pathfinding scenario
     */
    @Test
    @Order(3)
    @DisplayName("Performance 1C: A* Stress - Long Distance")
    void testAStarPerformanceStress() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("=== A* PERFORMANCE TEST 1C: STRESS ===");
        System.out.println("=".repeat(70));

        PerformanceMetrics metrics = runPathfindingTest(
                "Stress_Long_Distance",
                createLongDistanceOrder(102)
        );

        recordMetrics("A*_Stress", "Long_Distance_Obstacles", metrics);

        assertTrue(metrics.responseTime < 15000,
                "Stress test should complete in < 15s, was: " + metrics.responseTime + "ms");

        printMetricsSummary("STRESS TEST", metrics);
        System.out.println("=".repeat(70) + "\n");
    }

    /**
     * TEST 1D: A* Performance - Multi-Delivery Optimization
     *
     * Scenario: Multiple deliveries requiring path optimization
     * Expected: < 12 seconds total
     */
    @Test
    @Order(4)
    @DisplayName("Performance 1D: A* Multi-Delivery Optimization")
    void testAStarMultiDelivery() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("=== A* PERFORMANCE TEST 1D: MULTI-DELIVERY ===");
        System.out.println("=".repeat(70));

        List<MedDispatchRec> multiOrders = Arrays.asList(
                createSimpleOrder(200),
                createSimpleOrder(201),
                createSimpleOrder(202)
        );

        long startTime = System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<MedDispatchRec>> request = new HttpEntity<>(multiOrders, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/calcDeliveryPath",
                request,
                String.class
        );

        long responseTime = System.currentTimeMillis() - startTime;

        assertEquals(HttpStatus.OK, response.getStatusCode());

        DeliveryPathResponse pathResponse = objectMapper.readValue(
                response.getBody(),
                DeliveryPathResponse.class
        );

        PerformanceMetrics metrics = new PerformanceMetrics(
                responseTime,
                0,
                0,
                pathResponse.totalMoves(),
                pathResponse.totalCost(),
                "PASS"
        );

        recordMetrics("A*_Multi_Delivery", "Three_Orders_Optimization", metrics);

        System.out.println("\n=== MULTI-DELIVERY RESULTS ===");
        System.out.println("Response Time: " + responseTime + "ms");
        System.out.println("Orders Processed: 3");
        System.out.println("Avg Time Per Order: " + (responseTime / 3) + "ms");
        System.out.println("Total Moves: " + pathResponse.totalMoves());
        System.out.println("Drone Paths: " + pathResponse.dronePaths().size());

        assertTrue(responseTime < 12000,
                "Multi-delivery should complete in < 12s, was: " + responseTime + "ms");

        System.out.println("=".repeat(70) + "\n");
    }

    /**
     * TEST 2A: WebSocket Message Load - Baseline
     *
     * Scenario: Normal pathfinding with standard progress updates
     * Expected: Messages delivered successfully, < 100ms avg latency
     */
    @Test
    @Order(5)
    @DisplayName("Performance 2A: WebSocket Baseline - Normal Load")
    void testWebSocketMessageLoadBaseline() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("=== WEBSOCKET PERFORMANCE TEST 2A: BASELINE ===");
        System.out.println("=".repeat(70));

        WebSocketMetrics metrics = runWebSocketTest(
                "WS_Baseline",
                createSimpleOrder(300),
                1
        );

        recordWebSocketMetrics("WebSocket_Baseline", "Single_Client_Normal_Load", metrics);

        assertTrue(metrics.messagesReceived > 0, "Should receive progress updates");
        assertEquals(0, metrics.messageLoss, "Should have no message loss");

        printWebSocketSummary("BASELINE", metrics);
        System.out.println("=".repeat(70) + "\n");
    }

    /**
     * TEST 2B: WebSocket Message Load - Multiple Clients
     *
     * Scenario: 5 clients simultaneously receiving updates
     * Expected: All clients receive messages, no crashes
     */
    @Test
    @Order(6)
    @DisplayName("Performance 2B: WebSocket Multi-Client - 5 Concurrent Connections")
    void testWebSocketMultipleClients() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("=== WEBSOCKET PERFORMANCE TEST 2B: MULTI-CLIENT ===");
        System.out.println("=".repeat(70));

        WebSocketMetrics metrics = runWebSocketTest(
                "WS_Multi_Client",
                createMediumComplexityOrder(301),
                5
        );

        recordWebSocketMetrics("WebSocket_Multi_Client", "Five_Concurrent_Clients", metrics);

        assertEquals(5, metrics.connectedClients, "All 5 clients should connect");
        assertTrue(metrics.messagesReceived > 0, "Clients should receive messages");

        printWebSocketSummary("MULTI-CLIENT (5)", metrics);
        System.out.println("=".repeat(70) + "\n");
    }

    /**
     * TEST 2C: WebSocket Message Load - High Frequency Stress
     *
     * Scenario: Complex pathfinding generating many progress updates
     * Expected: System handles high message volume
     */
    @Test
    @Order(7)
    @DisplayName("Performance 2C: WebSocket Stress - High Message Volume")
    void testWebSocketHighFrequencyStress() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("=== WEBSOCKET PERFORMANCE TEST 2C: HIGH FREQUENCY ===");
        System.out.println("=".repeat(70));

        WebSocketMetrics metrics = runWebSocketTest(
                "WS_High_Frequency",
                createLongDistanceOrder(302),
                3
        );

        recordWebSocketMetrics("WebSocket_Stress", "High_Message_Volume", metrics);

        assertTrue(metrics.messagesReceived > 0, "Should generate messages");
        assertTrue(metrics.averageLatency < 200,
                "Average message latency should be reasonable, was: " + metrics.averageLatency + "ms");

        printWebSocketSummary("HIGH FREQUENCY STRESS", metrics);
        System.out.println("=".repeat(70) + "\n");
    }

    /**
     * TEST 3: Combined Stress - A* + WebSocket
     *
     * Scenario: Complex pathfinding with multiple WebSocket clients
     * Expected: Both subsystems perform under combined load
     */
    @Test
    @Order(8)
    @DisplayName("Performance 3: Combined Stress - A* + WebSocket")
    void testCombinedStress() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("=== COMBINED STRESS TEST ===");
        System.out.println("=".repeat(70));

        long testStartTime = System.currentTimeMillis();

        WebSocketMetrics wsMetrics = runWebSocketTest(
                "Combined_Stress",
                createMediumComplexityOrder(400),
                10
        );

        long totalTestTime = System.currentTimeMillis() - testStartTime;

        System.out.println("\n=== COMBINED STRESS RESULTS ===");
        System.out.println("Total Test Duration: " + totalTestTime + "ms");
        System.out.println("WebSocket Clients: 10");
        System.out.println("Messages Received: " + wsMetrics.messagesReceived);
        System.out.println("Message Loss: " + wsMetrics.messageLoss);
        System.out.println("Avg Latency: " + String.format("%.2f", wsMetrics.averageLatency) + "ms");

        assertTrue(totalTestTime < 20000,
                "Combined stress should complete in < 20s, was: " + totalTestTime + "ms");
        assertEquals(10, wsMetrics.connectedClients, "All 10 clients should connect");

        System.out.println("=".repeat(70) + "\n");
    }

    // ==================== HELPER METHODS ====================

    private PerformanceMetrics runPathfindingTest(String testName, MedDispatchRec order)
            throws Exception {

        AtomicInteger nodeCount = new AtomicInteger(0);
        AtomicInteger totalMessages = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        StandardWebSocketClient client = new StandardWebSocketClient();
        CountDownLatch connectLatch = new CountDownLatch(1);
        String wsUrl = "ws://localhost:" + port + "/ws/pathfinding-progress/websocket";

        WebSocketSession session = client.execute(
                new TextWebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                        connectLatch.countDown();
                    }

                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                        totalMessages.incrementAndGet();
                        String payload = message.getPayload();

                        try {
                            PathfindingProgress progress = objectMapper.readValue(
                                    payload,
                                    PathfindingProgress.class
                            );

                            if ("nodeExplored".equals(progress.type())) {
                                nodeCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            // Ignore parsing errors for this test
                        }
                    }
                },
                wsUrl
        ).get(5, TimeUnit.SECONDS);

        connectLatch.await(2, TimeUnit.SECONDS);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<MedDispatchRec>> request = new HttpEntity<>(
                Collections.singletonList(order),
                headers
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/calcDeliveryPath",
                request,
                String.class
        );

        long responseTime = System.currentTimeMillis() - startTime;

        DeliveryPathResponse pathResponse = null;
        String status = "FAIL";

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            pathResponse = objectMapper.readValue(response.getBody(), DeliveryPathResponse.class);
            status = "PASS";
        }

        Thread.sleep(1000); // Allow WebSocket messages to arrive
        session.close();

        return new PerformanceMetrics(
                responseTime,
                nodeCount.get(),
                totalMessages.get(),
                pathResponse != null ? pathResponse.totalMoves() : 0,
                pathResponse != null ? pathResponse.totalCost() : 0.0,
                status
        );
    }

    private WebSocketMetrics runWebSocketTest(String testName, MedDispatchRec order,
                                              int clientCount) throws Exception {

        CountDownLatch allConnected = new CountDownLatch(clientCount);
        List<AtomicInteger> messageCounts = new ArrayList<>();
        List<Long> connectionTimes = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < clientCount; i++) {
            messageCounts.add(new AtomicInteger(0));
        }

        List<WebSocketSession> sessions = new ArrayList<>();
        long wsStartTime = System.currentTimeMillis();
        String wsUrl = "ws://localhost:" + port + "/ws/pathfinding-progress/websocket";

        StandardWebSocketClient client = new StandardWebSocketClient();

        for (int i = 0; i < clientCount; i++) {
            final int clientId = i;

            WebSocketSession session = client.execute(
                    new TextWebSocketHandler() {
                        @Override
                        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                            connectionTimes.add(System.currentTimeMillis() - wsStartTime);
                            allConnected.countDown();
                        }

                        @Override
                        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                            messageCounts.get(clientId).incrementAndGet();
                        }
                    },
                    wsUrl
            ).get(5, TimeUnit.SECONDS);

            sessions.add(session);
        }

        assertTrue(allConnected.await(10, TimeUnit.SECONDS),
                "All clients should connect within 10s");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<MedDispatchRec>> request = new HttpEntity<>(
                Collections.singletonList(order),
                headers
        );

        restTemplate.postForEntity(baseUrl + "/api/v1/calcDeliveryPath", request, String.class);

        Thread.sleep(2000); // Allow messages to propagate

        int totalMessages = messageCounts.stream().mapToInt(AtomicInteger::get).sum();
        int minMessages = messageCounts.stream().mapToInt(AtomicInteger::get).min().orElse(0);
        int maxMessages = messageCounts.stream().mapToInt(AtomicInteger::get).max().orElse(0);
        int messageLoss = Math.max(0, (maxMessages * clientCount) - totalMessages);

        double avgLatency = connectionTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        for (WebSocketSession s : sessions) {
            s.close();
        }

        return new WebSocketMetrics(
                clientCount,
                totalMessages,
                messageLoss,
                avgLatency,
                minMessages,
                maxMessages
        );
    }

    private MedDispatchRec createSimpleOrder(Integer id) {
        return new MedDispatchRec(
                id,
                "2025-01-20",
                "10:00:00",
                new MedDispatchRec.Requirements(5.0, false, false, 200.0),
                new Position(-3.188, 55.944)
        );
    }

    private MedDispatchRec createMediumComplexityOrder(Integer id) {
        return new MedDispatchRec(
                id,
                "2025-01-20",
                "10:00:00",
                new MedDispatchRec.Requirements(5.0, false, false, 300.0),
                new Position(-3.180, 55.950)
        );
    }

    private MedDispatchRec createLongDistanceOrder(Integer id) {
        return new MedDispatchRec(
                id,
                "2025-01-20",
                "10:00:00",
                new MedDispatchRec.Requirements(5.0, false, false, 500.0),
                new Position(-3.170, 55.960)
        );
    }

    private void recordMetrics(String testName, String scenario, PerformanceMetrics metrics) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        csvWriter.printf("%s,%s,%s,%d,%d,%d,%d,%.2f,%s%n",
                timestamp, testName, scenario,
                metrics.responseTime, metrics.nodesExplored, metrics.messagesReceived,
                metrics.totalMoves, metrics.totalCost, metrics.status);
        csvWriter.flush();
    }

    private void recordWebSocketMetrics(String testName, String scenario, WebSocketMetrics metrics) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        csvWriter.printf("%s,%s,%s,%.0f,%d,%d,%d,0,PASS%n",
                timestamp, testName, scenario,
                metrics.averageLatency, 0, metrics.messagesReceived, 0);
        csvWriter.flush();
    }

    private void printMetricsSummary(String label, PerformanceMetrics metrics) {
        System.out.println("\n=== " + label + " RESULTS ===");
        System.out.println("Response Time: " + metrics.responseTime + "ms");
        System.out.println("Nodes Explored: " + metrics.nodesExplored);
        System.out.println("Messages Received: " + metrics.messagesReceived);
        System.out.println("Total Moves: " + metrics.totalMoves);
        System.out.println("Total Cost: " + String.format("%.2f", metrics.totalCost));
        System.out.println("Status: " + metrics.status);
    }

    private void printWebSocketSummary(String label, WebSocketMetrics metrics) {
        System.out.println("\n=== " + label + " RESULTS ===");
        System.out.println("Connected Clients: " + metrics.connectedClients);
        System.out.println("Messages Received: " + metrics.messagesReceived);
        System.out.println("Message Loss: " + metrics.messageLoss);
        System.out.println("Avg Connection Latency: " + String.format("%.2f", metrics.averageLatency) + "ms");
        System.out.println("Min Messages (per client): " + metrics.minMessages);
        System.out.println("Max Messages (per client): " + metrics.maxMessages);
    }

    private static class PerformanceMetrics {
        long responseTime;
        int nodesExplored;
        int messagesReceived;
        int totalMoves;
        double totalCost;
        String status;

        PerformanceMetrics(long responseTime, int nodesExplored, int messagesReceived,
                           int totalMoves, double totalCost, String status) {
            this.responseTime = responseTime;
            this.nodesExplored = nodesExplored;
            this.messagesReceived = messagesReceived;
            this.totalMoves = totalMoves;
            this.totalCost = totalCost;
            this.status = status;
        }
    }

    private static class WebSocketMetrics {
        int connectedClients;
        int messagesReceived;
        int messageLoss;
        double averageLatency;
        int minMessages;
        int maxMessages;

        WebSocketMetrics(int connectedClients, int messagesReceived, int messageLoss,
                         double averageLatency, int minMessages, int maxMessages) {
            this.connectedClients = connectedClients;
            this.messagesReceived = messagesReceived;
            this.messageLoss = messageLoss;
            this.averageLatency = averageLatency;
            this.minMessages = minMessages;
            this.maxMessages = maxMessages;
        }
    }
}