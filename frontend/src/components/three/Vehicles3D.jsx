import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Html } from '@react-three/drei';
import { getVehicleColor } from '../../utils/mapUtils';
import useVehicleInterpolation from '../../hooks/useVehicleInterpolation';

const Vehicles3D = ({ vehicles, nodeMap, to3D, nightMode = false }) => {
  const interpolatedRef = useVehicleInterpolation(vehicles, nodeMap, to3D);
  const groupRef = useRef();
  const meshRefs = useRef({});

  useFrame(() => {
    const data = interpolatedRef.current;
    if (!data || !groupRef.current) return;

    // Update each vehicle mesh position/rotation imperatively for performance
    for (const v of data) {
      const mesh = meshRefs.current[v.flowId];
      if (mesh) {
        mesh.position.set(v.position[0], v.position[1], v.position[2]);
        mesh.rotation.set(0, v.rotation, 0);
      }
    }
  });

  // Render vehicle meshes — these are positioned by useFrame, not React state
  const currentVehicles = interpolatedRef.current?.length > 0
    ? interpolatedRef.current
    : vehicles.map(v => ({ ...v, position: [0, -10, 0], rotation: 0 }));

  return (
    <group ref={groupRef}>
      {currentVehicles.map(vehicle => {
        const color = getVehicleColor(vehicle.state);
        return (
          <group
            key={vehicle.flowId}
            ref={el => { if (el) meshRefs.current[vehicle.flowId] = el; }}
            position={vehicle.position || [0, -10, 0]}
          >
            {/* Car body */}
            <mesh castShadow>
              <boxGeometry args={[0.22, 0.08, 0.12]} />
              <meshStandardMaterial
                color={color}
                emissive={nightMode ? color : '#000000'}
                emissiveIntensity={nightMode ? 0.4 : 0}
              />
            </mesh>

            {/* Car roof (smaller box on top) */}
            <mesh position={[0, 0.055, 0]}>
              <boxGeometry args={[0.12, 0.05, 0.1]} />
              <meshStandardMaterial
                color={color}
                emissive={nightMode ? color : '#000000'}
                emissiveIntensity={nightMode ? 0.2 : 0}
              />
            </mesh>

            {/* Night mode headlights */}
            {nightMode && (
              <>
                <mesh position={[0.12, 0.02, 0.04]}>
                  <sphereGeometry args={[0.015, 8, 8]} />
                  <meshStandardMaterial
                    color="#ffffcc"
                    emissive="#ffffaa"
                    emissiveIntensity={2}
                  />
                </mesh>
                <mesh position={[0.12, 0.02, -0.04]}>
                  <sphereGeometry args={[0.015, 8, 8]} />
                  <meshStandardMaterial
                    color="#ffffcc"
                    emissive="#ffffaa"
                    emissiveIntensity={2}
                  />
                </mesh>
              </>
            )}

            {/* Vehicle count badge */}
            {vehicle.numberOfCars > 1 && (
              <Html position={[0, 0.2, 0]} center distanceFactor={6}>
                <div style={{
                  background: color,
                  color: '#fff',
                  borderRadius: '50%',
                  width: 18,
                  height: 18,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 9,
                  fontWeight: 700,
                  border: '1.5px solid #fff',
                  pointerEvents: 'none',
                }}>
                  {vehicle.numberOfCars}
                </div>
              </Html>
            )}
          </group>
        );
      })}
    </group>
  );
};

export default Vehicles3D;
