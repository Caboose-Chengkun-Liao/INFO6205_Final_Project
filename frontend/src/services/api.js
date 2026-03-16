import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const simulationAPI = {
  // 仿真控制
  initialize: () => api.post('/simulation/initialize'),
  start: () => api.post('/simulation/start'),
  pause: () => api.post('/simulation/pause'),
  stop: () => api.post('/simulation/stop'),
  reset: () => api.post('/simulation/reset'),
  step: () => api.post('/simulation/step'),

  // 获取状态
  getStatus: () => api.get('/simulation/status'),
  getMetrics: () => api.get('/simulation/metrics'),
  getEfficiencyTrend: (count = 50) => api.get(`/simulation/efficiency/trend?count=${count}`),
  getGraph: () => api.get('/simulation/graph'),
  getVehicles: () => api.get('/simulation/vehicles'),

  // 交通流管理
  createFlow: (flowRequest) => api.post('/simulation/flows', flowRequest),

  // 信号灯控制
  getSignals: () => api.get('/simulation/signals'),
  setSignalMode: (mode) => api.post(`/simulation/signals/mode?mode=${mode}`),
};

export default api;
