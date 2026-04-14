import React, { useMemo } from 'react';
import { useLoader } from '@react-three/fiber';
import { Sky } from '@react-three/drei';
import * as THREE from 'three';

/**
 * Ground plane with high-res Arlington VA map texture (2048x1792, zoom=15 OSM tiles).
 * Covers the expanded road network coordinate range (-2~14, -1~12).
 */
const GroundPlane = React.memo(({ nightMode = false }) => {
  const mapTexture = useLoader(THREE.TextureLoader, '/arlington-map.jpg');

  useMemo(() => {
    if (mapTexture) {
      mapTexture.colorSpace = THREE.SRGBColorSpace;
      mapTexture.minFilter = THREE.LinearMipmapLinearFilter;
      mapTexture.magFilter = THREE.LinearFilter;
      mapTexture.anisotropy = 8;
    }
  }, [mapTexture]);

  return (
    <group>
      {/* High-res map textured ground */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.02, 0]} receiveShadow>
        <planeGeometry args={[20, 17]} />
        <meshStandardMaterial
          map={mapTexture}
          transparent
          opacity={nightMode ? 0.3 : 0.75}
          toneMapped={false}
        />
      </mesh>

      {/* Extended solid ground beneath */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.03, 0]} receiveShadow>
        <planeGeometry args={[30, 26]} />
        <meshStandardMaterial color={nightMode ? '#0a0e17' : '#dde0e4'} />
      </mesh>

      {/* Subtle grid */}
      <gridHelper
        args={[30, 60,
          nightMode ? '#1a2332' : '#c0c4c8',
          nightMode ? '#141d29' : '#d0d3d7'
        ]}
        position={[0, -0.015, 0]}
      />

      {/* Sky dome — day mode only */}
      {!nightMode && (
        <Sky
          distance={450000}
          sunPosition={[100, 50, 100]}
          inclination={0.6}
          azimuth={0.25}
          turbidity={8}
          rayleigh={0.5}
        />
      )}

      {/* Fog for depth */}
      {nightMode
        ? <fog attach="fog" args={['#0a0e17', 15, 40]} />
        : <fog attach="fog" args={['#e8eaed', 25, 50]} />
      }
    </group>
  );
});

GroundPlane.displayName = 'GroundPlane';
export default GroundPlane;
