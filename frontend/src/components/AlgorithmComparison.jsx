import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { simulationAPI } from '../services/api';
import api from '../services/api';
import { computeCoordBounds, mapToSVG, colors } from '../utils/mapUtils';

const ALGO_COLORS = {
  dijkstra: '#0071E3',
  aStar: '#BF5AF2',
  dynamic: '#30D158',
};

const ALGO_LABELS = {
  dijkstra: 'Dijkstra',
  aStar: 'A* Search',
  dynamic: 'Dynamic Routing',
};

const AlgorithmComparison = () => {
  const [graphData, setGraphData] = useState(null);
  const [startNode, setStartNode] = useState('');
  const [endNode, setEndNode] = useState('');
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [animated, setAnimated] = useState(false);

  const svgWidth = 1000;
  const svgHeight = 500;
  const padding = 40;

  useEffect(() => {
    loadGraph();
  }, []);

  const loadGraph = async () => {
    try {
      const response = await api.get('/simulation/graph');
      setGraphData(response.data);
    } catch (err) {
      // Graph not available yet
    }
  };

  const coordBounds = useMemo(() => {
    if (!graphData || !graphData.nodes) return null;
    return computeCoordBounds(graphData.nodes);
  }, [graphData]);

  const toSVG = useCallback((x, y) => {
    return mapToSVG(x, y, coordBounds, svgWidth, svgHeight, padding);
  }, [coordBounds]);

  const nodeOptions = useMemo(() => {
    if (!graphData || !graphData.nodes) return [];
    return graphData.nodes.map(n => ({ id: n.id, name: n.name, type: n.type }));
  }, [graphData]);

  const handleCompare = async () => {
    if (!startNode || !endNode) {
      setError('Please select both start and end nodes');
      return;
    }
    if (startNode === endNode) {
      setError('Start and end nodes must be different');
      return;
    }
    setError('');
    setLoading(true);
    setAnimated(false);
    try {
      const response = await simulationAPI.compareAlgorithms(startNode, endNode);
      setResults(response.data);
      // Trigger animation
      setTimeout(() => setAnimated(true), 50);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to compare algorithms');
    } finally {
      setLoading(false);
    }
  };

  // Build path coordinates for SVG
  const getPathCoords = (pathNodeIds) => {
    if (!pathNodeIds || !graphData) return [];
    return pathNodeIds.map(id => {
      const node = graphData.nodes.find(n => n.id === id);
      if (!node) return null;
      return toSVG(node.x, node.y);
    }).filter(Boolean);
  };

  const buildPathD = (coords) => {
    if (coords.length < 2) return '';
    return coords.map((c, i) => `${i === 0 ? 'M' : 'L'} ${c.x} ${c.y}`).join(' ');
  };

  // Compute total path length for animation
  const computePathLength = (coords) => {
    let total = 0;
    for (let i = 1; i < coords.length; i++) {
      const dx = coords[i].x - coords[i - 1].x;
      const dy = coords[i].y - coords[i - 1].y;
      total += Math.sqrt(dx * dx + dy * dy);
    }
    return total;
  };

  // Find best value per metric
  const getBest = (key, lowerIsBetter = true) => {
    if (!results) return '';
    const algos = ['dijkstra', 'aStar', 'dynamic'];
    const vals = algos.map(a => results[a]?.[key]).filter(v => v != null && v > 0);
    if (vals.length === 0) return '';
    const best = lowerIsBetter ? Math.min(...vals) : Math.max(...vals);
    return algos.find(a => results[a]?.[key] === best) || '';
  };

  return (
    <div style={s.container}>
      <div style={s.header}>
        <div>
          <h2 style={s.title}>Algorithm Comparison</h2>
          <p style={s.subtitle}>Compare Dijkstra, A*, and Dynamic Routing side by side</p>
        </div>
      </div>

      {/* Controls */}
      <div style={s.controls}>
        <div style={s.selectGroup}>
          <label style={s.label}>Start Node</label>
          <select
            value={startNode}
            onChange={(e) => setStartNode(e.target.value)}
            style={s.select}
          >
            <option value="">Select start...</option>
            {nodeOptions.map(n => (
              <option key={n.id} value={n.id}>{n.id} — {n.name}</option>
            ))}
          </select>
        </div>

        <div style={s.arrow}>&#8594;</div>

        <div style={s.selectGroup}>
          <label style={s.label}>End Node</label>
          <select
            value={endNode}
            onChange={(e) => setEndNode(e.target.value)}
            style={s.select}
          >
            <option value="">Select end...</option>
            {nodeOptions.map(n => (
              <option key={n.id} value={n.id}>{n.id} — {n.name}</option>
            ))}
          </select>
        </div>

        <button
          onClick={handleCompare}
          disabled={loading || !startNode || !endNode}
          style={{
            ...s.compareBtn,
            opacity: (loading || !startNode || !endNode) ? 0.5 : 1,
          }}
        >
          {loading ? 'Computing...' : 'Compare'}
        </button>
      </div>

      {error && <p style={s.error}>{error}</p>}

      {/* Mini Map with Paths */}
      {graphData && results && (
        <div style={s.mapContainer}>
          <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} style={s.svg}>
            <defs>
              <pattern id="miniGrid" width="40" height="40" patternUnits="userSpaceOnUse">
                <path d="M 40 0 L 0 0 0 40" fill="none" stroke="rgba(0,0,0,0.02)" strokeWidth="0.5"/>
              </pattern>
            </defs>
            <rect width={svgWidth} height={svgHeight} fill="white"/>
            <rect width={svgWidth} height={svgHeight} fill="url(#miniGrid)"/>

            {/* Base network (faded) */}
            <g opacity="0.15">
              {graphData.edges.map((edge) => {
                const fromNode = graphData.nodes.find(n => n.id === edge.from);
                const toNode = graphData.nodes.find(n => n.id === edge.to);
                if (!fromNode || !toNode) return null;
                const from = toSVG(fromNode.x, fromNode.y);
                const to = toSVG(toNode.x, toNode.y);
                return (
                  <line key={edge.id} x1={from.x} y1={from.y} x2={to.x} y2={to.y}
                    stroke="#86868B" strokeWidth="1.5" strokeLinecap="round"/>
                );
              })}
            </g>

            {/* Base nodes (faded) */}
            <g opacity="0.3">
              {graphData.nodes.map((node) => {
                const pos = toSVG(node.x, node.y);
                return (
                  <circle key={node.id} cx={pos.x} cy={pos.y} r="3"
                    fill={node.type === 'INTERSECTION' ? '#0071E3' : '#86868B'}/>
                );
              })}
            </g>

            {/* Algorithm Paths */}
            {['dijkstra', 'aStar', 'dynamic'].map((algo) => {
              const data = results[algo];
              if (!data || !data.reachable) return null;
              const coords = getPathCoords(data.path);
              const d = buildPathD(coords);
              const pathLen = computePathLength(coords);
              const dashStyle = algo === 'aStar' ? '10,5' : algo === 'dynamic' ? '5,5' : 'none';

              return (
                <path
                  key={algo}
                  d={d}
                  fill="none"
                  stroke={ALGO_COLORS[algo]}
                  strokeWidth="4"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeDasharray={animated ? dashStyle : `${pathLen}`}
                  strokeDashoffset={animated ? '0' : `${pathLen}`}
                  style={{
                    transition: 'stroke-dashoffset 1.5s ease-in-out',
                    filter: `drop-shadow(0 0 4px ${ALGO_COLORS[algo]}40)`,
                  }}
                />
              );
            })}

            {/* Start & End markers */}
            {(() => {
              const sNode = graphData.nodes.find(n => n.id === startNode);
              const eNode = graphData.nodes.find(n => n.id === endNode);
              if (!sNode || !eNode) return null;
              const sPos = toSVG(sNode.x, sNode.y);
              const ePos = toSVG(eNode.x, eNode.y);
              return (
                <>
                  <circle cx={sPos.x} cy={sPos.y} r="10" fill="none"
                    stroke="#30D158" strokeWidth="3" opacity="0.8"/>
                  <circle cx={sPos.x} cy={sPos.y} r="4" fill="#30D158"/>
                  <text x={sPos.x} y={sPos.y - 16} textAnchor="middle"
                    fontSize="11" fontWeight="700" fill="#30D158">START</text>

                  <circle cx={ePos.x} cy={ePos.y} r="10" fill="none"
                    stroke="#FF453A" strokeWidth="3" opacity="0.8"/>
                  <circle cx={ePos.x} cy={ePos.y} r="4" fill="#FF453A"/>
                  <text x={ePos.x} y={ePos.y - 16} textAnchor="middle"
                    fontSize="11" fontWeight="700" fill="#FF453A">END</text>
                </>
              );
            })()}

            {/* Node labels on path */}
            {graphData.nodes.map((node) => {
              const pos = toSVG(node.x, node.y);
              return (
                <text key={`lbl-${node.id}`} x={pos.x} y={pos.y - 6}
                  textAnchor="middle" fontSize="7" fill="#86868B" fontWeight="500"
                  style={{ textShadow: '0 0 3px white' }}>
                  {node.id}
                </text>
              );
            })}
          </svg>

          {/* Path Legend */}
          <div style={s.pathLegend}>
            {['dijkstra', 'aStar', 'dynamic'].map(algo => (
              <div key={algo} style={s.legendItem}>
                <div style={{
                  width: '20px', height: '3px', borderRadius: '2px',
                  background: ALGO_COLORS[algo],
                  borderStyle: algo === 'aStar' ? 'dashed' : algo === 'dynamic' ? 'dotted' : 'solid',
                }}/>
                <span style={s.legendText}>{ALGO_LABELS[algo]}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Comparison Table */}
      {results && (
        <div style={s.tableContainer}>
          <table style={s.table}>
            <thead>
              <tr>
                <th style={s.th}>Metric</th>
                {['dijkstra', 'aStar', 'dynamic'].map(algo => (
                  <th key={algo} style={{...s.th, color: ALGO_COLORS[algo]}}>
                    {ALGO_LABELS[algo]}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              <tr>
                <td style={s.td}>Path</td>
                {['dijkstra', 'aStar', 'dynamic'].map(algo => (
                  <td key={algo} style={s.td}>
                    <span style={s.pathText}>
                      {results[algo]?.reachable ? results[algo].path.join(' → ') : 'No path'}
                    </span>
                  </td>
                ))}
              </tr>
              <tr>
                <td style={s.td}>Distance</td>
                {['dijkstra', 'aStar', 'dynamic'].map(algo => {
                  const best = getBest('distance', true);
                  const val = results[algo]?.distance;
                  return (
                    <td key={algo} style={{...s.td, ...(best === algo ? s.bestCell : {})}}>
                      {val != null ? `${val.toFixed(2)} km` : '--'}
                    </td>
                  );
                })}
              </tr>
              <tr>
                <td style={s.td}>Nodes in Path</td>
                {['dijkstra', 'aStar', 'dynamic'].map(algo => {
                  const best = getBest('nodesInPath', true);
                  const val = results[algo]?.nodesInPath;
                  return (
                    <td key={algo} style={{...s.td, ...(best === algo ? s.bestCell : {})}}>
                      {val != null ? val : '--'}
                    </td>
                  );
                })}
              </tr>
              <tr>
                <td style={s.td}>Computation Time</td>
                {['dijkstra', 'aStar', 'dynamic'].map(algo => {
                  const best = getBest('computationTimeMs', true);
                  const val = results[algo]?.computationTimeMs;
                  return (
                    <td key={algo} style={{...s.td, ...(best === algo ? s.bestCell : {})}}>
                      {val != null ? `${val.toFixed(3)} ms` : '--'}
                    </td>
                  );
                })}
              </tr>
            </tbody>
          </table>
        </div>
      )}

      {/* Empty State */}
      {!results && !loading && (
        <div style={s.emptyState}>
          <p style={s.emptyTitle}>Select Start & End Nodes</p>
          <p style={s.emptyDesc}>
            Choose two nodes from the network to compare how Dijkstra, A*, and Dynamic Routing
            find different paths between them.
          </p>
        </div>
      )}
    </div>
  );
};

const s = {
  container: {
    background: '#FFFFFF',
    borderRadius: '24px',
    padding: '28px 32px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
    border: '1px solid rgba(0,0,0,0.06)',
  },
  header: {
    marginBottom: '20px',
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
  controls: {
    display: 'flex',
    alignItems: 'flex-end',
    gap: '16px',
    marginBottom: '24px',
    flexWrap: 'wrap',
  },
  selectGroup: {
    flex: '1 1 200px',
  },
  label: {
    display: 'block',
    fontSize: '12px',
    fontWeight: '600',
    color: '#86868B',
    textTransform: 'uppercase',
    letterSpacing: '0.4px',
    marginBottom: '6px',
  },
  select: {
    width: '100%',
    padding: '10px 14px',
    borderRadius: '12px',
    border: '1px solid rgba(0,0,0,0.1)',
    fontSize: '14px',
    fontFamily: 'inherit',
    background: '#F5F5F7',
    color: '#1D1D1F',
    outline: 'none',
    appearance: 'auto',
    transition: 'border-color 0.2s',
  },
  arrow: {
    fontSize: '20px',
    color: '#86868B',
    paddingBottom: '10px',
    flexShrink: 0,
  },
  compareBtn: {
    padding: '10px 28px',
    background: '#0071E3',
    color: 'white',
    border: 'none',
    borderRadius: '980px',
    fontSize: '15px',
    fontWeight: '500',
    cursor: 'pointer',
    transition: 'all 0.2s',
    fontFamily: 'inherit',
    flexShrink: 0,
    whiteSpace: 'nowrap',
  },
  error: {
    color: '#FF453A',
    fontSize: '13px',
    marginBottom: '16px',
    fontWeight: '500',
  },
  mapContainer: {
    marginBottom: '24px',
    borderRadius: '16px',
    overflow: 'hidden',
    background: '#F5F5F7',
    border: '1px solid rgba(0,0,0,0.04)',
    padding: '16px',
  },
  svg: {
    width: '100%',
    height: 'auto',
    borderRadius: '12px',
    background: 'white',
    boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
  },
  pathLegend: {
    display: 'flex',
    justifyContent: 'center',
    gap: '24px',
    marginTop: '12px',
  },
  legendItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  legendText: {
    fontSize: '12px',
    color: '#86868B',
    fontWeight: '500',
  },
  tableContainer: {
    borderRadius: '16px',
    overflow: 'hidden',
    border: '1px solid rgba(0,0,0,0.06)',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    fontSize: '14px',
  },
  th: {
    padding: '14px 16px',
    textAlign: 'left',
    fontWeight: '600',
    fontSize: '13px',
    background: '#F5F5F7',
    borderBottom: '1px solid rgba(0,0,0,0.06)',
    letterSpacing: '-0.01em',
  },
  td: {
    padding: '12px 16px',
    borderBottom: '1px solid rgba(0,0,0,0.04)',
    color: '#1D1D1F',
    fontWeight: '500',
  },
  bestCell: {
    fontWeight: '700',
    position: 'relative',
  },
  pathText: {
    fontSize: '12px',
    fontFamily: 'var(--apple-mono, monospace)',
    color: '#86868B',
  },
  emptyState: {
    padding: '48px 24px',
    textAlign: 'center',
  },
  emptyTitle: {
    fontSize: '17px',
    fontWeight: '600',
    color: '#1D1D1F',
    margin: '0 0 8px 0',
  },
  emptyDesc: {
    fontSize: '14px',
    color: '#86868B',
    margin: 0,
    maxWidth: '480px',
    marginLeft: 'auto',
    marginRight: 'auto',
    lineHeight: '1.5',
  },
};

export default AlgorithmComparison;
