import { useEffect, useState, useRef, useMemo } from 'react';
import { Marker, Popup, Polyline, CircleMarker } from 'react-leaflet';
import L from 'leaflet';
import type { DeliveryPathResponse } from '../../types';

interface DroneAnimationProps {
  deliveryPath: DeliveryPathResponse | null;
  isAnimating: boolean;
  speed?: number;
  onAnimationComplete?: () => void;
}

const createDroneIcon = (color: string) => {
  return L.divIcon({
    html: `
      <div style="
        width: 32px;
        height: 32px;
        background: ${color};
        border: 3px solid white;
        border-radius: 50%;
        box-shadow: 0 2px 8px rgba(0,0,0,0.4);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;
        animation: pulse 1.5s ease-in-out infinite;
      ">
        🚁
      </div>
      <style>
        @keyframes pulse {
          0%, 100% { transform: scale(1); }
          50% { transform: scale(1.1); }
        }
      </style>
    `,
    className: 'drone-marker',
    iconSize: [32, 32],
    iconAnchor: [16, 16],
  });
};

const DRONE_COLORS = [
  '#2196F3',
  '#4CAF50',
  '#FF9800',
  '#E91E63',
  '#9C27B0',
];

interface DroneState {
  droneId: string;
  currentPosition: [number, number];
  pathIndex: number;
  deliveryIndex: number;
  completedPath: [number, number][];
  isReturning: boolean;
  isCompleted: boolean;
}

