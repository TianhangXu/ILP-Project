export interface Position {
  lng: number;
  lat: number;
}

export interface MedDispatchRec {
  id: number;
  date: string;
  time: string;
  requirements: {
    capacity: number;
    cooling?: boolean;
    heating?: boolean;
    maxCost?: number;
  };
  delivery: Position;
}

export interface Drone {
  id: string;
  name: string;
  capability: {
    cooling: boolean;
    heating: boolean;
    capacity: number;
    maxMoves: number;
    costPerMove: number;
    costInitial: number;
    costFinal: number;
  };
}

export interface DroneServicePoint {
  name: string;
  id: number;
  location: {
    lng: number;
    lat: number;
    alt: number;
  };
}

export interface RestrictedArea {
  name: string;
  id: number;
  limits: {
    lower: number;
    upper: number;
  };
  vertices: Position[];
}

export interface DeliveryPathResponse {
  totalCost: number;
  totalMoves: number;
  dronePaths: DronePath[];
}

export interface DronePath {
  droneId: string;
  deliveries: Delivery[];
}

export interface Delivery {
  deliveryId: number | null;
  flightPath: Position[];
}


export interface PathfindingProgress {
  type: 'node_explored' | 'path_found' | 'batch_started' |
        'batch_completed' | 'delivery_started' | 'delivery_completed' |
        'connection_established' | 'error';
  position?: Position;
  deliveryId?: number;
  totalNodes?: number;
  currentBatch?: number;
  droneId?: string;
  message: string;
}


export interface InitData {
  drones: Drone[];
  servicePoints: DroneServicePoint[];
  restrictedAreas: RestrictedArea[];
}

export interface CalculationState {
  isCalculating: boolean;
  progress: PathfindingProgress[];
  result: DeliveryPathResponse | null;
  error: string | null;
}

export interface GeoJsonFeature {
  type: 'Feature';
  geometry: {
    type: 'LineString' | 'Polygon' | 'Point' | 'MultiLineString';
    coordinates: number[][][] | number[][] | number[];
  };
  properties: {
    name?: string;
    id?: string | number;
    stroke?: string;
    'stroke-width'?: number;
    'stroke-opacity'?: number;
    [key: string]: any;
  };
}

export interface GeoJsonFeatureCollection {
  type: 'FeatureCollection';
  features: GeoJsonFeature[];
}