import React, { useMemo } from 'react';
import { Text, Html } from '@react-three/drei';
import { colors } from '../../utils/mapUtils';

const Intersections3D = React.memo(({ nodes, to3D, selectedNode, hoveredNode, onSelectNode, onHoverNode }) => {

  const nodePositions = useMemo(() => {
    if (!nodes) return [];
    return nodes.map(node => {
      const pos = to3D(node.x, node.y);
      const isIntersection = node.type === 'INTERSECTION';
      return {
        ...node,
        pos,
        isIntersection,
        color: isIntersection ? colors.intersection : colors.boundary,
        radius: isIntersection ? 0.15 : 0.1,
        height: isIntersection ? 0.08 : undefined,
      };
    });
  }, [nodes, to3D]);

  return (
    <group>
      {nodePositions.map(node => {
        const isSelected = selectedNode?.id === node.id;
        const isHovered = hoveredNode === node.id;

        return (
          <group key={node.id} position={node.pos}>
            {/* Node marker */}
            {node.isIntersection ? (
              // Intersection: cylinder
              <mesh
                position={[0, 0.04, 0]}
                onClick={(e) => { e.stopPropagation(); onSelectNode?.(node); }}
                onPointerEnter={(e) => { e.stopPropagation(); onHoverNode?.(node.id); }}
                onPointerLeave={() => onHoverNode?.(null)}
                castShadow
              >
                <cylinderGeometry args={[node.radius, node.radius, node.height, 16]} />
                <meshStandardMaterial
                  color={isSelected ? '#FF9F0A' : node.color}
                  emissive={isSelected ? '#FF9F0A' : isHovered ? node.color : '#000000'}
                  emissiveIntensity={isSelected ? 0.5 : isHovered ? 0.3 : 0}
                />
              </mesh>
            ) : (
              // Boundary: sphere
              <mesh
                position={[0, node.radius, 0]}
                onClick={(e) => { e.stopPropagation(); onSelectNode?.(node); }}
                onPointerEnter={(e) => { e.stopPropagation(); onHoverNode?.(node.id); }}
                onPointerLeave={() => onHoverNode?.(null)}
              >
                <sphereGeometry args={[node.radius, 16, 16]} />
                <meshStandardMaterial
                  color={isSelected ? '#FF9F0A' : node.color}
                  emissive={isHovered ? node.color : '#000000'}
                  emissiveIntensity={isHovered ? 0.3 : 0}
                />
              </mesh>
            )}

            {/* Node ID label */}
            <Text
              position={[0, 0.3, 0]}
              fontSize={0.12}
              color={isSelected ? '#FF9F0A' : '#333'}
              anchorX="center"
              anchorY="bottom"
              outlineWidth={0.01}
              outlineColor="#ffffff"
            >
              {node.id}
            </Text>

            {/* Hover name label */}
            {(isHovered || isSelected) && (
              <Html position={[0, 0.5, 0]} center>
                <div style={{
                  background: 'rgba(0,0,0,0.85)',
                  color: '#fff',
                  padding: '4px 10px',
                  borderRadius: 6,
                  fontSize: 12,
                  whiteSpace: 'nowrap',
                  pointerEvents: 'none',
                  fontFamily: '-apple-system, sans-serif',
                }}>
                  <strong>{node.name}</strong>
                  <span style={{ color: '#999', marginLeft: 6 }}>
                    {node.isIntersection ? 'Intersection' : 'Boundary'}
                  </span>
                </div>
              </Html>
            )}
          </group>
        );
      })}
    </group>
  );
});

Intersections3D.displayName = 'Intersections3D';
export default Intersections3D;
