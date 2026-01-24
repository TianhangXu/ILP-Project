export interface Position {
  lat: number;
  lng: number;
}

export interface Vertex {
  lat: number;
  lng: number;
}

export interface Drone {
  id: string;
  name: string;
  capacity: number;
  cooling?: boolean;
  heating?: boolean;
  [key: string]: any;
}

export interface ServicePoint {
  id: string;
  name: string;
  lat: number;
  lng: number;
  [key: string]: any;
}

export interface RestrictedArea {
  id: string;
  name: string;
  vertices: Vertex[];
  limits?: {
    lower: number;
    upper: number;
  };
  [key: string]: any;
}

export interface InitData {
  drones?: Drone[];
  servicePoints: ServicePoint[];
  restrictedAreas?: RestrictedArea[];
  edges: any[];
  [key: string]: any;
}

export interface MedDispatchRec {
  id?: number;
  orderId?: string;
  date?: string;
  time?: string;
  from?: string;
  to?: string;
  priority?: number;
  requirements?: {
    capacity?: number;
    cooling?: boolean;
    heating?: boolean;
    maxCost?: number;
    [key: string]: any;
  };
  delivery?: {
    lng: number;
    lat: number;
  };
  [key: string]: any;
}


export interface PathfindingProgress {
  type: 'calculation_start'
      | 'node_explored'
      | 'path_found'
      | 'calculation_complete'
      | 'error'
      | 'warning'
      | 'no_solution'
      | 'batch_completed';
  timestamp?: string;
  message?: string;
  position?: {
    lng: number;
    lat: number;
  };
  cost?: number;
  totalCost?: number;
  droneId?: string;
  orderId?: string;
  batchIndex?: number;
}


export interface FlightPath {
  lat: number;
  lng: number;
  altitude?: number;
  [key: string]: any;
}

export interface Delivery {
  deliveryId: string | null;
  orderId?: string;
  flightPath: FlightPath[];
  cost?: number;
  [key: string]: any;
}

export interface DronePath {
  droneId: string;
  deliveries: Delivery[];
  totalCost?: number;
  [key: string]: any;
}

export interface DeliveryPathResponse {
  totalCost: number;
  totalMoves: number;
  dronePaths: DronePath[];
  [key: string]: any;
}

export interface WebSocketMessage {
  event: string;
  data: any;
  timestamp?: number;
}

export type GeoJSONFeature = any;
export type GeoJSONFeatureCollection = any;