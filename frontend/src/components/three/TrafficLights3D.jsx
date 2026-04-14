import React, { useMemo } from 'react';
import { colors as mapColors } from '../../utils/mapUtils';

const TrafficLights3D = React.memo(({ signals, nodes, nodeMap, to3D, nightMode = false }) => {

  const signalObjects = useMemo(() => {
    if (!signals || !nodeMap || nodeMap.size === 0) return [];

    return signals.map(signal => {
      const node = nodeMap.get(String(signal.nodeId));
      if (!node || node.type !== 'INTERSECTION') return null;

      const pos = to3D(node.x, node.y);
      const state = signal.currentState || signal.state || 'RED';

      return {
        nodeId: signal.nodeId,
        position: [pos[0] + 0.25, 0, pos[2] - 0.15],
        state: state.toUpperCase(),
      };
    }).filter(Boolean);
  }, [signals, nodeMap, to3D]);

  return (
    <group>
      {signalObjects.map(sig => {
        const isGreen = sig.state === 'GREEN';
        const isYellow = sig.state === 'YELLOW';
        const isRed = sig.state === 'RED' || sig.state === 'ALL_RED';

        const emissiveIntensity = nightMode ? 3 : 1.5;
        const offIntensity = nightMode ? 0.05 : 0;

        return (
          <group key={sig.nodeId} position={sig.position}>
            {/* Pole */}
            <mesh position={[0, 0.3, 0]}>
              <cylinderGeometry args={[0.015, 0.015, 0.6, 8]} />
              <meshStandardMaterial color="#555" />
            </mesh>

            {/* Signal housing */}
            <mesh position={[0, 0.5, 0]}>
              <boxGeometry args={[0.06, 0.22, 0.06]} />
              <meshStandardMaterial color="#222" />
            </mesh>

            {/* Red light */}
            <mesh position={[0, 0.56, 0.031]}>
              <sphereGeometry args={[0.022, 12, 12]} />
              <meshStandardMaterial
                color={mapColors.signalRed}
                emissive={mapColors.signalRed}
                emissiveIntensity={isRed ? emissiveIntensity : offIntensity}
              />
            </mesh>

            {/* Yellow light */}
            <mesh position={[0, 0.50, 0.031]}>
              <sphereGeometry args={[0.022, 12, 12]} />
              <meshStandardMaterial
                color={mapColors.signalYellow}
                emissive={mapColors.signalYellow}
                emissiveIntensity={isYellow ? emissiveIntensity : offIntensity}
              />
            </mesh>

            {/* Green light */}
            <mesh position={[0, 0.44, 0.031]}>
              <sphereGeometry args={[0.022, 12, 12]} />
              <meshStandardMaterial
                color={mapColors.signalGreen}
                emissive={mapColors.signalGreen}
                emissiveIntensity={isGreen ? emissiveIntensity : offIntensity}
              />
            </mesh>
          </group>
        );
      })}
    </group>
  );
});

TrafficLights3D.displayName = 'TrafficLights3D';
export default TrafficLights3D;
