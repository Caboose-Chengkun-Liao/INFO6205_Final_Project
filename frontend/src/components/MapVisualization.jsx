import React, { useState, useEffect, useRef } from 'react';
import api from '../services/api';
import websocketService from '../services/websocket';

/**
 * Professional-grade Map Visualization Component
 * Arlington, VA Traffic Network with Real-time Vehicle Tracking
 */
const MapVisualization = () => {
  const [graphData, setGraphData] = useState(null);
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedNode, setSelectedNode] = useState(null);
  const [hoveredEdge, setHoveredEdge] = useState(null);
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });

  const svgRef = useRef(null);
  const containerRef = useRef(null);

  // Canvas dimensions - larger for better visibility
  const svgWidth = 1400;
  const svgHeight = 900;
  const padding = 80;

  useEffect(() => {
    loadGraphData();

    // Subscribe to WebSocket for real-time updates
    const unsubscribe = websocketService.subscribe('/topic/simulation', (data) => {
      if (data.activeFlows) {
        const vehiclePositions = data.activeFlows
          .filter(flow => flow.currentEdge)
          .map(flow => ({
            flowId: flow.flowId,
            numberOfCars: flow.numberOfCars,
            state: flow.state,
            currentEdge: flow.currentEdge.id,
            from: flow.currentEdge.fromNode.id,
            to: flow.currentEdge.toNode.id,
            progress: Math.min(1.0, flow.timeOnCurrentEdge / (flow.currentEdge.idealTravelTime * 60))
          }));
        setVehicles(vehiclePositions);
      }
    });

    return () => unsubscribe && unsubscribe();
  }, []);

  const loadGraphData = async () => {
    try {
      setLoading(true);
      const response = await api.get('/simulation/graph');
      setGraphData(response.data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to load map data');
    } finally {
      setLoading(false);
    }
  };

  // Coordinate transformation with zoom and pan
  const mapToSVG = (x, y) => {
    if (!graphData) return { x: 0, y: 0 };

    const nodes = graphData.nodes;
    const xCoords = nodes.map(n => n.x);
    const yCoords = nodes.map(n => n.y);
    const minX = Math.min(...xCoords);
    const maxX = Math.max(...xCoords);
    const minY = Math.min(...yCoords);
    const maxY = Math.max(...yCoords);

    // Map to SVG coordinates with Y-axis inversion
    const svgX = padding + ((x - minX) / (maxX - minX)) * (svgWidth - 2 * padding);
    const svgY = svgHeight - padding - ((y - minY) / (maxY - minY)) * (svgHeight - 2 * padding);

    return { x: svgX, y: svgY };
  };

  // Professional color scheme
  const colors = {
    intersection: '#2563EB',      // Blue
    boundary: '#DC2626',          // Red
    roadNormal: '#94A3B8',        // Slate gray
    roadMedium: '#F59E0B',        // Amber
    roadHeavy: '#EF4444',         // Red
    vehicleActive: '#10B981',     // Green
    vehicleBlocked: '#EF4444',    // Red
    vehicleWaiting: '#F59E0B',    // Amber
    background: '#F8FAFC',        // Light slate
    gridLines: '#E2E8F0',         // Light gray
    text: '#1E293B',              // Dark slate
    textSecondary: '#64748B'      // Medium slate
  };

  // Get road color based on load
  const getRoadColor = (edge) => {
    if (!edge.distance) return colors.roadNormal;
    const capacity = edge.distance * 50; // 50 cars/km
    const loadRatio = edge.currentLoad / capacity;

    if (loadRatio > 0.8) return colors.roadHeavy;
    if (loadRatio > 0.5) return colors.roadMedium;
    return colors.roadNormal;
  };

  // Get vehicle color based on state
  const getVehicleColor = (state) => {
    switch (state) {
      case 'BLOCKED': return colors.vehicleBlocked;
      case 'ACTIVE': return colors.vehicleActive;
      case 'WAITING': return colors.vehicleWaiting;
      default: return colors.vehicleActive;
    }
  };

  // Mouse event handlers for pan and zoom
  const handleMouseDown = (e) => {
    if (e.button === 0) {
      setIsDragging(true);
      setDragStart({ x: e.clientX - pan.x, y: e.clientY - pan.y });
    }
  };

  const handleMouseMove = (e) => {
    if (isDragging) {
      setPan({
        x: e.clientX - dragStart.x,
        y: e.clientY - dragStart.y
      });
    }
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const handleWheel = (e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? 0.9 : 1.1;
    setZoom(prevZoom => Math.max(0.5, Math.min(3, prevZoom * delta)));
  };

  // Calculate edge statistics
  const getEdgeStats = (edge) => {
    if (!edge.distance) return null;
    const capacity = edge.distance * 50;
    const loadPercent = ((edge.currentLoad / capacity) * 100).toFixed(1);
    return {
      capacity,
      loadPercent,
      vehicles: edge.currentLoad
    };
  };

  if (loading) {
    return (
      <div style={styles.container}>
        <div style={styles.loadingContainer}>
          <div style={styles.spinner}></div>
          <p style={styles.loadingText}>Loading Arlington Traffic Network...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.container}>
        <div style={styles.errorContainer}>
          <div style={styles.errorIcon}>⚠️</div>
          <p style={styles.errorText}>{error}</p>
          <button onClick={loadGraphData} style={styles.retryButton}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!graphData) return null;

  return (
    <div style={styles.container} ref={containerRef}>
      {/* Header with title and real-time stats */}
      <div style={styles.header}>
        <div style={styles.titleSection}>
          <h2 style={styles.title}>Arlington, VA - Traffic Network Visualization</h2>
          <p style={styles.subtitle}>Real-time traffic flow monitoring and optimization</p>
        </div>
        <div style={styles.statsBar}>
          <div style={styles.statCard}>
            <div style={styles.statValue}>{graphData.nodes.length}</div>
            <div style={styles.statLabel}>Nodes</div>
          </div>
          <div style={styles.statCard}>
            <div style={styles.statValue}>{graphData.edges.length}</div>
            <div style={styles.statLabel}>Roads</div>
          </div>
          <div style={styles.statCard}>
            <div style={styles.statValue}>{vehicles.length}</div>
            <div style={styles.statLabel}>Active Flows</div>
          </div>
        </div>
      </div>

      {/* Main map container */}
      <div style={styles.mapWrapper}>
        {/* Legend */}
        <div style={styles.legend}>
          <h4 style={styles.legendTitle}>Legend</h4>
          <div style={styles.legendItems}>
            <div style={styles.legendItem}>
              <div style={{...styles.legendDot, background: colors.intersection}}></div>
              <span style={styles.legendText}>Intersection</span>
            </div>
            <div style={styles.legendItem}>
              <div style={{...styles.legendDot, background: colors.boundary}}></div>
              <span style={styles.legendText}>Boundary</span>
            </div>
            <div style={styles.legendItem}>
              <div style={{...styles.legendDot, background: colors.vehicleActive}}></div>
              <span style={styles.legendText}>Active Vehicle</span>
            </div>
            <div style={styles.legendItem}>
              <div style={{...styles.legendDot, background: colors.vehicleBlocked}}></div>
              <span style={styles.legendText}>Blocked Vehicle</span>
            </div>
            <div style={styles.legendSeparator}></div>
            <div style={styles.legendItem}>
              <div style={{...styles.legendBar, background: colors.roadNormal}}></div>
              <span style={styles.legendText}>Light Traffic</span>
            </div>
            <div style={styles.legendItem}>
              <div style={{...styles.legendBar, background: colors.roadMedium}}></div>
              <span style={styles.legendText}>Medium Traffic</span>
            </div>
            <div style={styles.legendItem}>
              <div style={{...styles.legendBar, background: colors.roadHeavy}}></div>
              <span style={styles.legendText}>Heavy Traffic</span>
            </div>
          </div>
        </div>

        {/* Controls */}
        <div style={styles.controls}>
          <button
            onClick={() => setZoom(z => Math.min(3, z * 1.2))}
            style={styles.controlButton}
            title="Zoom In"
          >
            +
          </button>
          <button
            onClick={() => setZoom(z => Math.max(0.5, z / 1.2))}
            style={styles.controlButton}
            title="Zoom Out"
          >
            −
          </button>
          <button
            onClick={() => { setZoom(1); setPan({ x: 0, y: 0 }); }}
            style={styles.controlButton}
            title="Reset View"
          >
            ⟲
          </button>
        </div>

        {/* SVG Canvas */}
        <svg
          ref={svgRef}
          width={svgWidth}
          height={svgHeight}
          style={{
            ...styles.svg,
            cursor: isDragging ? 'grabbing' : 'grab',
            transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`
          }}
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseUp}
          onWheel={handleWheel}
        >
          {/* Grid background */}
          <defs>
            <pattern id="grid" width="50" height="50" patternUnits="userSpaceOnUse">
              <path d="M 50 0 L 0 0 0 50" fill="none" stroke={colors.gridLines} strokeWidth="0.5"/>
            </pattern>

            {/* Glow effect for active elements */}
            <filter id="glow">
              <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
              <feMerge>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="SourceGraphic"/>
              </feMerge>
            </filter>

            {/* Shadow for nodes */}
            <filter id="shadow" x="-50%" y="-50%" width="200%" height="200%">
              <feDropShadow dx="0" dy="2" stdDeviation="2" floodOpacity="0.3"/>
            </filter>
          </defs>

          <rect width={svgWidth} height={svgHeight} fill="url(#grid)"/>

          {/* Roads (Edges) */}
          <g className="roads">
            {graphData.edges.map((edge) => {
              const fromNode = graphData.nodes.find(n => n.id === edge.from);
              const toNode = graphData.nodes.find(n => n.id === edge.to);

              if (!fromNode || !toNode) return null;

              const from = mapToSVG(fromNode.x, fromNode.y);
              const to = mapToSVG(toNode.x, toNode.y);
              const edgeColor = getRoadColor(edge);
              const isHovered = hoveredEdge === edge.id;
              const stats = getEdgeStats(edge);

              return (
                <g key={edge.id}>
                  {/* Road line */}
                  <line
                    x1={from.x}
                    y1={from.y}
                    x2={to.x}
                    y2={to.y}
                    stroke={edgeColor}
                    strokeWidth={isHovered ? "6" : "4"}
                    strokeLinecap="round"
                    opacity={isHovered ? "0.9" : "0.6"}
                    filter={isHovered ? "url(#glow)" : ""}
                    onMouseEnter={() => setHoveredEdge(edge.id)}
                    onMouseLeave={() => setHoveredEdge(null)}
                    style={{ cursor: 'pointer', transition: 'all 0.2s' }}
                  />

                  {/* Distance label */}
                  {edge.distance && (
                    <text
                      x={(from.x + to.x) / 2}
                      y={(from.y + to.y) / 2 - 8}
                      fontSize="11"
                      fontWeight="500"
                      fill={colors.text}
                      textAnchor="middle"
                      pointerEvents="none"
                      style={{
                        opacity: isHovered ? 1 : 0.7,
                        textShadow: '0 0 3px white'
                      }}
                    >
                      {edge.distance.toFixed(1)} km
                    </text>
                  )}

                  {/* Load indicator */}
                  {stats && isHovered && (
                    <text
                      x={(from.x + to.x) / 2}
                      y={(from.y + to.y) / 2 + 15}
                      fontSize="10"
                      fontWeight="600"
                      fill={edgeColor}
                      textAnchor="middle"
                      pointerEvents="none"
                      style={{ textShadow: '0 0 3px white' }}
                    >
                      Load: {stats.loadPercent}% ({stats.vehicles}/{stats.capacity.toFixed(0)})
                    </text>
                  )}
                </g>
              );
            })}
          </g>

          {/* Nodes */}
          <g className="nodes">
            {graphData.nodes.map((node) => {
              const pos = mapToSVG(node.x, node.y);
              const color = node.type === 'INTERSECTION' ? colors.intersection : colors.boundary;
              const radius = node.type === 'INTERSECTION' ? 10 : 8;
              const isSelected = selectedNode?.id === node.id;

              return (
                <g key={node.id}>
                  {/* Selection ring */}
                  {isSelected && (
                    <circle
                      cx={pos.x}
                      cy={pos.y}
                      r={radius + 6}
                      fill="none"
                      stroke={color}
                      strokeWidth="2"
                      opacity="0.5"
                    />
                  )}

                  {/* Node circle */}
                  <circle
                    cx={pos.x}
                    cy={pos.y}
                    r={radius}
                    fill={color}
                    stroke="white"
                    strokeWidth="2.5"
                    filter="url(#shadow)"
                    onClick={() => setSelectedNode(node)}
                    style={{
                      cursor: 'pointer',
                      transition: 'all 0.2s'
                    }}
                    onMouseEnter={(e) => {
                      e.target.setAttribute('r', radius + 2);
                    }}
                    onMouseLeave={(e) => {
                      e.target.setAttribute('r', radius);
                    }}
                  />

                  {/* Node ID */}
                  <text
                    x={pos.x}
                    y={pos.y - radius - 8}
                    fontSize="12"
                    fontWeight="bold"
                    fill={colors.text}
                    textAnchor="middle"
                    pointerEvents="none"
                    style={{ textShadow: '0 0 3px white' }}
                  >
                    {node.id}
                  </text>

                  {/* Node name (for intersections only) */}
                  {node.type === 'INTERSECTION' && (
                    <text
                      x={pos.x}
                      y={pos.y + radius + 18}
                      fontSize="10"
                      fill={colors.textSecondary}
                      textAnchor="middle"
                      pointerEvents="none"
                      style={{ textShadow: '0 0 3px white' }}
                    >
                      {node.name.length > 25 ? node.name.substring(0, 25) + '...' : node.name}
                    </text>
                  )}
                </g>
              );
            })}
          </g>

          {/* Vehicles with animation */}
          <g className="vehicles">
            {vehicles.map((vehicle) => {
              const fromNode = graphData.nodes.find(n => n.id === vehicle.from);
              const toNode = graphData.nodes.find(n => n.id === vehicle.to);

              if (!fromNode || !toNode) return null;

              const from = mapToSVG(fromNode.x, fromNode.y);
              const to = mapToSVG(toNode.x, toNode.y);

              // Interpolate position
              const x = from.x + (to.x - from.x) * vehicle.progress;
              const y = from.y + (to.y - from.y) * vehicle.progress;

              const vehicleColor = getVehicleColor(vehicle.state);

              return (
                <g key={vehicle.flowId}>
                  {/* Vehicle marker with pulse effect */}
                  <circle
                    cx={x}
                    cy={y}
                    r="7"
                    fill={vehicleColor}
                    stroke="white"
                    strokeWidth="2"
                    filter="url(#glow)"
                    style={{
                      animation: 'pulse 2s ease-in-out infinite'
                    }}
                  />

                  {/* Vehicle count badge */}
                  <circle
                    cx={x + 10}
                    cy={y - 10}
                    r="8"
                    fill="white"
                    stroke={vehicleColor}
                    strokeWidth="2"
                  />
                  <text
                    x={x + 10}
                    y={y - 7}
                    fontSize="10"
                    fontWeight="bold"
                    fill={vehicleColor}
                    textAnchor="middle"
                  >
                    {vehicle.numberOfCars}
                  </text>
                </g>
              );
            })}
          </g>
        </svg>

        {/* Selected node details panel */}
        {selectedNode && (
          <div style={styles.detailsPanel}>
            <div style={styles.detailsHeader}>
              <h4 style={styles.detailsTitle}>Node Details</h4>
              <button
                onClick={() => setSelectedNode(null)}
                style={styles.closeButton}
              >
                ×
              </button>
            </div>
            <div style={styles.detailsContent}>
              <div style={styles.detailRow}>
                <span style={styles.detailLabel}>ID:</span>
                <span style={styles.detailValue}>{selectedNode.id}</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.detailLabel}>Name:</span>
                <span style={styles.detailValue}>{selectedNode.name}</span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.detailLabel}>Type:</span>
                <span style={{
                  ...styles.detailValue,
                  ...styles.badge,
                  background: selectedNode.type === 'INTERSECTION' ? colors.intersection : colors.boundary
                }}>
                  {selectedNode.type}
                </span>
              </div>
              <div style={styles.detailRow}>
                <span style={styles.detailLabel}>Position:</span>
                <span style={styles.detailValue}>
                  ({selectedNode.x.toFixed(1)}, {selectedNode.y.toFixed(1)})
                </span>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* CSS Animations */}
      <style>{`
        @keyframes pulse {
          0%, 100% {
            opacity: 1;
            transform: scale(1);
          }
          50% {
            opacity: 0.8;
            transform: scale(1.1);
          }
        }

        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }

        .node-circle:hover {
          filter: url(#glow);
        }
      `}</style>
    </div>
  );
};

// Professional styles
const styles = {
  container: {
    width: '100%',
    background: '#FFFFFF',
    borderRadius: '16px',
    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
    overflow: 'hidden'
  },
  header: {
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    padding: '24px 32px',
    color: 'white'
  },
  titleSection: {
    marginBottom: '20px'
  },
  title: {
    margin: 0,
    fontSize: '28px',
    fontWeight: '700',
    letterSpacing: '-0.5px'
  },
  subtitle: {
    margin: '8px 0 0 0',
    fontSize: '14px',
    opacity: 0.9,
    fontWeight: '400'
  },
  statsBar: {
    display: 'flex',
    gap: '16px'
  },
  statCard: {
    background: 'rgba(255, 255, 255, 0.15)',
    backdropFilter: 'blur(10px)',
    borderRadius: '12px',
    padding: '16px 24px',
    minWidth: '120px',
    textAlign: 'center',
    border: '1px solid rgba(255, 255, 255, 0.2)'
  },
  statValue: {
    fontSize: '32px',
    fontWeight: '700',
    lineHeight: '1'
  },
  statLabel: {
    fontSize: '12px',
    marginTop: '8px',
    opacity: 0.9,
    textTransform: 'uppercase',
    letterSpacing: '0.5px',
    fontWeight: '500'
  },
  mapWrapper: {
    position: 'relative',
    padding: '24px',
    background: '#F8FAFC',
    minHeight: '700px',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center'
  },
  svg: {
    background: 'white',
    borderRadius: '12px',
    boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
    transformOrigin: 'center center',
    transition: 'transform 0.1s ease-out',
    userSelect: 'none'
  },
  legend: {
    position: 'absolute',
    top: '32px',
    left: '32px',
    background: 'white',
    borderRadius: '12px',
    padding: '20px',
    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
    minWidth: '200px',
    zIndex: 10
  },
  legendTitle: {
    margin: '0 0 16px 0',
    fontSize: '16px',
    fontWeight: '600',
    color: '#1E293B'
  },
  legendItems: {
    display: 'flex',
    flexDirection: 'column',
    gap: '10px'
  },
  legendItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px'
  },
  legendDot: {
    width: '14px',
    height: '14px',
    borderRadius: '50%',
    border: '2px solid white',
    boxShadow: '0 1px 2px rgba(0, 0, 0, 0.1)'
  },
  legendBar: {
    width: '30px',
    height: '4px',
    borderRadius: '2px'
  },
  legendText: {
    fontSize: '13px',
    color: '#64748B',
    fontWeight: '500'
  },
  legendSeparator: {
    height: '1px',
    background: '#E2E8F0',
    margin: '4px 0'
  },
  controls: {
    position: 'absolute',
    top: '32px',
    right: '32px',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    zIndex: 10
  },
  controlButton: {
    width: '44px',
    height: '44px',
    background: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: '20px',
    fontWeight: 'bold',
    color: '#667eea',
    cursor: 'pointer',
    boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
    transition: 'all 0.2s',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  },
  detailsPanel: {
    position: 'absolute',
    bottom: '32px',
    right: '32px',
    background: 'white',
    borderRadius: '12px',
    padding: '0',
    boxShadow: '0 10px 25px rgba(0, 0, 0, 0.15)',
    minWidth: '300px',
    zIndex: 10,
    overflow: 'hidden'
  },
  detailsHeader: {
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    color: 'white',
    padding: '16px 20px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  detailsTitle: {
    margin: 0,
    fontSize: '16px',
    fontWeight: '600'
  },
  closeButton: {
    background: 'rgba(255, 255, 255, 0.2)',
    border: 'none',
    color: 'white',
    width: '28px',
    height: '28px',
    borderRadius: '6px',
    fontSize: '20px',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    transition: 'background 0.2s'
  },
  detailsContent: {
    padding: '20px'
  },
  detailRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '10px 0',
    borderBottom: '1px solid #F1F5F9'
  },
  detailLabel: {
    fontSize: '13px',
    color: '#64748B',
    fontWeight: '500'
  },
  detailValue: {
    fontSize: '14px',
    color: '#1E293B',
    fontWeight: '600'
  },
  badge: {
    padding: '4px 12px',
    borderRadius: '6px',
    color: 'white',
    fontSize: '11px',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  loadingContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '80px 40px',
    gap: '20px'
  },
  spinner: {
    width: '50px',
    height: '50px',
    border: '4px solid #E2E8F0',
    borderTop: '4px solid #667eea',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite'
  },
  loadingText: {
    fontSize: '16px',
    color: '#64748B',
    fontWeight: '500'
  },
  errorContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '80px 40px',
    gap: '16px'
  },
  errorIcon: {
    fontSize: '48px'
  },
  errorText: {
    fontSize: '16px',
    color: '#EF4444',
    fontWeight: '500',
    textAlign: 'center'
  },
  retryButton: {
    padding: '12px 32px',
    background: '#667eea',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s',
    boxShadow: '0 2px 4px rgba(102, 126, 234, 0.3)'
  }
};

export default MapVisualization;
