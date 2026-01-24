import axios, { AxiosRequestConfig } from 'axios';
import type {
  InitData,
  MedDispatchRec,
  DeliveryPathResponse
} from '../types';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/v1';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 60000,
});

export const getInitData = async (): Promise<InitData> => {
  try {
    const [dronesRes, servicePointsRes, restrictedAreasRes] = await Promise.all([
      api.get('/drones'),
      api.get('/service-points'),
      api.get('/restricted-areas'),
    ]);

    return {
      drones: dronesRes.data,
      servicePoints: servicePointsRes.data,
      restrictedAreas: restrictedAreasRes.data,
      edges: [],
    };
  } catch (error) {
    console.error('Failed to fetch init data:', error);
    throw error;
  }
};

export const calculateDeliveryPath = async (
  orders: MedDispatchRec[],
  signal?: AbortSignal
): Promise<DeliveryPathResponse> => {
  try {
    const config: AxiosRequestConfig = {
      signal,
    };

    const response = await api.post('/calcDeliveryPath', orders, config);
    return response.data;
  } catch (error: any) {
    if (axios.isCancel(error)) {
      console.log('Delivery path calculation cancelled');
      throw new Error('CanceledError');
    }
    console.error('Failed to calculate delivery path:', error);
    throw error;
  }
};

export const calculateDeliveryPathAsGeoJson = async (
  orders: MedDispatchRec[],
  signal?: AbortSignal
): Promise<any> => {
  try {
    const config: AxiosRequestConfig = {
      signal,
    };

    const response = await api.post('/calcDeliveryPathAsGeoJson', orders, config);

    return typeof response.data === 'string'
      ? JSON.parse(response.data)
      : response.data;
  } catch (error: any) {
    if (axios.isCancel(error)) {
      console.log('GeoJSON path calculation cancelled');
      throw new Error('CanceledError');
    }
    console.error('Failed to calculate GeoJSON path:', error);
    throw error;
  }
};

export const queryAvailableDrones = async (
  orders: MedDispatchRec[]
): Promise<string[]> => {
  try {
    const response = await api.post('/queryAvailableDrones', orders);
    return response.data;
  } catch (error) {
    console.error('Failed to query available drones:', error);
    throw error;
  }
};

export const getWebSocketStatus = async (): Promise<{
  activeConnections: number;
  hasConnections: boolean;
  status: string;
}> => {
  try {
    const response = await api.get('/monitor/websocket-status');
    return response.data;
  } catch (error) {
    console.error('Failed to get WebSocket status:', error);
    throw error;
  }
};

export default api;