# 🚁 Drone Delivery Visualization System

Real-time visualization platform for drone delivery path planning and optimization.

## 🎯 Features

- **Interactive Map**: Leaflet-based map showing Edinburgh area with service points and restricted areas
- **Real-time Progress**: WebSocket connection displaying A* pathfinding algorithm execution
- **Order Management**: Add, edit, and manage delivery orders with various constraints
- **Performance Analytics**: Real-time metrics showing cost, moves, nodes explored, and calculation time
- **Multi-Drone Support**: Visualize complex multi-drone delivery scenarios
- **Demo Scenarios**: Pre-configured test cases for quick demonstration
- **Animation Control:** Start, pause, reset and speed adjustment (0.5×–5×) for drone path animation, enabling step-by-step inspection of A* exploration and delivery movements.

## 🛠️ Tech Stack

- **Frontend**: React 18 + TypeScript
- **UI Library**: Ant Design 5
- **Map**: Leaflet + React-Leaflet
- **Charts**: Recharts
- **Communication**: Axios + SockJS (WebSocket)
- **Build Tool**: Vite (powered by Node.js)

## 📦 Installation
Install node.js and npm:

```bash
npm install leaflet react-leaflet @types/leaflet
npm install recharts
npm install antd
npm install axios
npm install sockjs-client @types/sockjs-client
npm install dayjs
```

## 🚀 Quick Start

### 1. Start Backend
Ensure your backend is running on `http://localhost:8080`

Run the Spring Boot application:
`IlpSubmissionImageApplication.java`

### 2. Use the Application

1. **Add Orders**: Use the left panel or click directly on the map to add delivery orders 
2. **Load Demo**: Click dropdown next to "Calculate Path" to load pre-configured scenarios
3. **Calculate**: Click "Calculate Path" to start the optimization
4. **Monitor**: Watch real-time progress in the right panel
5. **Analyze**: Review performance metrics after calculation completes
6. **Animate**: Click "Animation Control" to control the drone path animation

## 🧩 Development Mode

During development, you may want to run the frontend and backend separately for faster debugging and hot-reloading.

### 1. Start Frontend (Vite Dev Server)

Navigate to the frontend project folder and run:

```bash
cd drone-visualizer
```

```bash
npm install
```

```bash
npm run dev
```
The application will start at:
`http://localhost:3000`

### 2. Start Backend (Spring Boot)

Make sure your backend is running at:
`http://localhost:8080`
You can start it using your IDE (e.g., IntelliJ) or with:
```bash
mvn spring-boot:run
```

### 3. Frontend–Backend Integration in Dev Mode

No additional configuration is required because the frontend automatically uses the values from your `.env` file:
```env
VITE_API_BASE=http://localhost:8080/api/v1
VITE_WS_BASE=http://localhost:8080
```

This setup allows:
- Hot module replacement (HMR) for all frontend changes

- Instant UI updates without rebuilding static files

- Independent backend debugging

- Real-time WebSocket communication via the backend

### 4. When to Build for Production

You only need to build the frontend when preparing for deployment or packaging the Spring Boot JAR:
```bash
npm run build
```

This generates a dist/ folder that can be copied to:

`src/main/resources/static/`

Spring Boot will serve it automatically on startup.

## 📊 Demo Scenarios

### Simple Scenario (2 orders)
- Basic delivery with cooling/heating requirements
- Single drone, same day

### Complex Scenario (6 orders)
- Multiple deliveries with various constraints
- Cost optimization with maxCost limits
- Restricted area avoidance

### Multi-Day Scenario (15 orders)
- Deliveries across multiple days
- Different time windows
- Multi-drone coordination

