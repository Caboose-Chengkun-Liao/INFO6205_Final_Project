import React, { useState, useEffect, useCallback } from 'react';
import { simulationAPI } from '../services/api';
import api from '../services/api';
import Mini3DScene from './three/Mini3DScene';
import ComparisonEfficiencyChart from './ComparisonEfficiencyChart';
import ModeRadarChart from './ModeRadarChart';

const MODES = [
  { label: 'FIXED TIMING', color: '#FF453A', key: 'FIXED_TIME' },
  { label: 'ADAPTIVE', color: '#FF9F0A', key: 'TRAFFIC_ADAPTIVE' },
  { label: 'AI-OPTIMIZED', color: '#30D158', key: 'LEARNING_BASED' },
];

/**
 * Three side-by-side 3D scenes comparing signal control modes.
 * Each scene runs an independent simulation with identical traffic flows.
 */
const ComparisonView = ({ onBack }) => {
  const [running, setRunning] = useState(false);
  const [metrics, setMetrics] = useState([]);
  const [status, setStatus] = useState('Ready to compare');

  const startComparison = useCallback(async () => {
    try {
      setStatus('Initializing simulations...');
      // Ensure main simulation is initialized first (provides base graph)
      try { await simulationAPI.initialize(); } catch (e) { /* may already be initialized */ }

      const res = await api.post('/compare/start');
      if (res.data.error) {
        setStatus('Error: ' + res.data.error);
        return;
      }
      setRunning(true);
      setStatus('Running — observe differences between modes');
    } catch (err) {
      setStatus('Failed to start: ' + (err.response?.data?.error || err.message));
    }
  }, []);

  const stopComparison = useCallback(async () => {
    try {
      await api.post('/compare/stop');
    } catch (e) { /* ignore */ }
    setRunning(false);
    setMetrics([]);
    setStatus('Stopped');
  }, []);

  // Poll metrics
  useEffect(() => {
    if (!running) return;
    const interval = setInterval(async () => {
      try {
        const res = await api.get('/compare/metrics');
        if (Array.isArray(res.data)) setMetrics(res.data);
      } catch (e) { /* ignore */ }
    }, 2000);
    return () => clearInterval(interval);
  }, [running]);

  return (
    <div style={styles.container}>
      {/* Header */}
      <div style={styles.header}>
        <button onClick={onBack} style={styles.backBtn}>Back</button>
        <h2 style={styles.title}>Signal Mode Comparison</h2>
        <div style={styles.controls}>
          <span style={styles.status}>{status}</span>
          {!running ? (
            <button onClick={startComparison} style={styles.startBtn}>Start Comparison</button>
          ) : (
            <button onClick={stopComparison} style={styles.stopBtn}>Stop</button>
          )}
        </div>
      </div>

      {/* KPI comparison bar */}
      {metrics.length === 3 && (
        <div style={styles.metricsBar}>
          {metrics.map((m, i) => (
            <div key={i} style={{ ...styles.metricCard, borderTop: `3px solid ${MODES[i].color}` }}>
              <div style={styles.metricLabel}>{MODES[i].label}</div>
              <div style={styles.metricRow}>
                <span>Vehicles</span>
                <strong>{m.totalVehicles || 0}</strong>
              </div>
              <div style={styles.metricRow}>
                <span>Blocked</span>
                <strong style={{ color: (m.blockedFlows || 0) > 0 ? '#FF453A' : '#30D158' }}>
                  {m.blockedFlows || 0}
                </strong>
              </div>
              <div style={styles.metricRow}>
                <span>Congested Roads</span>
                <strong style={{ color: (m.congestedEdges || 0) > 5 ? '#FF453A' : '#30D158' }}>
                  {m.congestedEdges || 0}
                </strong>
              </div>
              <div style={styles.metricRow}>
                <span>Avg Progress</span>
                <strong>{((m.avgProgress || 0) * 100).toFixed(0)}%</strong>
              </div>
              <div style={styles.metricRow}>
                <span>Completed</span>
                <strong>{m.completedFlows || 0}</strong>
              </div>
              <div style={styles.metricRow}>
                <span>Sim Time</span>
                <strong>{m.currentTime || 0}s</strong>
              </div>

              {/* --- Network-level metrics (new) --- */}
              <div style={styles.metricSeparator} />
              <div style={styles.metricRow}>
                <span>Net Occupancy</span>
                <strong style={{ color: (m.networkOccupancy || 0) > 0.5 ? '#FF453A' : '#30D158' }}>
                  {((m.networkOccupancy || 0) * 100).toFixed(0)}%
                </strong>
              </div>
              <div style={styles.metricRow}>
                <span>Congested %</span>
                <strong style={{ color: (m.congestedEdgeRatio || 0) > 0.3 ? '#FF453A' : '#30D158' }}>
                  {((m.congestedEdgeRatio || 0) * 100).toFixed(0)}%
                </strong>
              </div>
              <div style={styles.metricRow}>
                <span>Avg Queue</span>
                <strong>{(m.avgQueueLength || 0).toFixed(1)}</strong>
              </div>
              <div style={styles.metricRow}>
                <span>Stopped %</span>
                <strong style={{ color: (m.stoppedVehicleRate || 0) > 0.2 ? '#FF453A' : '#30D158' }}>
                  {((m.stoppedVehicleRate || 0) * 100).toFixed(0)}%
                </strong>
              </div>
              <div style={styles.metricRow}>
                <span>Speed Flow</span>
                <strong style={{ color: (m.speedReductionRatio || 0) > 0.7 ? '#30D158' : '#FF453A' }}>
                  {((m.speedReductionRatio || 0) * 100).toFixed(0)}%
                </strong>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Three side-by-side 3D scenes */}
      <div style={styles.scenesContainer}>
        {MODES.map((mode, i) => (
          <div key={mode.key} style={styles.sceneWrapper}>
            <Mini3DScene
              index={i}
              label={mode.label}
              color={mode.color}
              running={running}
            />
          </div>
        ))}
      </div>

      {/* Efficiency trend comparison chart */}
      {running && <ComparisonEfficiencyChart running={running} />}

      {/* Multi-dimensional radar chart */}
      {running && metrics.length === 3 && <ModeRadarChart metrics={metrics} />}
    </div>
  );
};

const styles = {
  container: {
    padding: '0 16px 16px',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    padding: '12px 0',
  },
  backBtn: {
    background: '#86868B',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    padding: '6px 16px',
    fontSize: 13,
    fontWeight: 600,
    cursor: 'pointer',
  },
  title: {
    fontSize: 18,
    fontWeight: 700,
    color: '#1D1D1F',
    margin: 0,
    flex: 1,
  },
  controls: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
  },
  status: {
    fontSize: 12,
    color: '#86868B',
  },
  startBtn: {
    background: 'linear-gradient(135deg, #0071E3, #0055b3)',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    padding: '8px 20px',
    fontSize: 13,
    fontWeight: 600,
    cursor: 'pointer',
  },
  stopBtn: {
    background: 'linear-gradient(135deg, #FF453A, #cc3333)',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    padding: '8px 20px',
    fontSize: 13,
    fontWeight: 600,
    cursor: 'pointer',
  },
  metricsBar: {
    display: 'flex',
    gap: 12,
    marginBottom: 12,
  },
  metricCard: {
    flex: 1,
    background: 'rgba(255,255,255,0.92)',
    backdropFilter: 'saturate(180%) blur(16px)',
    borderRadius: 10,
    padding: '10px 14px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
  },
  metricLabel: {
    fontSize: 11,
    fontWeight: 700,
    color: '#1D1D1F',
    marginBottom: 6,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  metricRow: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: 12,
    color: '#555',
    marginTop: 3,
  },
  metricSeparator: {
    height: 1,
    background: 'rgba(0,0,0,0.06)',
    marginTop: 8,
    marginBottom: 4,
  },
  scenesContainer: {
    display: 'flex',
    gap: 8,
    height: '55vh',
    minHeight: 400,
  },
  sceneWrapper: {
    flex: 1,
    borderRadius: 10,
    overflow: 'hidden',
    boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
  },
};

export default ComparisonView;
