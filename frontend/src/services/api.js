import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const simulationAPI = {
  // Simulation control
  initialize: () => api.post('/simulation/initialize'),
  start: () => api.post('/simulation/start'),
  pause: () => api.post('/simulation/pause'),
  stop: () => api.post('/simulation/stop'),
  reset: () => api.post('/simulation/reset'),
  step: () => api.post('/simulation/step'),

  // Get status
  getStatus: () => api.get('/simulation/status'),
  getMetrics: () => api.get('/simulation/metrics'),
  getEfficiencyTrend: (count = 50) => api.get(`/simulation/efficiency/trend?count=${count}`),
  getGraph: () => api.get('/simulation/graph'),
  getVehicles: () => api.get('/simulation/vehicles'),

  // Traffic flow management
  createFlow: (flowRequest) => api.post('/simulation/flows', flowRequest),

  // Signal control
  getSignals: () => api.get('/simulation/signals'),
  setSignalMode: (mode) => api.post(`/simulation/signals/mode?mode=${mode}`),

  // Algorithm comparison
  compareAlgorithms: (startId, endId) =>
    api.get(`/simulation/pathfind/compare?start=${startId}&end=${endId}`),
};

export default api;