## 🏗️ Project Structure
```
drone-visualizer/
├── src/
│   ├── components/
│   │   ├── Map/
│   │   │   ├── DroneAnimation.tsx        # Main drone animation component
│   │   │   ├── DroneMap.tsx              # Main map rendering component
│   │   │   ├── PathAnimation.tsx         # Path animation visualization
│   │   │   └── RestrictedAreas.tsx       # Restricted (no-fly) zones renderer
│   │   ├── Control/
│   │   │   ├── AnimationControl.tsx      # Animation controls (start/pause/speed)
│   │   │   ├── OrderForm.tsx             # Order creation form
│   │   │   ├── OrderList.tsx             # Order list panel
│   │   │   └── CalculateButton.tsx       # Path calculation trigger button
│   │   ├── Monitor/
│   │   │   ├── ProgressMonitor.tsx       # Real-time progress monitoring
│   │   │   └── PerformanceChart.tsx      # Performance statistics and charts
│   │   └── Layout/
│   │       └── AppLayout.tsx             # Overall application layout
│   ├── services/
│   │   ├── api.ts                        # REST API services
│   │   └── websocket.ts                  # WebSocket connection handler
│   ├── types/
│   │   └── index.ts                      # TypeScript type definitions
│   ├── App.tsx
│   ├── App.css
│   └── main.tsx
├── package.json
└── vite.config.ts
```

## 🔧 Configuration

### Environment Variables

Create `.env` file in the root directory:
```env
VITE_API_BASE=http://localhost:8080/api/v1
VITE_WS_BASE=http://localhost:8080
```

### Backend Requirements

Your backend must provide the following endpoints:

- `GET /api/v1/drones`
- `GET /api/v1/service-points`
- `GET /api/v1/restricted-areas`
- `POST /api/v1/calcDeliveryPathAsGeoJson`
- `WS /ws/pathfinding-progress`

## 🔗 Frontend-Backend Integration

### Architecture
```
Frontend (React + Vite)          Backend (Spring Boot)
Port 3000/5173                   Port 8080
     │                                │
     ├──── HTTP REST ────────────────►│
     │     • Load drones/locations    │
     │     • Submit orders             │
     │                                │
     └──── WebSocket ────────────────►│
           • Real-time A* progress    │
           • Node exploration updates │
```

### Communication Flow

**1. Startup**: Frontend loads initial data via 3 parallel REST calls (`/drones`, `/service-points`, `/restricted-areas`)

**2. Calculate**: User submits orders → POST to `/calcDeliveryPathAsGeoJson`

**3. Progress**: Backend streams A* exploration via WebSocket at `/ws/pathfinding-progress`
- Message types: `node_explored`, `path_found`, `batch_completed`
- Throttled to every 10th node to reduce network load

**4. Complete**: Final GeoJSON path returned via REST response

### Key Integration Points

- **SockJS**: Used for WebSocket to support fallback transports
- **CORS**: Backend allows origins `localhost:3000` and `localhost:5173` (Vite dev server)
- **Reconnection**: Frontend auto-reconnects up to 5 times with increasing delays
- **Cancellation**: Path calculation can be aborted mid-flight via AbortSignal

### Running Together

Backend must start first on port 8080, then frontend connects automatically using `.env` configuration.

## 📈 Performance Optimization

- Automatic chunking for better caching
- WebSocket message throttling (every 10 nodes)
- Only last 100 explored nodes rendered on map
- React.memo for expensive components

## 🐛 Troubleshooting

### WebSocket Connection Failed

Check that:
1. Backend is running on correct port (8080)
2. CORS is properly configured
3. Firewall allows WebSocket connections

### Map Not Loading

Ensure:
1. Internet connection available (for OpenStreetMap tiles)
2. Leaflet CSS is properly imported
3. Init data loaded successfully

### Orders Not Calculating

Verify:
1. At least one order added
2. Backend endpoints responding
3. Order data format matches backend expectations

## 📝 Development Notes

### Adding New Features

1. **New DTO**: Add to `src/types/index.ts`
2. **New API**: Add to `src/services/api.ts`
3. **New Component**: Create in appropriate `src/components/` subfolder
4. **State Management**: Use React hooks (useState, useEffect)

### WebSocket Message Types
```typescript
type MessageType = 
  | 'node_explored'           // A* explored a node
  | 'path_found'              // Path found for delivery
  | 'batch_started'           // Batch execution started
  | 'batch_completed'         // Batch execution completed
  | 'delivery_started'        // Started calculating delivery
  | 'connection_established'  // WebSocket connected
  | 'error';                  // Error occurred
```

## 📄 License

This project is part of ILP CW3 coursework submission.

## 👤 Author

Student ID: s2337850