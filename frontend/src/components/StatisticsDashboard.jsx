import React, { useState, useEffect, useRef } from 'react';
import api from '../services/api';
import websocketService from '../services/websocket';

const StatisticsDashboard = () => {
  const [stats, setStats] = useState({
    totalVehicles: 0,
    activeFlows: 0,
    completedJourneys: 0,
    avgEfficiency: 0,
    avgSpeed: 0,
    totalDistance: 0,
    avgTravelTime: 0,
    networkCongestion: 0
  });

  const prevStatsRef = useRef(null);

  useEffect(() => {
    loadStatistics();

    const unsubscribe = websocketService.subscribe('/topic/simulation', (data) => {
      if (data.metrics) {
        updateStats(data.metrics);
      }
    });

    const interval = setInterval(loadStatistics, 3000);

    return () => {
      if (unsubscribe) unsubscribe();
      clearInterval(interval);
    };
  }, []);

  const loadStatistics = async () => {
    try {
      const response = await api.get('/simulation/metrics');
      updateStats(response.data);
    } catch (error) {
      console.error('Failed to load statistics:', error);
    }
  };

  const updateStats = (metrics) => {
    if (!metrics) return;

    const newStats = {
      totalVehicles: metrics.totalVehicles || (metrics.activeFlowCount * 10) || 0,
      activeFlows: metrics.activeFlows || metrics.activeFlowCount || 0,
      completedJourneys: metrics.completedFlows || metrics.completedFlowCount || 0,
      avgEfficiency: metrics.averageEfficiency || metrics.efficiency || 0,
      avgSpeed: metrics.averageSpeed || metrics.avgSpeed || 0,
      totalDistance: metrics.totalDistance || 0,
      avgTravelTime: metrics.averageTravelTime || metrics.avgTravelTime || 0,
      networkCongestion: metrics.congestionLevel || 0
    };

    prevStatsRef.current = stats;
    setStats(newStats);
  };

  const getChange = (currentVal, key) => {
    const prev = prevStatsRef.current;
    if (!prev || !prev[key] || prev[key] === 0) return '';
    const prevVal = prev[key];
    const pct = ((currentVal - prevVal) / Math.abs(prevVal)) * 100;
    if (Math.abs(pct) < 0.1) return '';
    const sign = pct > 0 ? '+' : '';
    return `${sign}${pct.toFixed(1)}%`;
  };

  const getTrend = (currentVal, key) => {
    const prev = prevStatsRef.current;
    if (!prev || prev[key] === undefined) return 'neutral';
    if (currentVal > prev[key]) return 'up';
    if (currentVal < prev[key]) return 'down';
    return 'neutral';
  };

  const getEfficiencyColor = (value) => {
    if (value >= 40) return '#30D158';
    if (value >= 25) return '#FF9F0A';
    return '#FF453A';
  };

  const getCongestionColor = (value) => {
    if (value < 40) return '#30D158';
    if (value < 70) return '#FF9F0A';
    return '#FF453A';
  };

  const getSpeedColor = (value) => {
    if (value >= 50) return '#30D158';
    if (value >= 30) return '#FF9F0A';
    return '#FF453A';
  };

  const kpis = [
    {
      id: 'efficiency', label: 'Network Efficiency',
      value: stats.avgEfficiency.toFixed(1), unit: 'km/h',
      color: getEfficiencyColor(stats.avgEfficiency),
      change: getChange(stats.avgEfficiency, 'avgEfficiency'),
      trend: getTrend(stats.avgEfficiency, 'avgEfficiency')
    },
    {
      id: 'speed', label: 'Average Speed',
      value: stats.avgSpeed.toFixed(1), unit: 'km/h',
      color: getSpeedColor(stats.avgSpeed),
      change: getChange(stats.avgSpeed, 'avgSpeed'),
      trend: getTrend(stats.avgSpeed, 'avgSpeed')
    },
    {
      id: 'vehicles', label: 'Active Vehicles',
      value: stats.totalVehicles, unit: '',
      color: '#0071E3',
      change: `${stats.activeFlows} flows`,
      trend: 'neutral'
    },
    {
      id: 'completed', label: 'Completed Journeys',
      value: stats.completedJourneys, unit: '',
      color: '#30D158',
      change: getChange(stats.completedJourneys, 'completedJourneys'),
      trend: getTrend(stats.completedJourneys, 'completedJourneys')
    },
    {
      id: 'distance', label: 'Total Distance',
      value: stats.totalDistance.toFixed(1), unit: 'km',
      color: '#0071E3',
      change: getChange(stats.totalDistance, 'totalDistance'),
      trend: getTrend(stats.totalDistance, 'totalDistance')
    },
    {
      id: 'travelTime', label: 'Avg Travel Time',
      value: (stats.avgTravelTime / 60).toFixed(1), unit: 'min',
      color: '#FF9F0A',
      change: getChange(stats.avgTravelTime, 'avgTravelTime'),
      trend: getTrend(stats.avgTravelTime, 'avgTravelTime')
    },
    {
      id: 'congestion', label: 'Network Congestion',
      value: stats.networkCongestion.toFixed(0), unit: '%',
      color: getCongestionColor(stats.networkCongestion),
      change: getChange(stats.networkCongestion, 'networkCongestion'),
      trend: getTrend(stats.networkCongestion, 'networkCongestion')
    },
    {
      id: 'throughput', label: 'System Throughput',
      value: (stats.totalVehicles * stats.avgSpeed / 60).toFixed(0), unit: 'v/min',
      color: '#BF5AF2',
      change: getChange(stats.totalVehicles * stats.avgSpeed / 60, 'throughput'),
      trend: getTrend(stats.totalVehicles * stats.avgSpeed / 60, 'throughput')
    }
  ];

  return (
    <div style={s.container}>
      <div style={s.header}>
        <div>
          <h2 style={s.title}>System Statistics</h2>
          <p style={s.subtitle}>Real-time Key Performance Indicators</p>
        </div>
        <div style={s.liveIndicator}>
          <div style={s.liveDot}></div>
          <span style={s.liveText}>LIVE</span>
        </div>
      </div>

      <div style={s.kpiGrid}>
        {kpis.map((kpi) => (
          <div key={kpi.id} style={s.kpiCard}>
            <div style={{...s.kpiAccent, background: kpi.color}}></div>
            <div style={s.kpiHeader}>
              <div style={{...s.kpiDot, background: kpi.color}}></div>
              <div style={s.kpiLabel}>{kpi.label}</div>
            </div>
            <div style={s.kpiBody}>
              <div style={{...s.kpiValue, color: kpi.color}}>
                {kpi.value}
                {kpi.unit && <span style={s.kpiUnit}>{kpi.unit}</span>}
              </div>
              <div style={s.kpiChange}>
                <span style={{
                  ...s.trendIcon,
                  color: kpi.trend === 'up' ? '#30D158' : kpi.trend === 'down' ? '#FF453A' : '#86868B'
                }}>
                  {kpi.trend === 'up' ? '\u2197' : kpi.trend === 'down' ? '\u2198' : '\u2192'}
                </span>
                <span style={s.changeText}>{kpi.change}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Summary Cards */}
      <div style={s.summarySection}>
        <div style={s.summaryCard}>
          <h3 style={s.summaryTitle}>Performance Summary</h3>
          <div style={s.summaryContent}>
            <div style={s.summaryItem}>
              <span style={s.summaryLabel}>Network Health</span>
              <span style={{
                ...s.summaryValue,
                color: stats.avgEfficiency > 35 ? '#30D158' : '#FF9F0A'
              }}>
                {stats.avgEfficiency > 35 ? 'Excellent' : stats.avgEfficiency > 25 ? 'Good' : 'Fair'}
              </span>
            </div>
            <div style={s.summaryItem}>
              <span style={s.summaryLabel}>Traffic Flow</span>
              <span style={{
                ...s.summaryValue,
                color: stats.networkCongestion < 50 ? '#30D158' : '#FF453A'
              }}>
                {stats.networkCongestion < 50 ? 'Smooth' : 'Congested'}
              </span>
            </div>
            <div style={s.summaryItem}>
              <span style={s.summaryLabel}>System Status</span>
              <span style={{...s.summaryValue, color: '#30D158'}}>Operational</span>
            </div>
          </div>
        </div>

        <div style={s.summaryCard}>
          <h3 style={s.summaryTitle}>Optimization Goals</h3>
          <div style={s.summaryContent}>
            <div style={s.goalItem}>
              <div style={s.goalHeader}>
                <span style={s.goalLabel}>Efficiency Target</span>
                <span style={s.goalValue}>{stats.avgEfficiency.toFixed(1)} / 50 km/h</span>
              </div>
              <div style={s.progressBar}>
                <div style={{
                  ...s.progressFill,
                  width: `${Math.min(100, (stats.avgEfficiency / 50) * 100)}%`,
                  background: getEfficiencyColor(stats.avgEfficiency)
                }}></div>
              </div>
            </div>
            <div style={s.goalItem}>
              <div style={s.goalHeader}>
                <span style={s.goalLabel}>Congestion Reduction</span>
                <span style={s.goalValue}>{(100 - stats.networkCongestion).toFixed(0)}% Clear</span>
              </div>
              <div style={s.progressBar}>
                <div style={{
                  ...s.progressFill,
                  width: `${Math.max(0, 100 - stats.networkCongestion)}%`,
                  background: getCongestionColor(stats.networkCongestion)
                }}></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <style>{`
        @keyframes pulse-live {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.4; }
        }
      `}</style>
    </div>
  );
};

const s = {
  container: {
    padding: '28px 32px',
    background: '#FFFFFF',
    borderRadius: '24px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
    border: '1px solid rgba(0,0,0,0.06)',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '24px',
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
    fontWeight: '400',
  },
  liveIndicator: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    padding: '6px 12px',
    background: '#F5F5F7',
    borderRadius: '980px',
    border: '1px solid rgba(0,0,0,0.06)',
  },
  liveDot: {
    width: '6px',
    height: '6px',
    borderRadius: '50%',
    background: '#30D158',
    animation: 'pulse-live 2s ease-in-out infinite',
  },
  liveText: {
    fontSize: '11px',
    fontWeight: '600',
    color: '#86868B',
    letterSpacing: '0.6px',
    textTransform: 'uppercase',
  },
  kpiGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
    gap: '12px',
    marginBottom: '24px',
  },
  kpiCard: {
    position: 'relative',
    background: '#F5F5F7',
    borderRadius: '16px',
    padding: '20px',
    overflow: 'hidden',
    transition: 'transform 0.2s, box-shadow 0.2s',
    cursor: 'default',
    border: '1px solid rgba(0,0,0,0.04)',
  },
  kpiAccent: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: '2px',
    borderRadius: '2px 2px 0 0',
  },
  kpiHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    marginBottom: '12px',
  },
  kpiDot: {
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    flexShrink: 0,
  },
  kpiLabel: {
    fontSize: '12px',
    fontWeight: '500',
    color: '#86868B',
    textTransform: 'uppercase',
    letterSpacing: '0.4px',
  },
  kpiBody: {},
  kpiValue: {
    fontSize: '28px',
    fontWeight: '700',
    lineHeight: '1',
    marginBottom: '6px',
    letterSpacing: '-0.5px',
  },
  kpiUnit: {
    fontSize: '14px',
    fontWeight: '500',
    marginLeft: '3px',
    opacity: 0.6,
  },
  kpiChange: {
    display: 'flex',
    alignItems: 'center',
    gap: '4px',
  },
  trendIcon: {
    fontSize: '14px',
    fontWeight: '600',
  },
  changeText: {
    fontSize: '12px',
    color: '#86868B',
    fontWeight: '500',
  },
  summarySection: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
    gap: '12px',
  },
  summaryCard: {
    background: '#F5F5F7',
    borderRadius: '16px',
    padding: '20px 24px',
    border: '1px solid rgba(0,0,0,0.04)',
  },
  summaryTitle: {
    margin: '0 0 16px 0',
    fontSize: '15px',
    fontWeight: '600',
    color: '#1D1D1F',
  },
  summaryContent: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  },
  summaryItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  summaryLabel: {
    fontSize: '14px',
    color: '#86868B',
    fontWeight: '400',
  },
  summaryValue: {
    fontSize: '14px',
    fontWeight: '600',
  },
  goalItem: {
    marginBottom: '12px',
  },
  goalHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
  },
  goalLabel: {
    fontSize: '13px',
    color: '#86868B',
    fontWeight: '500',
  },
  goalValue: {
    fontSize: '12px',
    color: '#86868B',
    fontWeight: '500',
  },
  progressBar: {
    height: '6px',
    background: 'rgba(0,0,0,0.06)',
    borderRadius: '3px',
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    transition: 'width 0.5s ease',
    borderRadius: '3px',
  },
};

export default StatisticsDashboard;
