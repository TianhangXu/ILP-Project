import { useState, useEffect, useRef } from 'react';
import { message } from 'antd';
import AppLayout from './components/Layout/AppLayout';
import DroneMap from './components/Map/DroneMap';
import OrderForm from './components/Control/OrderForm';
import OrderList from './components/Control/OrderList';
import CalculateButton from './components/Control/CalculateButton';
import ProgressMonitor from './components/Monitor/ProgressMonitor';
import PerformanceChart from './components/Monitor/PerformanceChart';
import { wsService } from './services/websocket';
import { getInitData, calculateDeliveryPathAsGeoJson, calculateDeliveryPath } from './services/api';
import type {
  MedDispatchRec,
  InitData,
  PathfindingProgress,
  DeliveryPathResponse,
} from './types';
import './App.css';

function App() {
  const [initData, setInitData] = useState<InitData | null>(null);
  const [orders, setOrders] = useState<MedDispatchRec[]>([]);
  const [geoJsonData, setGeoJsonData] = useState<any>(null);
  const [progress, setProgress] = useState<PathfindingProgress[]>([]);
  const [isCalculating, setIsCalculating] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const [calculationResult, setCalculationResult] = useState<DeliveryPathResponse | null>(null);
  const [calculationTime, setCalculationTime] = useState(0);
  const [clickedLocation, setClickedLocation] = useState<{ lat: number; lng: number } | null>(null);

  const abortControllerRef = useRef<AbortController | null>(null);
  const progressTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    const loadInitData = async () => {
      try {
        const data = await getInitData();
        setInitData(data);
        message.success('Map data loaded successfully');
      } catch (error) {
        console.error('Failed to load init data:', error);
        message.error('Failed to load map data');
      }
    };

    loadInitData();

    wsService.connect();

    const unsubscribeMessage = wsService.onMessage((msg) => {
      setProgress((prev) => {
        if (!isCalculating) {
          return prev;
        }
        return [...prev, msg];
      });
    });

    const unsubscribeConnection = wsService.onConnectionChange((connected) => {
      setIsConnected(connected);

      if (connected) {
        message.success('WebSocket connected', 2);
      }
    });

    return () => {
      unsubscribeMessage();
      unsubscribeConnection();
      wsService.disconnect();
      if (progressTimeoutRef.current) {
        clearTimeout(progressTimeoutRef.current);
      }
    };
  }, [isCalculating]);

  const handleAddOrder = (order: MedDispatchRec) => {
    setOrders((prev) => [...prev, order]);
    message.success(`Order #${order.id} added`);
  };

  const handleRemoveOrder = (id: number) => {
    setOrders((prev) => prev.filter((o) => o.id !== id));
    message.info(`Order #${id} removed`);
  };

  const handleClearAll = () => {
    if (isCalculating) {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
      setIsCalculating(false);
    }

    setOrders([]);
    setGeoJsonData(null);
    setProgress([]);
    setCalculationResult(null);
    setCalculationTime(0);
    setClickedLocation(null);

    message.info('All orders cleared');
  };

  const handleLoadScenario = (scenarioOrders: MedDispatchRec[]) => {
    setOrders(scenarioOrders);
    setGeoJsonData(null);
    setProgress([]);
    setCalculationResult(null);
  };

  const handleCancelCalculation = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }

    setIsCalculating(false);

    if (progressTimeoutRef.current) {
      clearTimeout(progressTimeoutRef.current);
    }

    progressTimeoutRef.current = setTimeout(() => {
      setProgress([]);
    }, 500);

    message.warning('Calculation cancelled by user');
  };

  const handleCalculate = async () => {
    if (orders.length === 0) {
      message.warning('Please add at least one order');
      return;
    }

    if (!isConnected) {
      message.error('WebSocket is not connected. Please check if the backend server is running.');
      return;
    }

    setIsCalculating(true);
    setProgress([]);
    setGeoJsonData(null);
    setCalculationResult(null);

    const startTime = performance.now();

    abortControllerRef.current = new AbortController();
    const { signal } = abortControllerRef.current;

    const hideLoading = message.loading('Calculating optimal delivery paths...', 0);

    try {
      await new Promise(resolve => setTimeout(resolve, 100));

      const [geoJson, detailedResult] = await Promise.all([
        calculateDeliveryPathAsGeoJson(orders, signal),
        calculateDeliveryPath(orders, signal)
      ]);

      if (signal.aborted) {
        hideLoading();
        return;
      }

      const endTime = performance.now();
      const timeTaken = Math.round(endTime - startTime);
      setCalculationTime(timeTaken);

      setGeoJsonData(geoJson);
      setCalculationResult(detailedResult);

      hideLoading();
      message.success(`Path calculated successfully in ${(timeTaken / 1000).toFixed(2)}s!`);
    } catch (error: any) {
      hideLoading();

      if (error.name === 'AbortError' || error.name === 'CanceledError' || error.message === 'CanceledError') {
        return;
      }

      console.error('Calculation failed:', error);
      message.error('Failed to calculate path. Please check your orders and try again.');
    } finally {
      setIsCalculating(false);
      abortControllerRef.current = null;
    }
  };

  const handleMapClick = (lat: number, lng: number) => {
    setClickedLocation({ lat, lng });
  };

  const handleClearClickedLocation = () => {
    setClickedLocation(null);
  };

  return (
    <AppLayout
      leftPanel={
        <div style={{ padding: '20px' }}>
          <h3 style={{ marginTop: 0 }}>Add Delivery Order</h3>

          <div style={{
            marginBottom: 16,
            padding: '8px 12px',
            borderRadius: 4,
            background: isConnected ? '#f6ffed' : '#fff1f0',
            border: `1px solid ${isConnected ? '#b7eb8f' : '#ffa39e'}`,
            display: 'flex',
            alignItems: 'center',
            gap: 8,
          }}>
            <div style={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              background: isConnected ? '#52c41a' : '#ff4d4f',
              animation: isConnected ? 'none' : 'blink 1.5s infinite',
            }} />
            <span style={{
              fontSize: 13,
              color: isConnected ? '#52c41a' : '#ff4d4f',
              fontWeight: 500,
            }}>
              {isConnected ? 'WebSocket Connected' : 'WebSocket Disconnected'}
            </span>
          </div>

          <OrderForm
            onAddOrder={handleAddOrder}
            existingOrders={orders}
            maxOrders={30}
            restrictedAreas={initData?.restrictedAreas}
            clickedLocation={clickedLocation}
            onClearClickedLocation={handleClearClickedLocation}
          />
          <OrderList
            orders={orders}
            onRemoveOrder={handleRemoveOrder}
            onClearAll={handleClearAll}
          />
          <CalculateButton
            orders={orders}
            isCalculating={isCalculating}
            onCalculate={handleCalculate}
            onCancelCalculation={handleCancelCalculation}
            onLoadScenario={handleLoadScenario}
          />
        </div>
      }
      map={
        <DroneMap
          initData={initData}
          geoJsonData={geoJsonData}
          exploredNodes={progress}
          isCalculating={isCalculating}
          deliveryPathResult={calculationResult}
          pendingOrders={orders}
          onMapClick={handleMapClick}
        />
      }
      rightPanel={
        <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: 20, height: '100%' }}>
          <div style={{ flex: 1, minHeight: 0 }}>
            <ProgressMonitor progress={progress} isConnected={isConnected} />
          </div>
          <div style={{ flex: 1, minHeight: 0 }}>
            <PerformanceChart
              result={calculationResult}
              progress={progress}
              calculationTime={calculationTime}
            />
          </div>
        </div>
      }
    />
  );
}

export default App;