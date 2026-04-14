import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

/**
 * WebSocket 服务 - 带指数退避自动重连
 */
class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.connected = false;
    this.subscriptions = {};
    this.pendingSubscriptions = []; // 重连后需要恢复的订阅
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
      // Stomp.over 需要传入 factory 函数（不是 socket 实例）以支持自动重连
      this.stompClient = Stomp.over(() => new SockJS(WS_URL));

      // 禁用 STOMP 调试日志
      this.stompClient.debug = () => {};

      this.stompClient.connect(
        {},
        (frame) => {
          this.connected = true;
          this.reconnectAttempts = 0; // 重置重连计数
          console.log('WebSocket连接成功');

          // 恢复之前的订阅
          this._restoreSubscriptions();

          if (this.onConnectCallback) this.onConnectCallback();
        },
        (error) => {
          this.connected = false;
          console.error('WebSocket连接失败:', error);

          if (this.onErrorCallback) this.onErrorCallback(error);

          // 尝试重连
          this._scheduleReconnect();
        }
      );

    } catch (error) {
      console.error('WebSocket创建失败:', error);
      this._scheduleReconnect();
    }
  }

  /**
   * 指数退避重连
   * 延迟: 1s, 2s, 4s, 8s, 16s, 30s(max)
   */
  _scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error(`WebSocket重连失败，已达最大尝试次数 (${this.maxReconnectAttempts})`);
      return;
    }

    const delay = Math.min(
      1000 * Math.pow(2, this.reconnectAttempts),
      30000 // 最大30秒
    );

    this.reconnectAttempts++;
    console.log(`WebSocket将在 ${delay / 1000}s 后重连 (第${this.reconnectAttempts}次)...`);

    this.reconnectTimer = setTimeout(() => {
      this._doConnect();
    }, delay);
  }

  /**
   * 重连后恢复所有订阅
   */
  _restoreSubscriptions() {
    for (const { topic, callback } of this.pendingSubscriptions) {
      this._doSubscribe(topic, callback);
    }
  }

  disconnect() {
    // 停止重连
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.stompClient && this.connected) {
      this.stompClient.disconnect(() => {
        this.connected = false;
        this.subscriptions = {};
        this.pendingSubscriptions = [];
        console.log('WebSocket已断开');
      });
    }
  }

  subscribe(topic, callback) {
    // 记录订阅以便重连后恢复
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
    // 先取消旧订阅
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
        console.error('WebSocket消息解析失败:', e);
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
