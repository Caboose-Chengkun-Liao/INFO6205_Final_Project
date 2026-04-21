import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

/**
 * Two operational KPI panels derived from the client-accumulated metrics history:
 *   - Completed flows over sim time
 *   - Stopped vehicle percentage over sim time
 *
 * `history` is an array of snapshots { timestamp, FIXED_TIME: {...}, TRAFFIC_ADAPTIVE: {...}, GREEN_WAVE: {...} }
 * where each inner object has `completedFlows` and `stoppedVehicleRate`.
 */
const ComparisonOperationalCharts = ({ history }) => {
  if (!history || history.length === 0) {
    return (
      <div style={styles.row}>
        <div style={styles.panel}>
          <div style={styles.title}>Completed Flows Over Time</div>
          <div style={styles.subtitle}>Waiting for data...</div>
        </div>
        <div style={styles.panel}>
          <div style={styles.title}>Stopped Vehicle %</div>
          <div style={styles.subtitle}>Waiting for data...</div>
        </div>
      </div>
    );
  }

  const completedData = history.map(h => ({
    timestamp: h.timestamp,
    FIXED: h.FIXED_TIME?.completedFlows ?? 0,
    ADAPTIVE: h.TRAFFIC_ADAPTIVE?.completedFlows ?? 0,
    GREEN_WAVE: h.GREEN_WAVE?.completedFlows ?? 0,
  }));

  const stoppedData = history.map(h => ({
    timestamp: h.timestamp,
    FIXED: ((h.FIXED_TIME?.stoppedVehicleRate ?? 0) * 100),
    ADAPTIVE: ((h.TRAFFIC_ADAPTIVE?.stoppedVehicleRate ?? 0) * 100),
    GREEN_WAVE: ((h.GREEN_WAVE?.stoppedVehicleRate ?? 0) * 100),
  }));

  const formatTime = (v) => {
    const s = Math.floor(v);
    const mm = Math.floor(s / 60);
    const ss = s % 60;
    return `${mm}:${ss.toString().padStart(2, '0')}`;
  };

  const commonAxisProps = {
    tick: { fill: '#86868B', fontSize: 11 },
  };

  const renderChart = (data, yLabel, yFormatter) => (
    <ResponsiveContainer width="100%" height={220}>
      <LineChart data={data} margin={{ top: 6, right: 16, left: 0, bottom: 6 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(0,0,0,0.06)" />
        <XAxis
          dataKey="timestamp"
          type="number"
          domain={['dataMin', 'dataMax']}
          tickFormatter={formatTime}
          {...commonAxisProps}
        />
        <YAxis
          domain={[0, 'auto']}
          tickFormatter={yFormatter}
          {...commonAxisProps}
          label={{ value: yLabel, angle: -90, position: 'insideLeft', fill: '#86868B', fontSize: 10 }}
        />
        <Tooltip
          labelFormatter={(v) => `Sim ${formatTime(v)}`}
          formatter={(v, name) => [yFormatter(v), name]}
          contentStyle={{
            background: 'rgba(255,255,255,0.96)',
            border: '1px solid rgba(0,0,0,0.08)',
            borderRadius: 10,
            fontSize: 12,
          }}
        />
        <Legend wrapperStyle={{ fontSize: 11, paddingTop: 4 }} />
        <Line type="stepAfter" dataKey="FIXED" name="Fixed Timing" stroke="#FF453A" strokeWidth={2} dot={false} isAnimationActive={false} />
        <Line type="stepAfter" dataKey="ADAPTIVE" name="Adaptive (Webster)" stroke="#FF9F0A" strokeWidth={2} dot={false} isAnimationActive={false} />
        <Line type="stepAfter" dataKey="GREEN_WAVE" name="Green Wave" stroke="#30D158" strokeWidth={2.5} dot={false} isAnimationActive={false} />
      </LineChart>
    </ResponsiveContainer>
  );

  return (
    <div style={styles.row}>
      <div style={styles.panel}>
        <div style={styles.title}>Completed Flows Over Time</div>
        <div style={styles.subtitle}>Higher = more traffic cleared. Green Wave should lead.</div>
        {renderChart(completedData, 'Flows', (v) => String(Math.round(v)))}
      </div>
      <div style={styles.panel}>
        <div style={styles.title}>Stopped Vehicle %</div>
        <div style={styles.subtitle}>Lower = fewer cars waiting at reds. Green Wave designed for 0%.</div>
        {renderChart(stoppedData, '%', (v) => `${Number(v).toFixed(0)}%`)}
      </div>
    </div>
  );
};

const styles = {
  row: {
    display: 'flex',
    gap: 12,
    marginBottom: 16,
  },
  panel: {
    flex: 1,
    background: 'rgba(255,255,255,0.95)',
    backdropFilter: 'saturate(180%) blur(16px)',
    borderRadius: 14,
    padding: '14px 16px',
    boxShadow: '0 2px 12px rgba(0,0,0,0.06)',
  },
  title: {
    fontSize: 14,
    fontWeight: 700,
    color: '#1D1D1F',
    letterSpacing: 0.3,
  },
  subtitle: {
    fontSize: 11,
    color: '#86868B',
    marginTop: 2,
    marginBottom: 8,
  },
};

export default ComparisonOperationalCharts;
