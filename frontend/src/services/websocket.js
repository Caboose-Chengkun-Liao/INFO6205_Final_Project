import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.connected = false;
    this.subscriptions = {};
  }

  connect(onConnect, onError) {
    const socket = new SockJS('http://localhost:8080/ws');
    this.stompClient = Stomp.over(socket);

    // Disable debug logging
    this.stompClient.debug = () => {};

    this.stompClient.connect(
      {},
      (frame) => {
        this.connected = true;
        console.log('WebSocket connected:', frame);
        if (onConnect) onConnect();
      },
      (error) => {
        this.connected = false;
        console.error('WebSocket connection failed:', error);
        if (onError) onError(error);
      }
    );
  }

  disconnect() {
    if (this.stompClient && this.connected) {
      this.stompClient.disconnect(() => {
        this.connected = false;
        console.log('WebSocket disconnected');
      });
    }
  }

  subscribe(topic, callback) {
    if (!this.connected) {
      console.error('WebSocket not connected');
      return null;
    }

    const subscription = this.stompClient.subscribe(topic, (message) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions[topic] = subscription;
    return subscription;
  }

  unsubscribe(topic) {
    if (this.subscriptions[topic]) {
      this.subscriptions[topic].unsubscribe();
      delete this.subscriptions[topic];
    }
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
