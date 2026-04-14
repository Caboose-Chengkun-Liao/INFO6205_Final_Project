import React, { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls } from '@react-three/drei';
import api from '../services/api';
import websocketService from '../services/websocket';
import { computeCoordBounds } from '../utils/mapUtils';

import GroundPlane from './three/GroundPlane';
import Roads3D from './three/Roads3D';
import Intersections3D from './three/Intersections3D';
import Vehicles3D from './three/Vehicles3D';
import TrafficLights3D from './three/TrafficLights3D';
import SceneLighting from './three/SceneLighting';

/** Auto-rotate camera around the scene */
const CameraAnimator = ({ active, radius = 16, height = 10, speed = 0.15 }) => {
  useFrame(({ camera, clock }) => {
    if (!active) return;
    const t = clock.getElapsedTime() * speed;
    camera.position.x = Math.sin(t) * radius;
    camera.position.z = Math.cos(t) * radius;
    camera.position.y = height + Math.sin(t * 0.5) * 2;
    camera.lookAt(0, 0, 0);
  });
  return null;
};

const TrafficScene3D = ({ signals = [] }) => {
  const [graphData, setGraphData] = useState(null);
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedNode, setSelectedNode] = useState(null);
  const [hoveredEdge, setHoveredEdge] = useState(null);
  const [hoveredNode, setHoveredNode] = useState(null);
  const [nightMode, setNightMode] = useState(false);
  const [flyAround, setFlyAround] = useState(false);
  const controlsRef = useRef();

  // Load graph data + poll vehicles via REST API
  useEffect(() => {
    loadGraphData();

    // Poll vehicle positions every second (REST API returns flat format)
    const vehicleInterval = setInterval(async () => {
      try {
        const response = await api.get('/simulation/vehicles');
        if (Array.isArray(response.data)) {
          setVehicles(response.data);
        }
      } catch (e) {
        // non-critical, skip
      }
    }, 1000);

    // Refresh graph data (edge loads change) every 3 seconds
    const graphRefreshInterval = setInterval(() => loadGraphData(false), 3000);

    return () => {
      clearInterval(vehicleInterval);
      clearInterval(graphRefreshInterval);
    };
  }, []);

  const loadGraphData = async (showLoading = true) => {
    try {
      if (showLoading) setLoading(true);
      const response = await api.get('/simulation/graph');
      setGraphData(response.data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to load map data');
    } finally {
      if (showLoading) setLoading(false);
    }
  };

  // Coordinate bounds and transform
  const coordBounds = useMemo(() => {
    if (!graphData?.nodes) return null;
    return computeCoordBounds(graphData.nodes);
  }, [graphData]);

  const to3D = useCallback((x, y) => {
    if (!coordBounds) return [0, 0, 0];
    const cx = (coordBounds.minX + coordBounds.maxX) / 2;
    const cy = (coordBounds.minY + coordBounds.maxY) / 2;
    return [x - cx, 0, -(y - cy)];
  }, [coordBounds]);

  // O(1) node lookup map
  const nodeMap = useMemo(() => {
    if (!graphData?.nodes) return new Map();
    return new Map(graphData.nodes.map(n => [String(n.id), n]));
  }, [graphData]);

  if (loading) {
    return (
      <div style={styles.container}>
        <div style={styles.loading}>Loading Arlington Traffic Network (3D)...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.container}>
        <div style={styles.error}>{error}</div>
        <button onClick={() => loadGraphData()} style={styles.retryBtn}>Retry</button>
      </div>
    );
  }

  const bgColor = nightMode ? '#0a0a1a' : '#f0f0f0';

  return (
    <div style={styles.container}>
      {/* Header bar */}
      <div style={styles.header}>
        <div style={styles.headerTitle}>
          3D Traffic Network — Arlington, VA
        </div>
        <div style={styles.headerControls}>
          <span style={styles.stat}>{graphData?.nodes?.length || 0} nodes</span>
          <span style={styles.stat}>{graphData?.edges?.length || 0} edges</span>
          <span style={styles.stat}>{vehicles.length} flows</span>
          <button
            onClick={() => setFlyAround(!flyAround)}
            style={{
              ...styles.toggleBtn,
              background: flyAround ? '#30D158' : '#86868B',
              color: '#fff',
            }}
          >
            {flyAround ? 'Stop Fly' : 'Fly Around'}
          </button>
          <button
            onClick={() => setNightMode(!nightMode)}
            style={{
              ...styles.toggleBtn,
              background: nightMode ? '#FFD60A' : '#1D1D1F',
              color: nightMode ? '#000' : '#fff',
            }}
          >
            {nightMode ? 'Day' : 'Night'}
          </button>
        </div>
      </div>

      {/* 3D Canvas */}
      <Canvas
        camera={{ position: [0, 16, 12], fov: 50 }}
        shadows
        style={{ background: bgColor }}
      >
        <CameraAnimator active={flyAround} />
        <SceneLighting nightMode={nightMode} />
        <GroundPlane nightMode={nightMode} />

        {graphData && (
          <>
            <Roads3D
              edges={graphData.edges}
              nodeMap={nodeMap}
              to3D={to3D}
              hoveredEdge={hoveredEdge}
              onHoverEdge={setHoveredEdge}
            />
            <Intersections3D
              nodes={graphData.nodes}
              to3D={to3D}
              selectedNode={selectedNode}
              hoveredNode={hoveredNode}
              onSelectNode={setSelectedNode}
              onHoverNode={setHoveredNode}
            />
            <TrafficLights3D
              signals={signals}
              nodes={graphData.nodes}
              nodeMap={nodeMap}
              to3D={to3D}
              nightMode={nightMode}
            />
            <Vehicles3D
              vehicles={vehicles}
              nodeMap={nodeMap}
              to3D={to3D}
              nightMode={nightMode}
            />
          </>
        )}

        <OrbitControls
          enableDamping
          dampingFactor={0.1}
          maxPolarAngle={Math.PI / 2.1}
          minDistance={3}
          maxDistance={35}
          enableZoom={false}
        />
      </Canvas>

      {/* Selected node detail panel */}
      {selectedNode && (
        <div style={styles.detailPanel}>
          <div style={styles.detailHeader}>
            <strong>{selectedNode.name}</strong>
            <button onClick={() => setSelectedNode(null)} style={styles.closeBtn}>x</button>
          </div>
          <div style={styles.detailRow}>ID: {selectedNode.id}</div>
          <div style={styles.detailRow}>Type: {selectedNode.isIntersection ? 'Intersection' : 'Boundary'}</div>
          <div style={styles.detailRow}>Position: ({selectedNode.x}, {selectedNode.y})</div>
        </div>
      )}
    </div>
  );
};

