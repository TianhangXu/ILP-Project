import SockJS from 'sockjs-client';
import type { PathfindingProgress } from '../types';

const WS_BASE =
  import.meta.env.VITE_WS_BASE ||
  (typeof window !== 'undefined' ? window.location.origin : '');

export class WebSocketService {
  private socket: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 2000;
  private messageHandlers: Set<(message: PathfindingProgress) => void> = new Set();
  private connectionHandlers: Set<(connected: boolean) => void> = new Set();

  connect(): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      console.log('✅ WebSocket already connected');
      return;
    }

    try {
      console.log('🔌 Connecting to WebSocket...');

      const sockjs = new SockJS(`${WS_BASE}/ws/pathfinding-progress`);
      this.socket = sockjs as any;

      if (!this.socket) {
        console.error('❌ SockJS failed to create socket');
        this.attemptReconnect();
        return;
      }

      this.socket.onopen = () => {
        console.log('✅ WebSocket connected');
        this.reconnectAttempts = 0;
        this.notifyConnectionHandlers(true);
      };

      this.socket.onmessage = (event) => {
        try {
          const progress: PathfindingProgress = JSON.parse(event.data);
          console.log('📨 Received:', progress.type, progress.message);
          this.notifyMessageHandlers(progress);
        } catch (error) {
          console.error('❌ Failed to parse WebSocket message:', error);
        }
      };

      this.socket.onerror = (error) => {
        console.error('❌ WebSocket error:', error);
      };

      this.socket.onclose = () => {
        console.log('🔌 WebSocket disconnected');
        this.notifyConnectionHandlers(false);
        this.attemptReconnect();
      };

    } catch (error) {
      console.error('❌ Failed to establish WebSocket connection:', error);
      this.attemptReconnect();
    }
  }

  disconnect(): void {
    if (this.socket) {
      console.log('🔌 Disconnecting WebSocket...');
      this.socket.close();
      this.socket = null;
    }
  }

  private attemptReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('❌ Max reconnect attempts reached');
      return;
    }

    this.reconnectAttempts++;
    const delay = this.reconnectDelay * this.reconnectAttempts;
    console.log(`🔄 Reconnect attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts} in ${delay}ms...`);

    setTimeout(() => this.connect(), delay);
  }

  onMessage(handler: (message: PathfindingProgress) => void): () => void {
    this.messageHandlers.add(handler);
    return () => this.messageHandlers.delete(handler);
  }

  onConnectionChange(handler: (connected: boolean) => void): () => void {
    this.connectionHandlers.add(handler);
    return () => this.connectionHandlers.delete(handler);
  }

  private notifyMessageHandlers(message: PathfindingProgress): void {
    this.messageHandlers.forEach((handler) => {
      try {
        handler(message);
      } catch (error) {
        console.error('Error in message handler:', error);
      }
    });
  }

  private notifyConnectionHandlers(connected: boolean): void {
    this.connectionHandlers.forEach((handler) => {
      try {
        handler(connected);
      } catch (error) {
        console.error('Error in connection handler:', error);
      }
    });
  }

  isConnected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN;
  }
}

export const wsService = new WebSocketService();
