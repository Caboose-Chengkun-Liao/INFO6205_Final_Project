import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import api from '../services/api';
import websocketService from '../services/websocket';
import { computeCoordBounds, mapToSVG, colors, getRoadColor, getVehicleColor, getSignalColor } from '../utils/mapUtils';

const MapVisualization = ({ signals = [] }) => {
  const [graphData, setGraphData] = useState(null);
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedNode, setSelectedNode] = useState(null);
  const [hoveredEdge, setHoveredEdge] = useState(null);

  const svgRef = useRef(null);
  const containerRef = useRef(null);

  const svgWidth = 1600;
  const svgHeight = 800;
  const padding = 60;

  useEffect(() => {
    loadGraphData();

    websocketService.subscribe('/topic/simulation', (data) => {
      if (data.activeFlows) {
        const vehiclePositions = data.activeFlows
          .filter(flow => flow.currentEdge && flow.currentEdge.fromNode && flow.currentEdge.toNode)
          .map(flow => ({
            flowId: flow.flowId,
            numberOfCars: flow.numberOfCars,
            state: flow.state,
            currentEdge: flow.currentEdge.id,
            from: flow.currentEdge.fromNode.id,
            to: flow.currentEdge.toNode.id,
            progress: Math.min(1.0, flow.timeOnCurrentEdge / ((flow.currentEdge.idealTravelTime || 1) * 60))
          }));
        setVehicles(vehiclePositions);
      }
    });

    const graphRefreshInterval = setInterval(() => {
      loadGraphData(false);
    }, 5000);

    return () => {
      websocketService.unsubscribe('/topic/simulation');
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

  const coordBounds = useMemo(() => {
    if (!graphData || !graphData.nodes) return null;
    return computeCoordBounds(graphData.nodes);
  }, [graphData]);

  const toSVG = useCallback((x, y) => {
    return mapToSVG(x, y, coordBounds, svgWidth, svgHeight, padding);
  }, [coordBounds]);

  const getEdgeStats = (edge) => {
    if (!edge.distance) return null;
    const capacity = edge.distance * 50;
    const currentLoad = edge.currentLoad || 0;
    const loadPercent = ((currentLoad / capacity) * 100).toFixed(1);
    return { capacity, loadPercent, vehicles: currentLoad };
  };

  // Find signal for a node
  const getNodeSignal = (nodeId) => {
    return signals.find(s => s.nodeId === nodeId || s.nodeId === String(nodeId));
  };

  if (loading) {
    return (
      <div style={s.container}>
        <div style={s.loadingContainer}>
          <div style={s.spinner}></div>
          <p style={s.loadingText}>Loading Arlington Traffic Network...</p>
        </div>
        <style>{spinnerCSS}</style>
      </div>
    );
  }

  if (error) {
    return (
      <div style={s.container}>
        <div style={s.errorContainer}>
          <p style={s.errorIcon}>!</p>
          <p style={s.errorText}>{error}</p>
          <button onClick={loadGraphData} style={s.retryButton}>Retry</button>
        </div>
      </div>
    );
  }

  if (!graphData) return null;

  return (
    <div style={s.container} ref={containerRef}>
      <div style={s.header}>
        <div style={s.titleSection}>
          <h2 style={s.title}>Arlington, VA — Traffic Network</h2>
          <p style={s.subtitle}>Real-time traffic flow monitoring and optimization</p>
        </div>
        <div style={s.statsBar}>
          <div style={s.statCard}>
            <div style={s.statValue}>{graphData.nodes.length}</div>
            <div style={s.statLabel}>Nodes</div>
          </div>
          <div style={s.statCard}>
            <div style={s.statValue}>{graphData.edges.length}</div>
            <div style={s.statLabel}>Roads</div>
          </div>
          <div style={s.statCard}>
            <div style={s.statValue}>{vehicles.length}</div>
            <div style={s.statLabel}>Active Flows</div>
          </div>
        </div>
      </div>

      <div style={s.mapWrapper}>
        {/* Legend */}
        <div style={s.legend}>
          <h4 style={s.legendTitle}>Legend</h4>
          <div style={s.legendItems}>
            <div style={s.legendItem}>
              <div style={{...s.legendDot, background: colors.intersection}}></div>
              <span style={s.legendText}>Intersection</span>
            </div>
            <div style={s.legendItem}>
              <div style={{...s.legendDot, background: colors.boundary}}></div>
              <span style={s.legendText}>Boundary</span>
            </div>
            <div style={s.legendItem}>
              <div style={{...s.legendDot, background: colors.vehicleActive}}></div>
              <span style={s.legendText}>Active Vehicle</span>
            </div>
            <div style={s.legendItem}>
              <div style={{...s.legendDot, background: colors.vehicleBlocked}}></div>
              <span style={s.legendText}>Blocked Vehicle</span>
            </div>
            <div style={s.legendSep}></div>
            <div style={s.legendItem}>
              <div style={{...s.legendBar, background: colors.roadNormal}}></div>
              <span style={s.legendText}>Light Traffic</span>
            </div>
            <div style={s.legendItem}>
              <div style={{...s.legendBar, background: colors.roadMedium}}></div>
              <span style={s.legendText}>Medium Traffic</span>
            </div>
            <div style={s.legendItem}>
              <div style={{...s.legendBar, background: colors.roadHeavy}}></div>
              <span style={s.legendText}>Heavy Traffic</span>
            </div>
            <div style={s.legendSep}></div>
            <div style={s.legendItem}>
              <svg width="14" height="30" viewBox="0 0 14 30">
                <rect width="14" height="30" rx="3" fill="#1D1D1F" opacity="0.85"/>
                <circle cx="7" cy="6" r="3" fill={colors.signalRed}/>
                <circle cx="7" cy="15" r="3" fill={colors.signalOff}/>
                <circle cx="7" cy="24" r="3" fill={colors.signalOff}/>
              </svg>
              <span style={s.legendText}>Traffic Signal</span>
            </div>
          </div>
        </div>

        {/* SVG Canvas */}
        <svg ref={svgRef} viewBox={`0 0 ${svgWidth} ${svgHeight}`} style={s.svg}>
          <defs>
            <radialGradient id="bgGrad" cx="50%" cy="50%" r="65%">
              <stop offset="0%" stopColor="#FAFBFC"/>
              <stop offset="100%" stopColor="#EEEEF0"/>
            </radialGradient>
            <pattern id="grid" width="50" height="50" patternUnits="userSpaceOnUse">
              <path d="M 50 0 L 0 0 0 50" fill="none" stroke="rgba(0,0,0,0.04)" strokeWidth="0.6"/>
            </pattern>
            <filter id="glow">
              <feGaussianBlur stdDeviation="2.5" result="coloredBlur"/>
              <feMerge>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="SourceGraphic"/>
              </feMerge>
            </filter>
            <filter id="shadow" x="-50%" y="-50%" width="200%" height="200%">
              <feDropShadow dx="0" dy="1" stdDeviation="1.5" floodOpacity="0.12"/>
            </filter>
            <filter id="node-glow" x="-100%" y="-100%" width="300%" height="300%">
              <feGaussianBlur stdDeviation="4" result="blur"/>
              <feMerge>
                <feMergeNode in="blur"/>
                <feMergeNode in="SourceGraphic"/>
              </feMerge>
            </filter>
          </defs>

          <rect width={svgWidth} height={svgHeight} fill="url(#bgGrad)"/>
          <rect width={svgWidth} height={svgHeight} fill="url(#grid)"/>

          {/* Road shadows layer */}
          <g className="road-shadows" opacity="0.08">
            {graphData.edges.map((edge) => {
              const fromNode = graphData.nodes.find(n => n.id === edge.from);
              const toNode = graphData.nodes.find(n => n.id === edge.to);
              if (!fromNode || !toNode) return null;
              const from = toSVG(fromNode.x, fromNode.y);
              const to = toSVG(toNode.x, toNode.y);
              return (
                <line key={`sh-${edge.id}`}
                  x1={from.x} y1={from.y} x2={to.x} y2={to.y}
                  stroke="#1D1D1F" strokeWidth="9" strokeLinecap="round"/>
              );
            })}
          </g>

          {/* Road fill layer */}
          <g className="roads">
            {graphData.edges.map((edge) => {
              const fromNode = graphData.nodes.find(n => n.id === edge.from);
              const toNode = graphData.nodes.find(n => n.id === edge.to);
              if (!fromNode || !toNode) return null;

              const from = toSVG(fromNode.x, fromNode.y);
              const to = toSVG(toNode.x, toNode.y);
              const edgeColor = getRoadColor(edge);
              const isHovered = hoveredEdge === edge.id;
              const stats = getEdgeStats(edge);

              return (
                <g key={edge.id}>
                  {/* Road fill */}
                  <line
                    x1={from.x} y1={from.y} x2={to.x} y2={to.y}
                    stroke={edgeColor}
                    strokeWidth={isHovered ? "7" : "5"}
                    strokeLinecap="round"
                    opacity={isHovered ? "0.95" : "0.7"}
                    filter={isHovered ? "url(#glow)" : ""}
                    onMouseEnter={() => setHoveredEdge(edge.id)}
                    onMouseLeave={() => setHoveredEdge(null)}
                    style={{ cursor: 'pointer', transition: 'all 0.15s' }}
                  />
                  {/* Center highlight */}
                  <line
                    x1={from.x} y1={from.y} x2={to.x} y2={to.y}
                    stroke="white" strokeWidth="1" strokeLinecap="round"
                    opacity="0.12" pointerEvents="none"
                  />
                  {isHovered && stats && (
                    <text
                      x={(from.x + to.x) / 2} y={(from.y + to.y) / 2 - 10}
                      fontSize="10" fontWeight="600" fill={colors.text}
                      textAnchor="middle" pointerEvents="none"
                      style={{ textShadow: '0 0 5px white, 0 0 10px white' }}
                    >
                      {edge.distance?.toFixed(1)} km · {stats.loadPercent}%
                    </text>
                  )}
                </g>
              );
            })}
          </g>

          {/* Major road name labels */}
          <g className="road-labels" opacity="0.2" pointerEvents="none">
            {[
              { label: 'WILSON BLVD', x: 6.0, y: 8.15, rot: 0 },
              { label: 'LEE HWY', x: 5.0, y: 9.15, rot: 0 },
              { label: 'FAIRFAX DR', x: 5.0, y: 7.15, rot: 0 },
              { label: 'ARLINGTON BLVD (RT 50)', x: 5.5, y: 5.18, rot: 0 },
              { label: 'COLUMBIA PIKE', x: 4.5, y: 2.68, rot: 0 },
              { label: 'N GLEBE RD', x: 2.78, y: 6.5, rot: -90 },
              { label: 'N COURTHOUSE RD', x: 7.28, y: 6.0, rot: -90 },
              { label: 'N LYNN ST', x: 8.78, y: 6.0, rot: -90 },
            ].map((rd, i) => {
              const pos = toSVG(rd.x, rd.y);
              return (
                <text key={`rl-${i}`}
                  x={pos.x} y={pos.y}
                  fontSize="10" fontWeight="700" fill="#1D1D1F"
                  textAnchor="middle" letterSpacing="2"
                  transform={rd.rot ? `rotate(${rd.rot}, ${pos.x}, ${pos.y})` : undefined}
                >
                  {rd.label}
                </text>
              );
            })}
          </g>

          {/* Nodes */}
          <g className="nodes">
            {graphData.nodes.map((node) => {
              const pos = toSVG(node.x, node.y);
              const isIntersection = node.type === 'INTERSECTION';
              const color = isIntersection ? colors.intersection : colors.boundary;
              const outerR = isIntersection ? 12 : 8;
              const innerR = isIntersection ? 7 : 5;
              const isSelected = selectedNode?.id === node.id;
              const isHoveredNode = hoveredEdge === `node-${node.id}`;
              const signal = isIntersection ? getNodeSignal(node.id) : null;

              return (
                <g key={node.id}
                  onMouseEnter={() => setHoveredEdge(`node-${node.id}`)}
                  onMouseLeave={() => setHoveredEdge(null)}
                  onClick={() => setSelectedNode(node)}
                  style={{ cursor: 'pointer' }}
                >
                  {/* Outer glow ring */}
                  <circle cx={pos.x} cy={pos.y} r={outerR}
                    fill="none" stroke={color} strokeWidth="1.5"
                    opacity={isSelected ? 0.6 : 0.2}/>
                  {/* Inner filled circle */}
                  <circle cx={pos.x} cy={pos.y} r={innerR}
                    fill={color} stroke="white" strokeWidth="2"
                    filter="url(#shadow)"
                    style={{ transition: 'all 0.15s' }}
                  />
                  {/* Node ID label */}
                  <text
                    x={pos.x} y={pos.y + 3.5}
                    fontSize="8" fontWeight="700" fill="white"
                    textAnchor="middle" pointerEvents="none"
                  >
                    {node.id}
                  </text>
                  {/* Node name — only on hover */}
                  {isHoveredNode && (
                    <text
                      x={pos.x} y={pos.y - outerR - 6}
                      fontSize="10" fill={colors.text} fontWeight="600"
                      textAnchor="middle" pointerEvents="none"
                      style={{ textShadow: '0 0 6px white, 0 0 12px white' }}
                    >
                      {node.name.length > 30 ? node.name.substring(0, 30) + '...' : node.name}
                    </text>
                  )}

                  {/* Traffic Signal Light */}
                  {signal && (
                    <g transform={`translate(${pos.x + 14}, ${pos.y - 21})`}>
                      <rect width="16" height="38" rx="4" fill="rgba(29,29,31,0.85)"/>
                      <circle cx="8" cy="8" r="3.5"
                        fill={getSignalColor(signal.currentState || signal.nsState, 'RED')}
                        opacity={((signal.currentState || signal.nsState) === 'RED') ? 1 : 0.15}
                      />
                      <circle cx="8" cy="19" r="3.5"
                        fill={getSignalColor(signal.currentState || signal.nsState, 'YELLOW')}
                        opacity={((signal.currentState || signal.nsState) === 'YELLOW') ? 1 : 0.15}
                      />
                      <circle cx="8" cy="30" r="3.5"
                        fill={getSignalColor(signal.currentState || signal.nsState, 'GREEN')}
                        opacity={((signal.currentState || signal.nsState) === 'GREEN') ? 1 : 0.15}
                      />
                    </g>
                  )}
                </g>
              );
            })}
          </g>

          {/* Vehicles */}
          <g className="vehicles">
            {vehicles.map((vehicle) => {
              const fromNode = graphData.nodes.find(n => n.id === vehicle.from);
              const toNode = graphData.nodes.find(n => n.id === vehicle.to);
              if (!fromNode || !toNode) return null;

              const from = toSVG(fromNode.x, fromNode.y);
              const to = toSVG(toNode.x, toNode.y);
              const x = from.x + (to.x - from.x) * vehicle.progress;
              const y = from.y + (to.y - from.y) * vehicle.progress;
              const vehicleColor = getVehicleColor(vehicle.state);

              return (
                <g key={vehicle.flowId}>
                  <circle cx={x} cy={y} r="6" fill={vehicleColor}
                    stroke="white" strokeWidth="1.5" filter="url(#glow)"
                  />
                  <circle cx={x + 9} cy={y - 9} r="7"
                    fill="white" stroke={vehicleColor} strokeWidth="1.5"/>
                  <text x={x + 9} y={y - 6} fontSize="9" fontWeight="700"
                    fill={vehicleColor} textAnchor="middle">
                    {vehicle.numberOfCars}
                  </text>
                </g>
              );
            })}
          </g>
        </svg>

        {/* Selected node details */}
        {selectedNode && (
          <div style={s.detailsPanel}>
            <div style={s.detailsHeader}>
              <h4 style={s.detailsTitle}>Node Details</h4>
              <button onClick={() => setSelectedNode(null)} style={s.closeButton}>
                <span style={{fontSize: '18px', lineHeight: '1'}}>&#215;</span>
              </button>
            </div>
            <div style={s.detailsContent}>
              <div style={s.detailRow}>
                <span style={s.detailLabel}>ID</span>
                <span style={s.detailValue}>{selectedNode.id}</span>
              </div>
              <div style={s.detailRow}>
                <span style={s.detailLabel}>Name</span>
                <span style={s.detailValue}>{selectedNode.name}</span>
              </div>
              <div style={s.detailRow}>
                <span style={s.detailLabel}>Type</span>
                <span style={{
                  ...s.badge,
                  background: selectedNode.type === 'INTERSECTION' ? colors.intersection : colors.boundary
                }}>
                  {selectedNode.type}
                </span>
              </div>
              <div style={s.detailRow}>
                <span style={s.detailLabel}>Position</span>
                <span style={s.detailValue}>
                  ({selectedNode.x.toFixed(1)}, {selectedNode.y.toFixed(1)})
                </span>
              </div>
            </div>
          </div>
        )}
      </div>

      <style>{spinnerCSS}</style>
    </div>
  );
};

const spinnerCSS = `
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
`;

const s = {
  container: {
    width: '100%',
    background: '#FFFFFF',
    borderRadius: '24px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
    border: '1px solid rgba(0,0,0,0.06)',
    overflow: 'hidden',
  },
  header: {
    background: '#FFFFFF',
    padding: '24px 32px',
    borderBottom: '1px solid rgba(0,0,0,0.06)',
  },
  titleSection: {
    marginBottom: '16px',
  },
  title: {
    margin: 0,
    fontSize: '22px',
    fontWeight: '600',
    color: '#1D1D1F',
    letterSpacing: '-0.26px',
  },
  subtitle: {
    margin: '4px 0 0 0',
    fontSize: '13px',
    color: '#86868B',
  },
  statsBar: {
    display: 'flex',
    gap: '12px',
  },
  statCard: {
    background: '#F5F5F7',
    borderRadius: '12px',
    padding: '12px 20px',
    minWidth: '100px',
    textAlign: 'center',
    border: '1px solid rgba(0,0,0,0.04)',
  },
  statValue: {
    fontSize: '24px',
    fontWeight: '700',
    color: '#0071E3',
    lineHeight: '1',
  },
  statLabel: {
    fontSize: '11px',
    marginTop: '4px',
    color: '#86868B',
    textTransform: 'uppercase',
    letterSpacing: '0.4px',
    fontWeight: '500',
  },
  mapWrapper: {
    position: 'relative',
    padding: '24px',
    background: '#F5F5F7',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    overflow: 'visible',
  },
  svg: {
    width: '100%',
    height: 'auto',
    background: 'white',
    borderRadius: '16px',
    boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
    userSelect: 'none',
  },
  legend: {
    position: 'absolute',
    top: '32px',
    left: '32px',
    background: 'rgba(255,255,255,0.85)',
    backdropFilter: 'saturate(180%) blur(20px)',
    WebkitBackdropFilter: 'saturate(180%) blur(20px)',
    borderRadius: '16px',
    padding: '16px 20px',
    boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
    border: '1px solid rgba(255,255,255,0.5)',
    minWidth: '180px',
    zIndex: 10,
  },
  legendTitle: {
    margin: '0 0 12px 0',
    fontSize: '13px',
    fontWeight: '600',
    color: '#1D1D1F',
    textTransform: 'uppercase',
    letterSpacing: '0.4px',
  },
  legendItems: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
  },
  legendItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  legendDot: {
    width: '10px',
    height: '10px',
    borderRadius: '50%',
    flexShrink: 0,
  },
  legendBar: {
    width: '24px',
    height: '3px',
    borderRadius: '2px',
    flexShrink: 0,
  },
  legendText: {
    fontSize: '12px',
    color: '#86868B',
    fontWeight: '500',
  },
  legendSep: {
    height: '1px',
    background: 'rgba(0,0,0,0.06)',
    margin: '2px 0',
  },
  detailsPanel: {
    position: 'absolute',
    bottom: '32px',
    right: '32px',
    background: 'rgba(255,255,255,0.9)',
    backdropFilter: 'saturate(180%) blur(20px)',
    WebkitBackdropFilter: 'saturate(180%) blur(20px)',
    borderRadius: '16px',
    boxShadow: '0 12px 40px rgba(0,0,0,0.12)',
    border: '1px solid rgba(255,255,255,0.5)',
    minWidth: '280px',
    zIndex: 10,
    overflow: 'hidden',
  },
  detailsHeader: {
    padding: '16px 20px',
    borderBottom: '1px solid rgba(0,0,0,0.06)',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  detailsTitle: {
    margin: 0,
    fontSize: '15px',
    fontWeight: '600',
    color: '#1D1D1F',
  },
  closeButton: {
    background: '#F5F5F7',
    border: '1px solid rgba(0,0,0,0.06)',
    color: '#86868B',
    width: '28px',
    height: '28px',
    borderRadius: '8px',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    transition: 'background 0.2s',
  },
  detailsContent: {
    padding: '16px 20px',
  },
  detailRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '8px 0',
    borderBottom: '1px solid rgba(0,0,0,0.04)',
  },
  detailLabel: {
    fontSize: '13px',
    color: '#86868B',
    fontWeight: '500',
  },
  detailValue: {
    fontSize: '13px',
    color: '#1D1D1F',
    fontWeight: '600',
  },
  badge: {
    padding: '3px 10px',
    borderRadius: '6px',
    color: 'white',
    fontSize: '11px',
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: '0.3px',
  },
  loadingContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '80px 40px',
    gap: '16px',
  },
  spinner: {
    width: '40px',
    height: '40px',
    border: '3px solid rgba(0,0,0,0.06)',
    borderTop: '3px solid #0071E3',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  loadingText: {
    fontSize: '15px',
    color: '#86868B',
    fontWeight: '500',
  },
  errorContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '80px 40px',
    gap: '12px',
  },
  errorIcon: {
    width: '40px',
    height: '40px',
    borderRadius: '50%',
    background: '#FF453A',
    color: 'white',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '20px',
    fontWeight: '700',
    margin: 0,
  },
  errorText: {
    fontSize: '15px',
    color: '#FF453A',
    fontWeight: '500',
    textAlign: 'center',
    margin: 0,
  },
  retryButton: {
    padding: '10px 24px',
    background: '#0071E3',
    color: 'white',
    border: 'none',
    borderRadius: '980px',
    fontSize: '15px',
    fontWeight: '500',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
};

export default MapVisualization;