const DroneAnimation: React.FC<DroneAnimationProps> = ({
  deliveryPath,
  isAnimating,
  speed = 1,
  onAnimationComplete,
}) => {
  const [droneStates, setDroneStates] = useState<DroneState[]>([]);
  const animationRef = useRef<number | null>(null);
  const lastUpdateTime = useRef<number>(Date.now());

  const droneIcons = useMemo(() => {
    return DRONE_COLORS.map(color => createDroneIcon(color));
  }, []);

  const deliveryPoints = useMemo(() => {
    if (!deliveryPath) return [];

    const points: Array<{ id: string; position: [number, number] }> = [];

    deliveryPath.dronePaths.forEach((dronePath) => {
      dronePath.deliveries.forEach((delivery) => {
        if (delivery.deliveryId && delivery.flightPath.length > 0) {
          const lastPoint = delivery.flightPath[delivery.flightPath.length - 1];
          if (lastPoint && lastPoint.lat && lastPoint.lng) {
            points.push({
              id: delivery.deliveryId,
              position: [lastPoint.lat, lastPoint.lng]
            });
          }
        }
      });
    });

    console.log('📦 Delivery points:', points.length);
    return points;
  }, [deliveryPath]);

  useEffect(() => {
    if (!deliveryPath || !isAnimating) {
      return;
    }

    const initialStates: DroneState[] = deliveryPath.dronePaths.map((dronePath) => {
      const firstDelivery = dronePath.deliveries[0];
      const firstPosition = firstDelivery?.flightPath[0];

      return {
        droneId: dronePath.droneId,
        currentPosition: firstPosition
          ? [firstPosition.lat, firstPosition.lng]
          : [0, 0],
        pathIndex: 0,
        deliveryIndex: 0,
        completedPath: firstPosition
          ? [[firstPosition.lat, firstPosition.lng]]
          : [],
        isReturning: false,
        isCompleted: false,
      };
    });

    setDroneStates(initialStates);
    lastUpdateTime.current = Date.now();
  }, [deliveryPath, isAnimating]);

  useEffect(() => {
    if (!isAnimating || !deliveryPath || droneStates.length === 0) {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
        animationRef.current = null;
      }
      return;
    }

    let frameId: number;

    const animate = () => {
      const now = Date.now();
      const deltaTime = now - lastUpdateTime.current;

      const updateInterval = Math.max(50, 120 / speed);

      if (deltaTime >= updateInterval) {
        lastUpdateTime.current = now;

        setDroneStates((prevStates) => {
          let hasChanges = false;

          const newStates = prevStates.map((state, droneIndex) => {
            if (state.isCompleted) {
              return state;
            }

            hasChanges = true;
            const dronePath = deliveryPath.dronePaths[droneIndex];
            const currentDelivery = dronePath.deliveries[state.deliveryIndex];

            if (!currentDelivery) {
              return { ...state, isCompleted: true };
            }

            const flightPath = currentDelivery.flightPath;

            const step = Math.max(1, Math.floor(speed));
            const nextPathIndex = state.pathIndex + step;

            if (nextPathIndex >= flightPath.length) {
              const nextDeliveryIndex = state.deliveryIndex + 1;

              if (nextDeliveryIndex >= dronePath.deliveries.length) {
                return { ...state, isCompleted: true };
              }

              const nextDelivery = dronePath.deliveries[nextDeliveryIndex];
              const nextPosition = nextDelivery.flightPath[0];
              const nextPos: [number, number] = [nextPosition.lat, nextPosition.lng];

              return {
                ...state,
                deliveryIndex: nextDeliveryIndex,
                pathIndex: 0,
                currentPosition: nextPos,
                completedPath: [
                  ...state.completedPath,
                  nextPos,
                ],
              };
            }

            const actualIndex = Math.min(nextPathIndex, flightPath.length - 1);
            const nextPoint = flightPath[actualIndex];
            const newPosition: [number, number] = [nextPoint.lat, nextPoint.lng];

            const newCompletedPath = [...state.completedPath, newPosition];
            if (newCompletedPath.length > 500) {
              newCompletedPath.splice(0, newCompletedPath.length - 500);
            }

            return {
              ...state,
              pathIndex: actualIndex,
              currentPosition: newPosition,
              completedPath: newCompletedPath,
            };
          });

          const allCompleted = newStates.every((s) => s.isCompleted);
          if (allCompleted && onAnimationComplete) {
            onAnimationComplete();
            return newStates;
          }

          return hasChanges ? newStates : prevStates;
        });
      }

      frameId = requestAnimationFrame(animate);
    };

    frameId = requestAnimationFrame(animate);

    return () => {
      if (frameId) {
        cancelAnimationFrame(frameId);
      }
    };
  }, [isAnimating, deliveryPath, droneStates.length, onAnimationComplete, speed]);

  if (!deliveryPath) {
    return null;
  }

  return (
    <>
      {deliveryPoints.map((point) => (
        <CircleMarker
          key={`delivery-${point.id}`}
          center={point.position}
          radius={8}
          pathOptions={{
            color: '#FF6B6B',
            fillColor: '#FF6B6B',
            fillOpacity: 0.8,
            weight: 2,
          }}
        >
          <Popup>
            <div style={{ minWidth: 120 }}>
              <strong>📦 Delivery Point</strong>
              <br />
              Order ID: {point.id}
              <br />
              Location: ({point.position[0].toFixed(4)}, {point.position[1].toFixed(4)})
            </div>
          </Popup>
        </CircleMarker>
      ))}

      {isAnimating && droneStates.map((state, index) => {
        const color = DRONE_COLORS[index % DRONE_COLORS.length];
        const dronePath = deliveryPath.dronePaths[index];

        return (
          <div key={state.droneId}>
            {state.completedPath.length > 1 && (
              <Polyline
                positions={state.completedPath}
                pathOptions={{
                  color: color,
                  weight: 2,
                  opacity: 0.5,
                }}
              />
            )}

            {/* 无人机标记 */}
            <Marker
              position={state.currentPosition}
              icon={droneIcons[index % droneIcons.length]}
            >
              <Popup>
                <div style={{ minWidth: 150 }}>
                  <strong>🚁 Drone {index + 1}</strong>
                  <br />
                  ID: {state.droneId}
                  <br />
                  Delivery: {state.deliveryIndex + 1}/{dronePath.deliveries.length}
                  <br />
                  Status: {state.isCompleted ? '✅ Completed' : '🚀 Flying'}
                  <br />
                  Position: ({state.currentPosition[0].toFixed(4)},{' '}
                  {state.currentPosition[1].toFixed(4)})
                </div>
              </Popup>
            </Marker>
          </div>
        );
      })}
    </>
  );
};

export default DroneAnimation;