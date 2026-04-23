import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

/**
 * WebSocket service - with exponential backoff auto-reconnect
 */
class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.connected = false;
    this.subscriptions = {};
    this.pendingSubscriptions = []; // subscriptions to restore after reconnect
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 10;
    this.reconnectTimer = null;
    this.onConnectCallback = null;
    this.onErrorCallback = null;
    this.onReconnectCallback = null;
  }

  connect(onConnect, onError) {
    this.onConnectCallback = onConnect;
    this.onErrorCallback = onError;
    this._doConnect();
  }

  _doConnect() {
    try {
      // Stomp.over requires a factory function (not a socket instance) to support auto-reconnect
      this.stompClient = Stomp.over(() => new SockJS(WS_URL));

      // Disable STOMP debug logging
      this.stompClient.debug = () => {};

      this.stompClient.connect(
        {},
        (frame) => {
          this.connected = true;
          this.reconnectAttempts = 0; // reset reconnect counter
          console.log('WebSocket connected successfully');

          // Restore previous subscriptions
          this._restoreSubscriptions();

          if (this.onConnectCallback) this.onConnectCallback();
        },
        (error) => {
          this.connected = false;
          console.error('WebSocket connection failed:', error);

          if (this.onErrorCallback) this.onErrorCallback(error);

          // Attempt reconnect
          this._scheduleReconnect();
        }
      );

    } catch (error) {
      console.error('WebSocket creation failed:', error);
      this._scheduleReconnect();
    }
  }

  /**
   * Exponential backoff reconnect
   * Delays: 1s, 2s, 4s, 8s, 16s, 30s (max)
   */
  _scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error(`WebSocket reconnect failed: maximum attempts reached (${this.maxReconnectAttempts})`);
      return;
    }

    const delay = Math.min(
      1000 * Math.pow(2, this.reconnectAttempts),
      30000 // maximum 30 seconds
    );

    this.reconnectAttempts++;
    console.log(`WebSocket reconnecting in ${delay / 1000}s (attempt ${this.reconnectAttempts})...`);

    this.reconnectTimer = setTimeout(() => {
      this._doConnect();
    }, delay);
  }

  /**
   * Restore all subscriptions after reconnect
   */
  _restoreSubscriptions() {
    for (const { topic, callback } of this.pendingSubscriptions) {
      this._doSubscribe(topic, callback);
    }
  }

  disconnect() {
    // Stop reconnect timer
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.stompClient && this.connected) {
      this.stompClient.disconnect(() => {
        this.connected = false;
        this.subscriptions = {};
        this.pendingSubscriptions = [];
        console.log('WebSocket disconnected');
      });
    }
  }

  subscribe(topic, callback) {
    // Record the subscription so it can be restored after reconnect
    const exists = this.pendingSubscriptions.some(s => s.topic === topic);
    if (!exists) {
      this.pendingSubscriptions.push({ topic, callback });
    }

    if (this.connected) {
      this._doSubscribe(topic, callback);
    }

    // Always return an idempotent unsubscribe function so callers can use it
    // as a useEffect cleanup. Safe to call even if not yet connected.
    return () => this.unsubscribe(topic);
  }

  _doSubscribe(topic, callback) {
    // Cancel any existing subscription on this topic first
    if (this.subscriptions[topic]) {
      try {
        this.subscriptions[topic].unsubscribe();
      } catch (e) {
        // ignore
      }
    }

    const subscription = this.stompClient.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (e) {
        console.error('WebSocket message parse failed:', e);
      }
    });

    this.subscriptions[topic] = subscription;
    return subscription;
  }

  unsubscribe(topic) {
    if (this.subscriptions[topic]) {
      this.subscriptions[topic].unsubscribe();
      delete this.subscriptions[topic];
    }
    this.pendingSubscriptions = this.pendingSubscriptions.filter(s => s.topic !== topic);
  }

  send(destination, data) {
    if (this.stompClient && this.connected) {
      this.stompClient.send(destination, {}, JSON.stringify(data));
    }
  }

  isConnected() {
    return this.connected;
  }
}

export default new WebSocketService();
