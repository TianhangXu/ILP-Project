import React, { useRef, useEffect, useState, useMemo, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, Popup, GeoJSON, useMap, CircleMarker, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import RestrictedAreas from './RestrictedAreas';
import PathAnimation from './PathAnimation';
import DroneAnimation from './DroneAnimation';
import AnimationControl from '../Control/AnimationControl';
import type { InitData, PathfindingProgress, DeliveryPathResponse, MedDispatchRec } from '../../types';

delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

const servicePointIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

interface DroneMapProps {
  initData: InitData | null;
  geoJsonData: any;
  exploredNodes: PathfindingProgress[];
  isCalculating: boolean;
  deliveryPathResult: DeliveryPathResponse | null;
  pendingOrders?: MedDispatchRec[];
  onMapClick?: (lat: number, lng: number) => void;
}

const MapInitializer: React.FC = () => {
  const map = useMap();

  useEffect(() => {
    setTimeout(() => {
      map.invalidateSize();
    }, 100);
  }, [map]);

  return null;
};

interface MapClickHandlerProps {
  onMapClick?: (lat: number, lng: number) => void;
}

const MapClickHandler: React.FC<MapClickHandlerProps> = ({ onMapClick }) => {
  useMapEvents({
    click: (e) => {
      if (onMapClick) {
        onMapClick(e.latlng.lat, e.latlng.lng);
      }
    },
  });
  return null;
};

const DroneMap: React.FC<DroneMapProps> = ({
  initData,
  geoJsonData,
  exploredNodes,
  isCalculating,
  deliveryPathResult,
  pendingOrders = [],
  onMapClick,
}) => {
  const mapRef = useRef<any>(null);
  const [isAnimating, setIsAnimating] = useState(false);
  const [showStaticPath, setShowStaticPath] = useState(false);
  const [animationSpeed, setAnimationSpeed] = useState(1);
  const hasFittedBounds = useRef(false);

  const filteredGeoJson = useMemo(() => {
    if (!geoJsonData || !geoJsonData.features) return null;
    return {
      ...geoJsonData,
      features: geoJsonData.features.filter(
        (f: any) => f.geometry.type !== 'Polygon'
      )
    };
  }, [geoJsonData]);

  useEffect(() => {
    if (mapRef.current && geoJsonData && !hasFittedBounds.current) {
      try {
        const geoJsonLayer = L.geoJSON(geoJsonData);
        const bounds = geoJsonLayer.getBounds();
        if (bounds.isValid()) {
          setTimeout(() => {
            mapRef.current?.fitBounds(bounds, {
              padding: [50, 50],
              animate: false
            });
            hasFittedBounds.current = true;
          }, 100);
        }
      } catch (error) {
        console.error('Error fitting bounds:', error);
      }
    }
  }, [geoJsonData]);

  useEffect(() => {
    if (deliveryPathResult) {
      hasFittedBounds.current = false;
    }
  }, [deliveryPathResult]);

  useEffect(() => {
    const handleResize = () => {
      if (mapRef.current) {
        mapRef.current.invalidateSize();
      }
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  useEffect(() => {
    if (deliveryPathResult && !isCalculating && !isAnimating) {
      setShowStaticPath(false);
      const timer = setTimeout(() => {
        setIsAnimating(true);
      }, 150);
      return () => clearTimeout(timer);
    }

    if (!deliveryPathResult) {
      setIsAnimating(false);
      setShowStaticPath(false);
    }
  }, [deliveryPathResult, isCalculating]);

  const handleStartAnimation = useCallback(() => {
    setShowStaticPath(false);
    setIsAnimating(true);
  }, []);

  const handlePauseAnimation = useCallback(() => {
    setIsAnimating(false);
    setShowStaticPath(true);
  }, []);

  const handleResetAnimation = useCallback(() => {
    setIsAnimating(false);
    setShowStaticPath(false);
    setTimeout(() => {
      setIsAnimating(true);
    }, 100);
  }, []);

  const handleSpeedChange = useCallback((newSpeed: number) => {
    setAnimationSpeed(newSpeed);
  }, []);

  const handleAnimationComplete = useCallback(() => {
    console.log('✅ Animation completed!');
    setIsAnimating(false);
    setShowStaticPath(true);
  }, []);

  const geoJsonStyle = useCallback((feature: any) => {
    if (feature.geometry.type === 'LineString' || feature.geometry.type === 'MultiLineString') {
      return {
        color: feature.properties?.stroke || '#0000FF',
        weight: feature.properties?.['stroke-width'] || 3,
        opacity: feature.properties?.['stroke-opacity'] || 0.8,
      };
    }
    return {};
  }, []);

  const onEachFeature = useCallback((feature: any, layer: any) => {
    if (feature.properties && feature.properties.name) {
      layer.bindPopup(`
        <strong>${feature.properties.name}</strong>
        ${feature.properties.id ? `<br/>ID: ${feature.properties.id}` : ''}
      `);
    }
  }, []);

  const servicePointMarkers = useMemo(() => {
    if (!initData?.servicePoints) return null;

    return initData.servicePoints.map((sp: any, index: number) => {
      const lat = sp.location?.lat ?? sp.lat;
      const lng = sp.location?.lng ?? sp.lng;

      if (!lat || !lng) return null;

      return (
        <Marker
          key={`sp-${sp.id}-${index}`}
          position={[lat, lng]}
          icon={servicePointIcon}
        >
          <Popup>
            <strong>{sp.name}</strong>
            <br />
            Service Point ID: {sp.id}
            <br />
            Location: ({lng.toFixed(4)}, {lat.toFixed(4)})
          </Popup>
        </Marker>
      );
    });
  }, [initData?.servicePoints]);

  const pendingOrderMarkers = useMemo(() => {
    if (!pendingOrders || pendingOrders.length === 0) return null;

    return pendingOrders.map((order) => {
      if (!order.delivery?.lat || !order.delivery?.lng) return null;

      return (
        <CircleMarker
          key={`pending-order-${order.id}`}
          center={[order.delivery.lat, order.delivery.lng]}
          radius={8}
          pathOptions={{
            color: '#FFA500',
            fillColor: '#FFA500',
            fillOpacity: 0.7,
            weight: 2,
          }}
        >
          <Popup>
            <div style={{ minWidth: 140 }}>
              <strong>📦 Pending Order</strong>
              <br />
              <strong>Order ID:</strong> {order.id}
              <br />
              <strong>Date:</strong> {order.date}
              <br />
              <strong>Time:</strong> {order.time}
              <br />
              <strong>Capacity:</strong> {order.requirements?.capacity?.toFixed(2)} kg
              <br />
              {order.requirements?.cooling && (
                <>
                  <strong>🧊 Cooling:</strong> Required
                  <br />
                </>
              )}
              {order.requirements?.heating && (
                <>
                  <strong>🔥 Heating:</strong> Required
                  <br />
                </>
              )}
              {order.requirements?.maxCost && (
                <>
                  <strong>Max Cost:</strong> £{order.requirements.maxCost.toFixed(2)}
                  <br />
                </>
              )}
              <strong>Location:</strong> ({order.delivery.lng.toFixed(4)}, {order.delivery.lat.toFixed(4)})
            </div>
          </Popup>
        </CircleMarker>
      );
    });
  }, [pendingOrders]);

  return (
    <div
      style={{
        height: '100%',
        width: '100%',
        position: 'relative',
        overflow: 'hidden',
        transform: 'translateZ(0)',
        willChange: 'transform'
      }}
    >
      <MapContainer
        center={[55.9445, -3.1883]}
        zoom={13}
        style={{
          height: '100%',
          width: '100%',
          position: 'absolute',
          top: 0,
          left: 0,
          transform: 'translateZ(0)'
        }}
        ref={mapRef}
        scrollWheelZoom={true}
        zoomControl={true}
        preferCanvas={true}
      >
        <MapInitializer />
        <MapClickHandler onMapClick={onMapClick} />

        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
          url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
          maxZoom={20}
          subdomains="abcd"
          updateWhenIdle={true}
          updateWhenZooming={false}
        />

        {initData?.restrictedAreas && initData.restrictedAreas.length > 0 && (
          <RestrictedAreas areas={initData.restrictedAreas} showLabels={true} />
        )}

        {servicePointMarkers}
        {pendingOrderMarkers}

        {isAnimating && deliveryPathResult && (
          <DroneAnimation
            deliveryPath={deliveryPathResult}
            isAnimating={isAnimating}
            speed={animationSpeed}
            onAnimationComplete={handleAnimationComplete}
          />
        )}

        {showStaticPath && filteredGeoJson && (
          <GeoJSON
            key={JSON.stringify(geoJsonData)}
            data={filteredGeoJson}
            style={geoJsonStyle}
            onEachFeature={onEachFeature}
          />
        )}

        <PathAnimation
          progress={exploredNodes}
          isCalculating={isCalculating}
        />
      </MapContainer>

      {isCalculating && (
        <div
          style={{
            position: 'absolute',
            top: 10,
            right: 10,
            background: 'rgba(33, 150, 243, 0.9)',
            color: 'white',
            padding: '10px 15px',
            borderRadius: '5px',
            zIndex: 1000,
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
          }}
        >
          <div className="spinner"></div>
          <span>Calculating path...</span>
        </div>
      )}

      {isAnimating && deliveryPathResult && (
        <div
          style={{
            position: 'absolute',
            top: 10,
            right: 10,
            background: 'rgba(76, 175, 80, 0.9)',
            color: 'white',
            padding: '10px 15px',
            borderRadius: '5px',
            zIndex: 1000,
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
          }}
        >
          <span>🚁 Drones flying...</span>
        </div>
      )}

      <div
        style={{
          position: 'absolute',
          bottom: 20,
          right: 10,
          background: 'rgba(255, 255, 255, 0.95)',
          padding: '12px',
          borderRadius: '5px',
          zIndex: 1000,
          boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
          fontSize: '12px',
        }}
      >
        <div style={{ fontWeight: 'bold', marginBottom: 8 }}>Legend</div>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 4 }}>
          <div style={{ width: 20, height: 3, background: '#0000FF', marginRight: 8 }}></div>
          <span>Flight Path</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 4 }}>
          <div style={{ width: 20, height: 3, background: '#FF0000', marginRight: 8 }}></div>
          <span>Restricted Area</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 4 }}>
          <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#888', marginRight: 10, marginLeft: 6 }}></div>
          <span>Explored Node</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 4 }}>
          <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#52c41a', marginRight: 10, marginLeft: 6 }}></div>
          <span>Service Point</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 4 }}>
          <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#FFA500', marginRight: 10, marginLeft: 6 }}></div>
          <span>Pending Order</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#FF6B6B', marginRight: 10, marginLeft: 6 }}></div>
          <span>Delivery Point</span>
        </div>
      </div>

      {deliveryPathResult && (
        <div
          style={{
            position: 'absolute',
            top: 70,
            right: 10,
            width: 250,
            zIndex: 1000,
          }}
        >
          <AnimationControl
            isAnimating={isAnimating}
            speed={animationSpeed}
            hasPath={!!deliveryPathResult}
            onStart={handleStartAnimation}
            onPause={handlePauseAnimation}
            onReset={handleResetAnimation}
            onSpeedChange={handleSpeedChange}
          />
        </div>
      )}
    </div>
  );
};

export default DroneMap;