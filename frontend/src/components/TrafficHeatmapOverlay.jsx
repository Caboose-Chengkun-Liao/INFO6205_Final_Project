import React, { useState, useEffect } from 'react';
import api from '../services/api';
import websocketService from '../services/websocket';

/**
 * Traffic Heatmap Overlay
 * Visualize road congestion levels with color gradients
 */
const TrafficHeatmapOverlay = ({ graphData }) => {
  const [isEnabled, setIsEnabled] = useState(true);
  const [congestionData, setCongestionData] = useState({});
  const [showLegend, setShowLegend] = useState(true);
  const [heatmapMode, setHeatmapMode] = useState('congestion'); // congestion, speed, flow

  useEffect(() => {
    if (!graphData) return;

    // Load initial congestion data
    updateCongestionData();

    // Subscribe to real-time updates
    const unsubscribe = websocketService.subscribe('/topic/simulation', (data) => {
      if (data.edges) {
        updateCongestionFromEdges(data.edges);
      }
    });

    // Refresh periodically
    const interval = setInterval(updateCongestionData, 2000);

    return () => {
      if (unsubscribe) unsubscribe();
      clearInterval(interval);
    };
  }, [graphData]);

  const updateCongestionData = async () => {
    try {
      const response = await api.get('/simulation/graph');
      if (response.data && response.data.edges) {
        updateCongestionFromEdges(response.data.edges);
      }
    } catch (error) {
      console.error('Failed to update congestion data:', error);
    }
  };

  const updateCongestionFromEdges = (edges) => {
    const newData = {};

    edges.forEach(edge => {
      const capacity = edge.distance * (edge.capacityPerKm || 50);
      const load = edge.currentVehicleCount || 0;
      const congestionLevel = capacity > 0 ? (load / capacity) * 100 : 0;

      newData[edge.id] = {
        congestion: Math.min(congestionLevel, 100),
        speed: edge.currentSpeed || edge.speedLimit || 60,
        flow: load,
        capacity: capacity
      };
    });

    setCongestionData(newData);
  };

  const getCongestionColor = (level) => {
    if (level < 25) return '#10B981'; // Green - Free flow
    if (level < 50) return '#84CC16'; // Light green - Light traffic
    if (level < 70) return '#F59E0B'; // Amber - Moderate congestion
    if (level < 85) return '#EF4444'; // Red - Heavy congestion
    return '#991B1B'; // Dark red - Severe congestion
  };

  const getSpeedColor = (speed, speedLimit = 60) => {
    const ratio = speed / speedLimit;
    if (ratio > 0.8) return '#10B981'; // Green - Fast
    if (ratio > 0.6) return '#84CC16'; // Light green - Moderate
    if (ratio > 0.4) return '#F59E0B'; // Amber - Slow
    if (ratio > 0.2) return '#EF4444'; // Red - Very slow
    return '#991B1B'; // Dark red - Stopped
  };

  const getFlowColor = (flow, capacity) => {
    const ratio = flow / capacity;
    if (ratio < 0.3) return '#10B981'; // Green - Low volume
    if (ratio < 0.6) return '#84CC16'; // Light green - Medium volume
    if (ratio < 0.8) return '#F59E0B'; // Amber - High volume
    return '#EF4444'; // Red - Very high volume
  };

  const getEdgeColor = (edgeId) => {
    const data = congestionData[edgeId];
    if (!data) return '#94A3B8'; // Default gray

    switch (heatmapMode) {
      case 'congestion':
        return getCongestionColor(data.congestion);
      case 'speed':
        return getSpeedColor(data.speed);
      case 'flow':
        return getFlowColor(data.flow, data.capacity);
      default:
        return '#94A3B8';
    }
  };

  const getEdgeWidth = (edgeId) => {
    const data = congestionData[edgeId];
    if (!data || !isEnabled) return 3;

    // Make congested roads thicker for emphasis
    if (heatmapMode === 'congestion') {
      if (data.congestion > 70) return 6;
      if (data.congestion > 50) return 5;
      return 4;
    }

    return 4;
  };

  const modes = [
    { id: 'congestion', label: 'Congestion', icon: '🚦' },
    { id: 'speed', label: 'Speed', icon: '🏃' },
    { id: 'flow', label: 'Flow Volume', icon: '🚗' }
  ];

  const legends = {
    congestion: [
      { color: '#10B981', label: 'Free Flow (<25%)' },
      { color: '#84CC16', label: 'Light (25-50%)' },
      { color: '#F59E0B', label: 'Moderate (50-70%)' },
      { color: '#EF4444', label: 'Heavy (70-85%)' },
      { color: '#991B1B', label: 'Severe (>85%)' }
    ],
    speed: [
      { color: '#10B981', label: 'Fast (>80%)' },
      { color: '#84CC16', label: 'Moderate (60-80%)' },
      { color: '#F59E0B', label: 'Slow (40-60%)' },
      { color: '#EF4444', label: 'Very Slow (20-40%)' },
      { color: '#991B1B', label: 'Stopped (<20%)' }
    ],
    flow: [
      { color: '#10B981', label: 'Low (<30%)' },
      { color: '#84CC16', label: 'Medium (30-60%)' },
      { color: '#F59E0B', label: 'High (60-80%)' },
      { color: '#EF4444', label: 'Very High (>80%)' }
    ]
  };

  if (!graphData || !isEnabled) return null;

  return (
    <>
      {/* Heatmap Overlay on Map */}
      <div style={styles.overlay}>
        <svg
          width="100%"
          height="100%"
          style={styles.svg}
          viewBox="0 0 1400 900"
        >
          {/* Render edges with heatmap colors */}
          {graphData.edges && graphData.edges.map((edge) => {
            const fromNode = graphData.nodes.find(n => n.id === edge.fromNode?.id);
            const toNode = graphData.nodes.find(n => n.id === edge.toNode?.id);

            if (!fromNode || !toNode) return null;

            return (
              <g key={edge.id}>
                <line
                  x1={fromNode.x}
                  y1={fromNode.y}
                  x2={toNode.x}
                  y2={toNode.y}
                  stroke={getEdgeColor(edge.id)}
                  strokeWidth={getEdgeWidth(edge.id)}
                  strokeOpacity={0.8}
                  style={styles.edge}
                />
              </g>
            );
          })}
        </svg>
      </div>

      {/* Control Panel */}
      <div style={styles.controlPanel}>
        <div style={styles.panelHeader}>
          <span style={styles.panelTitle}>🌡️ Traffic Heatmap</span>
          <button
            onClick={() => setIsEnabled(!isEnabled)}
            style={{
              ...styles.toggleButton,
              background: isEnabled ? '#10B981' : '#6B7280'
            }}
          >
            {isEnabled ? 'ON' : 'OFF'}
          </button>
        </div>

        {isEnabled && (
          <>
            {/* Mode Selector */}
            <div style={styles.modeSelector}>
              {modes.map(mode => (
                <button
                  key={mode.id}
                  onClick={() => setHeatmapMode(mode.id)}
                  style={{
                    ...styles.modeButton,
                    background: heatmapMode === mode.id ? '#667eea' : '#F3F4F6',
                    color: heatmapMode === mode.id ? 'white' : '#6B7280'
                  }}
                >
                  <span>{mode.icon}</span>
                  <span>{mode.label}</span>
                </button>
              ))}
            </div>

            {/* Legend Toggle */}
            <button
              onClick={() => setShowLegend(!showLegend)}
              style={styles.legendToggle}
            >
              {showLegend ? '📖 Hide Legend' : '📖 Show Legend'}
            </button>
          </>
        )}
      </div>

      {/* Legend */}
      {isEnabled && showLegend && (
        <div style={styles.legend}>
          <div style={styles.legendTitle}>
            {modes.find(m => m.id === heatmapMode)?.label} Legend
          </div>
          <div style={styles.legendItems}>
            {legends[heatmapMode].map((item, index) => (
              <div key={index} style={styles.legendItem}>
                <div
                  style={{
                    ...styles.legendColor,
                    background: item.color
                  }}
                ></div>
                <span style={styles.legendLabel}>{item.label}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* CSS Animations */}
      <style>{`
        @keyframes pulse-edge {
          0%, 100% { stroke-opacity: 0.8; }
          50% { stroke-opacity: 1; }
        }
      `}</style>
    </>
  );
};

const styles = {
  overlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    pointerEvents: 'none',
    zIndex: 1
  },
  svg: {
    width: '100%',
    height: '100%'
  },
  edge: {
    transition: 'stroke 0.5s, stroke-width 0.3s',
    animation: 'pulse-edge 3s ease-in-out infinite'
  },
  controlPanel: {
    position: 'absolute',
    top: '20px',
    right: '20px',
    background: 'white',
    borderRadius: '12px',
    padding: '16px',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
    minWidth: '280px',
    zIndex: 10
  },
  panelHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '12px'
  },
  panelTitle: {
    fontSize: '16px',
    fontWeight: '700',
    color: '#1F2937'
  },
  toggleButton: {
    padding: '6px 16px',
    border: 'none',
    borderRadius: '6px',
    color: 'white',
    fontSize: '12px',
    fontWeight: '700',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  modeSelector: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    marginBottom: '12px'
  },
  modeButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '10px 12px',
    border: 'none',
    borderRadius: '8px',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  legendToggle: {
    width: '100%',
    padding: '8px',
    background: '#F3F4F6',
    border: 'none',
    borderRadius: '6px',
    fontSize: '12px',
    fontWeight: '600',
    color: '#6B7280',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  legend: {
    position: 'absolute',
    bottom: '20px',
    right: '20px',
    background: 'white',
    borderRadius: '12px',
    padding: '16px',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
    minWidth: '220px',
    zIndex: 10
  },
  legendTitle: {
    fontSize: '14px',
    fontWeight: '700',
    color: '#1F2937',
    marginBottom: '12px'
  },
  legendItems: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px'
  },
  legendItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px'
  },
  legendColor: {
    width: '20px',
    height: '20px',
    borderRadius: '4px',
    border: '1px solid rgba(0, 0, 0, 0.1)'
  },
  legendLabel: {
    fontSize: '12px',
    color: '#6B7280',
    fontWeight: '500'
  }
};

export default TrafficHeatmapOverlay;
