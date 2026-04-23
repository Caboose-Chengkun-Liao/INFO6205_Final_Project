import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Canvas } from '@react-three/fiber';
import { OrbitControls } from '@react-three/drei';
import api from '../../services/api';
import { computeCoordBounds } from '../../utils/mapUtils';

import GroundPlane from './GroundPlane';
import Roads3D from './Roads3D';
import Intersections3D from './Intersections3D';
import Vehicles3D from './Vehicles3D';
import TrafficLights3D from './TrafficLights3D';
import SceneLighting from './SceneLighting';

/**
 * Compact 3D scene for side-by-side comparison.
 * Fetches data from /api/compare/{index}/... endpoints.
 */
const Mini3DScene = ({ index, label, color, running }) => {
  const [graphData, setGraphData] = useState(null);
  const [vehicles, setVehicles] = useState([]);
  const [signals, setSignals] = useState([]);

  // Load graph + poll vehicles + signals
  useEffect(() => {
    if (!running) return;

    const loadGraph = async () => {
      try {
        const res = await api.get(`/compare/${index}/graph`);
        setGraphData(res.data);
      } catch (e) { /* not started yet */ }
    };
    loadGraph();

    const vehicleInterval = setInterval(async () => {
      try {
        const [vRes, sRes] = await Promise.all([
          api.get(`/compare/${index}/vehicles`),
          api.get(`/compare/${index}/signals`),
        ]);
        if (Array.isArray(vRes.data)) setVehicles(vRes.data);
        if (Array.isArray(sRes.data)) setSignals(sRes.data);
      } catch (e) { /* ignore */ }
    }, 1000);

    const graphInterval = setInterval(loadGraph, 3000);

    return () => {
      clearInterval(vehicleInterval);
      clearInterval(graphInterval);
    };
  }, [index, running]);

  const coordBounds = useMemo(() => {
    if (!graphData?.nodes) return null;
    return computeCoordBounds(graphData.nodes);
  }, [graphData]);

  const groundSize = useMemo(() => {
    if (!coordBounds) return { width: 18.66, depth: 16.19 };
    return {
      width: coordBounds.maxX - coordBounds.minX,
      depth: coordBounds.maxY - coordBounds.minY,
    };
  }, [coordBounds]);

  const to3D = useCallback((x, y) => {
    if (!coordBounds) return [0, 0, 0];
    const cx = (coordBounds.minX + coordBounds.maxX) / 2;
    const cy = (coordBounds.minY + coordBounds.maxY) / 2;
    return [x - cx, 0, -(y - cy)];
  }, [coordBounds]);

  const nodeMap = useMemo(() => {
    if (!graphData?.nodes) return new Map();
    return new Map(graphData.nodes.map(n => [String(n.id), n]));
  }, [graphData]);

  return (
    <div style={{ position: 'relative', height: '100%', borderRadius: 8, overflow: 'hidden' }}>
      {/* Mode label */}
      <div style={{
        position: 'absolute', top: 0, left: 0, right: 0, zIndex: 10,
        padding: '6px 12px',
        background: color,
        color: '#fff',
        fontWeight: 700,
        fontSize: 13,
        textAlign: 'center',
        letterSpacing: 0.5,
      }}>
        {label}
        <span style={{ fontWeight: 400, marginLeft: 8, opacity: 0.8 }}>
          {vehicles.length} flows
        </span>
      </div>

      <Canvas
        camera={{ position: [0, 14, 10], fov: 50 }}
        style={{ background: '#f0f0f0' }}
      >
        <SceneLighting nightMode={false} />
        <GroundPlane
          nightMode={false}
          width={groundSize.width}
          depth={groundSize.depth}
          coordBounds={coordBounds}
        />

        {graphData && (
          <>
            <Roads3D
              edges={graphData.edges}
              nodeMap={nodeMap}
              to3D={to3D}
              hoveredEdge={null}
              onHoverEdge={() => {}}
            />
            <Intersections3D
              nodes={graphData.nodes}
              to3D={to3D}
              selectedNode={null}
              hoveredNode={null}
              onSelectNode={() => {}}
              onHoverNode={() => {}}
            />
            <TrafficLights3D
              signals={signals}
              nodes={graphData.nodes}
              nodeMap={nodeMap}
              to3D={to3D}
              nightMode={false}
            />
            <Vehicles3D
              vehicles={vehicles}
              nodeMap={nodeMap}
              to3D={to3D}
              nightMode={false}
            />
          </>
        )}

        <OrbitControls
          enableDamping
          dampingFactor={0.1}
          maxPolarAngle={Math.PI / 2.1}
          minDistance={3}
          maxDistance={30}
          enableZoom={false}
        />
      </Canvas>
    </div>
  );
};

export default Mini3DScene;
