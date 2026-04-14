import React from 'react';

const SceneLighting = React.memo(({ nightMode = false }) => {
  if (nightMode) {
    return (
      <group>
        <ambientLight intensity={0.12} color="#334466" />
        <directionalLight
          position={[5, 12, 5]}
          intensity={0.25}
          color="#6688bb"
          castShadow
          shadow-mapSize-width={2048}
          shadow-mapSize-height={2048}
          shadow-camera-far={50}
          shadow-camera-left={-15}
          shadow-camera-right={15}
          shadow-camera-top={15}
          shadow-camera-bottom={-15}
        />
        <pointLight position={[0, 10, 0]} intensity={0.08} color="#223355" />
        {/* Moonlight-like backlight */}
        <directionalLight position={[-8, 6, -8]} intensity={0.1} color="#aabbdd" />
      </group>
    );
  }

  return (
    <group>
      <ambientLight intensity={0.55} color="#ffffff" />
      <directionalLight
        position={[12, 18, 8]}
        intensity={0.9}
        castShadow
        shadow-mapSize-width={2048}
        shadow-mapSize-height={2048}
        shadow-camera-far={50}
        shadow-camera-left={-15}
        shadow-camera-right={15}
        shadow-camera-top={15}
        shadow-camera-bottom={-15}
      />
      <directionalLight position={[-6, 10, -6]} intensity={0.25} color="#ffffee" />
      <hemisphereLight args={['#b1e1ff', '#b97a20', 0.15]} />
    </group>
  );
});

SceneLighting.displayName = 'SceneLighting';
export default SceneLighting;
