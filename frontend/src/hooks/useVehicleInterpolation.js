import { useRef, useCallback } from 'react';
import { useFrame } from '@react-three/fiber';

/**
 * 60fps vehicle position interpolation between WebSocket updates (~1s interval)
 *
 * Stores prev/current snapshots; on each frame, lerps progress values
 * so vehicles move smoothly instead of jumping once per second.
 *
 * Returns a ref whose .current is an array of { flowId, ...vehicle, interpolatedProgress }
 */
export default function useVehicleInterpolation(vehicles, nodeMap, to3D) {
  const prevRef = useRef([]);
  const currRef = useRef([]);
  const lastUpdateRef = useRef(Date.now());
  const interpolatedRef = useRef([]);
  const UPDATE_INTERVAL = 1000; // expected ms between WS frames

  // Called when new vehicle data arrives from WebSocket
  const updateVehicles = useCallback((newVehicles) => {
    prevRef.current = currRef.current;
    currRef.current = newVehicles;
    lastUpdateRef.current = Date.now();
  }, []);

  // Update when vehicles prop changes
  if (vehicles !== currRef.current && vehicles.length > 0) {
    updateVehicles(vehicles);
  }

  useFrame(() => {
    if (!nodeMap || nodeMap.size === 0) return;
    const now = Date.now();
    const t = Math.min((now - lastUpdateRef.current) / UPDATE_INTERVAL, 1.0);
    const result = [];

    for (const curr of currRef.current) {
      const fromNode = nodeMap.get(String(curr.from));
      const toNode = nodeMap.get(String(curr.to));
      if (!fromNode || !toNode) continue;

      // Find matching previous state for interpolation
      const prev = prevRef.current.find(p => p.flowId === curr.flowId);
      let progress;
      if (prev && prev.from === curr.from && prev.to === curr.to) {
        // Same edge — interpolate progress
        progress = prev.progress + (curr.progress - prev.progress) * t;
      } else {
        // Edge changed or new vehicle — use current progress directly
        progress = curr.progress;
      }

      const from3D = to3D(fromNode.x, fromNode.y);
      const to3D_ = to3D(toNode.x, toNode.y);

      const x = from3D[0] + (to3D_[0] - from3D[0]) * progress;
      const z = from3D[2] + (to3D_[2] - from3D[2]) * progress;

      // Rotation: face movement direction
      const dx = to3D_[0] - from3D[0];
      const dz = to3D_[2] - from3D[2];
      const rotation = Math.atan2(dx, dz);

      result.push({
        ...curr,
        interpolatedProgress: progress,
        position: [x, 0.06, z],
        rotation,
      });
    }

    interpolatedRef.current = result;
  });

  return interpolatedRef;
}