const styles = {
  container: {
    position: 'relative',
    width: '100%',
    height: '700px',
    borderRadius: 12,
    overflow: 'hidden',
    background: '#f0f0f0',
  },
  header: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 10,
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '10px 16px',
    background: 'rgba(255,255,255,0.85)',
    backdropFilter: 'saturate(180%) blur(12px)',
    borderBottom: '1px solid rgba(0,0,0,0.08)',
  },
  headerTitle: {
    fontWeight: 600,
    fontSize: 14,
    color: '#1D1D1F',
  },
  headerControls: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
  },
  stat: {
    fontSize: 12,
    color: '#86868B',
  },
  toggleBtn: {
    border: 'none',
    borderRadius: 6,
    padding: '4px 12px',
    fontSize: 12,
    fontWeight: 600,
    cursor: 'pointer',
  },
  loading: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '100%',
    color: '#86868B',
    fontSize: 16,
  },
  error: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '80%',
    color: '#FF453A',
    fontSize: 14,
  },
  retryBtn: {
    position: 'absolute',
    bottom: 20,
    left: '50%',
    transform: 'translateX(-50%)',
    padding: '8px 20px',
    border: 'none',
    borderRadius: 8,
    background: '#0071E3',
    color: '#fff',
    cursor: 'pointer',
    fontWeight: 600,
  },
  detailPanel: {
    position: 'absolute',
    bottom: 16,
    left: 16,
    zIndex: 10,
    background: 'rgba(255,255,255,0.92)',
    backdropFilter: 'saturate(180%) blur(16px)',
    borderRadius: 10,
    padding: '12px 16px',
    minWidth: 200,
    boxShadow: '0 4px 24px rgba(0,0,0,0.12)',
  },
  detailHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
    fontSize: 13,
  },
  detailRow: {
    fontSize: 12,
    color: '#555',
    marginTop: 3,
  },
  closeBtn: {
    background: 'none',
    border: 'none',
    fontSize: 14,
    cursor: 'pointer',
    color: '#999',
    padding: '0 4px',
  },
};

export default TrafficScene3D;
