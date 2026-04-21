import React, { useEffect, useState } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import api from '../services/api';

/**
 * Three-line efficiency comparison chart.
 * Plots FIXED_TIME / TRAFFIC_ADAPTIVE / GREEN_WAVE efficiency over sim time.
 */
const ComparisonEfficiencyChart = ({ running }) => {
  const [rawData, setRawData] = useState([]);
  const [view, setView] = useState('recent'); // 'recent' | 'all'

  useEffect(() => {
    if (!running) return;
    const fetchTrend = async () => {
      try {
        const res = await api.get('/compare/efficiency/trend');
        if (Array.isArray(res.data)) setRawData(res.data);
      } catch (e) { /* ignore */ }
    };
    fetchTrend();
    const iv = setInterval(fetchTrend, 2000);
    return () => clearInterval(iv);
  }, [running]);

  // Show recent 30 data points by default, skipping the initialization spike
  const data = view === 'recent'
    ? rawData.slice(-30)
    : rawData.slice(2); // skip first 2 warmup points

  const formatTime = (value) => {
    const s = Math.floor(value);
    const mm = Math.floor(s / 60);
    const ss = s % 60;
    return `${mm}:${ss.toString().padStart(2, '0')}`;
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div>
          <div style={styles.title}>Efficiency Comparison Over Time</div>
          <div style={styles.subtitle}>
            {rawData.length > 0
              ? `${data.length} of ${rawData.length} records · every 15 sim-seconds`
              : 'Waiting for data...'}
          </div>
        </div>
        <div style={styles.viewToggle}>
          <button
            onClick={() => setView('recent')}
            style={{
              ...styles.toggleBtn,
              background: view === 'recent' ? '#0071E3' : 'transparent',
              color: view === 'recent' ? '#fff' : '#86868B',
            }}
          >
            Recent 30
          </button>
          <button
            onClick={() => setView('all')}
            style={{
              ...styles.toggleBtn,
              background: view === 'all' ? '#0071E3' : 'transparent',
              color: view === 'all' ? '#fff' : '#86868B',
            }}
          >
            Full History
          </button>
        </div>
      </div>
      <ResponsiveContainer width="100%" height={280}>
        <LineChart data={data} margin={{ top: 10, right: 24, left: 0, bottom: 8 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(0,0,0,0.06)" />
          <XAxis
            dataKey="timestamp"
            type="number"
            domain={['dataMin', 'dataMax']}
            tickFormatter={formatTime}
            tick={{ fill: '#86868B', fontSize: 12 }}
            label={{ value: 'Simulation time', position: 'insideBottom', offset: -4, fill: '#86868B', fontSize: 11 }}
          />
          <YAxis
            domain={['auto', 'auto']}
            tick={{ fill: '#86868B', fontSize: 12 }}
            label={{ value: 'Efficiency', angle: -90, position: 'insideLeft', fill: '#86868B', fontSize: 11 }}
          />
          <Tooltip
            labelFormatter={(value) => `Sim time ${formatTime(value)}`}
            formatter={(value, name) => [value.toFixed(2), name]}
            contentStyle={{
              background: 'rgba(255,255,255,0.96)',
              border: '1px solid rgba(0,0,0,0.08)',
              borderRadius: 10,
              boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
              fontSize: 12,
            }}
          />
          <Legend wrapperStyle={{ fontSize: 12, paddingTop: 6 }} />
          <Line
            type="monotone"
            dataKey="FIXED_TIME"
            name="Fixed Timing"
            stroke="#FF453A"
            strokeWidth={2.5}
            dot={{ r: 3, fill: '#FF453A' }}
            activeDot={{ r: 5 }}
            isAnimationActive={false}
          />
          <Line
            type="monotone"
            dataKey="TRAFFIC_ADAPTIVE"
            name="Adaptive (Webster)"
            stroke="#FF9F0A"
            strokeWidth={2.5}
            dot={{ r: 3, fill: '#FF9F0A' }}
            activeDot={{ r: 5 }}
            isAnimationActive={false}
          />
          <Line
            type="monotone"
            dataKey="GREEN_WAVE"
            name="Green Wave (Coordinated)"
            stroke="#30D158"
            strokeWidth={2.5}
            dot={{ r: 3, fill: '#30D158' }}
            activeDot={{ r: 5 }}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

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
    alignItems: 'baseline',
    marginBottom: 10,
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
  viewToggle: {
    display: 'flex',
    gap: 4,
    background: '#f0f0f2',
    borderRadius: 8,
    padding: 3,
  },
  toggleBtn: {
    border: 'none',
    borderRadius: 6,
    padding: '5px 12px',
    fontSize: 11,
    fontWeight: 600,
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
};

export default ComparisonEfficiencyChart;
