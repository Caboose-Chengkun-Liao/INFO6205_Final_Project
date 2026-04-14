import React, { useMemo } from 'react';
import { Line, Html } from '@react-three/drei';
import { getRoadColor } from '../../utils/mapUtils';

const Roads3D = React.memo(({ edges, nodeMap, to3D, hoveredEdge, onHoverEdge }) => {

  const roadSegments = useMemo(() => {
    if (!edges || !nodeMap || nodeMap.size === 0) return [];
    return edges.map(edge => {
      const fromNode = nodeMap.get(String(edge.from));
      const toNode = nodeMap.get(String(edge.to));
      if (!fromNode || !toNode) return null;

      const from3D = to3D(fromNode.x, fromNode.y);
      const to3D_ = to3D(toNode.x, toNode.y);
      const color = getRoadColor(edge);
      const capacity = edge.distance * 50;
      const loadPercent = ((edge.currentLoad || 0) / capacity * 100).toFixed(1);
      const isHighLoad = (edge.currentLoad || 0) / capacity > 0.6;

      return {
        id: edge.id,
        points: [
          [from3D[0], 0.02, from3D[2]],
          [to3D_[0], 0.02, to3D_[2]],
        ],
        color,
        isHighLoad,
        distance: edge.distance,
        loadPercent,
        currentLoad: edge.currentLoad || 0,
        midpoint: [
          (from3D[0] + to3D_[0]) / 2,
          0.3,
          (from3D[2] + to3D_[2]) / 2,
        ],
      };
    }).filter(Boolean);
  }, [edges, nodeMap, to3D]);

  return (
    <group>
      {roadSegments.map(seg => (
        <group key={seg.id}>
          {/* High load glow underlay */}
          {seg.isHighLoad && (
            <Line
              points={seg.points}
              color={seg.color}
              lineWidth={8}
              transparent
              opacity={0.25}
            />
          )}

          {/* Main road line */}
          <Line
            points={seg.points}
            color={hoveredEdge === seg.id ? '#ffffff' : seg.color}
            lineWidth={hoveredEdge === seg.id ? 5 : 3}
          />

          {/* Invisible hover box along the edge */}
          <mesh
            position={seg.midpoint}
            rotation={[
              -Math.PI / 2, 0,
              Math.atan2(
                seg.points[1][2] - seg.points[0][2],
                seg.points[1][0] - seg.points[0][0]
              )
            ]}
            onPointerEnter={(e) => { e.stopPropagation(); onHoverEdge?.(seg.id); }}
            onPointerLeave={() => onHoverEdge?.(null)}
            visible={false}
          >
            <planeGeometry args={[
              Math.sqrt(
                (seg.points[1][0] - seg.points[0][0]) ** 2 +
                (seg.points[1][2] - seg.points[0][2]) ** 2
              ),
              0.3
            ]} />
            <meshBasicMaterial transparent opacity={0} />
          </mesh>

          {/* Hover tooltip */}
          {hoveredEdge === seg.id && (
            <Html position={seg.midpoint} center>
              <div style={{
                background: 'rgba(0,0,0,0.8)',
                color: '#fff',
                padding: '4px 8px',
                borderRadius: 4,
                fontSize: 11,
                whiteSpace: 'nowrap',
                pointerEvents: 'none',
              }}>
                {seg.distance.toFixed(2)} km | Load: {seg.loadPercent}% ({seg.currentLoad} vehicles)
              </div>
            </Html>
          )}
        </group>
      ))}
    </group>
  );
});

Roads3D.displayName = 'Roads3D';
export default Roads3D;
