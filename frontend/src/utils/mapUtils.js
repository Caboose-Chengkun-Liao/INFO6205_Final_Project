/**
 * Shared map coordinate and color utilities
 * Used by MapVisualization and AlgorithmComparison
 */

export function computeCoordBounds(nodes) {
  if (!nodes || nodes.length === 0) return null;
  const xCoords = nodes.map(n => n.x);
  const yCoords = nodes.map(n => n.y);
  return {
    minX: Math.min(...xCoords),
    maxX: Math.max(...xCoords),
    minY: Math.min(...yCoords),
    maxY: Math.max(...yCoords),
  };
}

export function mapToSVG(x, y, bounds, width, height, padding) {
  if (!bounds) return { x: 0, y: 0 };
  const { minX, maxX, minY, maxY } = bounds;
  const rangeX = maxX - minX || 1;
  const rangeY = maxY - minY || 1;

  const svgX = padding + ((x - minX) / rangeX) * (width - 2 * padding);
  const svgY = height - padding - ((y - minY) / rangeY) * (height - 2 * padding);

  return { x: svgX, y: svgY };
}

// Apple-style color palette
export const colors = {
  intersection: '#0071E3',
  boundary: '#86868B',
  roadNormal: '#D1D1D6',
  roadMedium: '#FF9F0A',
  roadHeavy: '#FF453A',
  vehicleActive: '#30D158',
  vehicleBlocked: '#FF453A',
  vehicleWaiting: '#FF9F0A',
  background: '#F5F5F7',
  gridLines: 'rgba(0,0,0,0.03)',
  text: '#1D1D1F',
  textSecondary: '#86868B',
  signalRed: '#FF453A',
  signalYellow: '#FFD60A',
  signalGreen: '#30D158',
  signalOff: '#3A3A3C',
};

// Smooth 5-step traffic color gradient with interpolation
const trafficStops = [
  { at: 0.0, r: 199, g: 199, b: 204 }, // #C7C7CC light gray
  { at: 0.2, r: 134, g: 184, b: 240 }, // #86B8F0 light blue
  { at: 0.4, r: 255, g: 214, b: 10  }, // #FFD60A yellow
  { at: 0.6, r: 255, g: 159, b: 10  }, // #FF9F0A orange
  { at: 0.8, r: 255, g: 69,  b: 58  }, // #FF453A red
];

function toHex(n) {
  return n.toString(16).padStart(2, '0');
}

function lerpColor(ratio) {
  const t = Math.max(0, Math.min(1, ratio));
  for (let i = 0; i < trafficStops.length - 1; i++) {
    const a = trafficStops[i], b = trafficStops[i + 1];
    if (t >= a.at && t <= b.at) {
      const f = (t - a.at) / (b.at - a.at);
      const r = Math.round(a.r + (b.r - a.r) * f);
      const g = Math.round(a.g + (b.g - a.g) * f);
      const bl = Math.round(a.b + (b.b - a.b) * f);
      return `#${toHex(r)}${toHex(g)}${toHex(bl)}`;
    }
  }
  const last = trafficStops[trafficStops.length - 1];
  return `#${toHex(last.r)}${toHex(last.g)}${toHex(last.b)}`;
}

export function getRoadColor(edge) {
  if (!edge.distance) return colors.roadNormal;
  const capacity = edge.distance * 50;
  const loadRatio = (edge.currentLoad || 0) / capacity;
  return lerpColor(loadRatio);
}

export function getVehicleColor(state) {
  switch (state) {
    case 'BLOCKED': return colors.vehicleBlocked;
    case 'ACTIVE': return colors.vehicleActive;
    case 'WAITING': return colors.vehicleWaiting;
    default: return colors.vehicleActive;
  }
}

export function getSignalColor(state, type) {
  if (state === type) {
    switch (type) {
      case 'RED': return colors.signalRed;
      case 'YELLOW': return colors.signalYellow;
      case 'GREEN': return colors.signalGreen;
      default: return colors.signalOff;
    }
  }
  return colors.signalOff;
}
