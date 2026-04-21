import React, { useMemo } from 'react';
import {
  RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis,
  Radar, Legend, ResponsiveContainer, Tooltip,
} from 'recharts';

/**
 * 5-axis normalized radar chart comparing FIXED / ADAPTIVE / AI modes.
 * All axes are mapped so that "higher = better" — larger filled area = better performance.
 */

const MODE_DEFS = [
  { key: 'Fixed Timing',   color: '#FF453A' },
  { key: 'Adaptive',       color: '#FF9F0A' },
  { key: 'Green Wave',     color: '#30D158' },
];

const AXES = [
  { id: 'throughput',  label: 'Throughput' },
  { id: 'speedFlow',   label: 'Speed Flow' },
  { id: 'freeRoads',   label: 'Free Roads' },
  { id: 'movement',    label: 'Movement' },
  { id: 'headroom',    label: 'Headroom' },
];

const ModeRadarChart = ({ metrics }) => {
  // metrics: array of 3 objects { mode, completedFlows, speedReductionRatio, congestedEdgeRatio, stoppedVehicleRate, networkOccupancy, ... }
  const chartData = useMemo(() => {
    if (!Array.isArray(metrics) || metrics.length !== 3) return [];

    // Normalize throughput across modes so the best mode reaches 1.0
    const maxCompleted = Math.max(
      1,
      ...metrics.map(m => m.completedFlows || 0)
    );

    const byMode = metrics.map(m => ({
      throughput: Math.min(1, (m.completedFlows || 0) / maxCompleted),
      speedFlow:  clamp01(m.speedReductionRatio || 0),
      freeRoads:  clamp01(1 - (m.congestedEdgeRatio || 0)),
      movement:   clamp01(1 - (m.stoppedVehicleRate || 0)),
      headroom:   clamp01(1 - (m.networkOccupancy || 0)),
    }));

    return AXES.map(axis => {
      const row = { axis: axis.label };
      MODE_DEFS.forEach((mode, idx) => {
        row[mode.key] = Number(byMode[idx][axis.id].toFixed(3));
      });
      return row;
    });
  }, [metrics]);

  if (chartData.length === 0) {
    return (
      <div style={{ ...styles.container, textAlign: 'center', color: '#86868B' }}>
        Waiting for data to render radar chart...
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div>
          <div style={styles.title}>Multi-Dimensional Performance Radar</div>
          <div style={styles.subtitle}>
            All axes normalized to [0, 1] — higher is better · larger area = better overall
          </div>
        </div>
        <div style={styles.legendPill}>
          {MODE_DEFS.map(m => (
            <span key={m.key} style={{ ...styles.pillItem }}>
              <span style={{ ...styles.pillDot, background: m.color }} />
              {m.key}
            </span>
          ))}
        </div>
      </div>

      <ResponsiveContainer width="100%" height={360}>
        <RadarChart data={chartData} margin={{ top: 20, right: 40, bottom: 20, left: 40 }}>
          <PolarGrid gridType="polygon" stroke="rgba(0,0,0,0.08)" />
          <PolarAngleAxis
            dataKey="axis"
            tick={{ fill: '#1D1D1F', fontSize: 12, fontWeight: 600 }}
          />
          <PolarRadiusAxis
            angle={90}
            domain={[0, 1]}
            tick={{ fill: '#86868B', fontSize: 10 }}
            tickCount={5}
          />
          <Tooltip
            formatter={(value, name) => [(value * 100).toFixed(0) + '%', name]}
            contentStyle={{
              background: 'rgba(255,255,255,0.96)',
              border: '1px solid rgba(0,0,0,0.08)',
              borderRadius: 10,
              boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
              fontSize: 12,
            }}
          />
          {MODE_DEFS.map(mode => (
            <Radar
              key={mode.key}
              name={mode.key}
              dataKey={mode.key}
              stroke={mode.color}
              fill={mode.color}
              fillOpacity={0.22}
              strokeWidth={2}
              isAnimationActive={false}
            />
          ))}
          <Legend wrapperStyle={{ fontSize: 12 }} />
        </RadarChart>
      </ResponsiveContainer>
    </div>
  );
};

function clamp01(x) {
  if (Number.isNaN(x) || x == null) return 0;
  return Math.max(0, Math.min(1, x));
}

const styles = {
  container: {
    background: 'rgba(255,255,255,0.95)',
    backdropFilter: 'saturate(180%) blur(16px)',
    borderRadius: 14,
    padding: '16px 18px',
    marginBottom: 16,
    boxShadow: '0 2px 12px rgba(0,0,0,0.06)',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 6,
    flexWrap: 'wrap',
    gap: 8,
  },
  title: {
    fontSize: 15,
    fontWeight: 700,
    color: '#1D1D1F',
    letterSpacing: 0.3,
  },
  subtitle: {
    fontSize: 11,
    color: '#86868B',
    marginTop: 2,
  },
  legendPill: {
    display: 'flex',
    gap: 10,
    flexWrap: 'wrap',
  },
  pillItem: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: 5,
    fontSize: 11,
    color: '#1D1D1F',
    background: '#f5f5f7',
    padding: '3px 10px',
    borderRadius: 14,
    fontWeight: 600,
  },
  pillDot: {
    width: 8,
    height: 8,
    borderRadius: '50%',
    display: 'inline-block',
  },
};

export default ModeRadarChart;
