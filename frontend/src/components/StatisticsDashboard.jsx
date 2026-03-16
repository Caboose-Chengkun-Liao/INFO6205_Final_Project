import React, { useState, useEffect } from 'react';
import api from '../services/api';
import websocketService from '../services/websocket';

/**
 * Real-time Statistics Dashboard
 * Displays key performance indicators and system metrics
 */
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

  useEffect(() => {
    // Load initial stats
    loadStatistics();

    // Subscribe to real-time updates
    const unsubscribe = websocketService.subscribe('/topic/simulation', (data) => {
      if (data.metrics) {
        updateStats(data.metrics);
      }
    });

    // Refresh stats periodically
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

    setStats({
      totalVehicles: metrics.totalVehicles || 0,
      activeFlows: metrics.activeFlows || 0,
      completedJourneys: metrics.completedFlows || 0,
      avgEfficiency: metrics.averageEfficiency || 0,
      avgSpeed: metrics.averageSpeed || 0,
      totalDistance: metrics.totalDistance || 0,
      avgTravelTime: metrics.averageTravelTime || 0,
      networkCongestion: metrics.congestionLevel || 0
    });
  };

  // Calculate status color based on value
  const getEfficiencyColor = (value) => {
    if (value >= 40) return '#10B981';
    if (value >= 25) return '#F59E0B';
    return '#EF4444';
  };

  const getCongestionColor = (value) => {
    if (value < 40) return '#10B981';
    if (value < 70) return '#F59E0B';
    return '#EF4444';
  };

  const getSpeedColor = (value) => {
    if (value >= 50) return '#10B981';
    if (value >= 30) return '#F59E0B';
    return '#EF4444';
  };

  const kpis = [
    {
      id: 'efficiency',
      label: 'Network Efficiency',
      value: stats.avgEfficiency.toFixed(1),
      unit: 'km/h',
      icon: '⚡',
      color: getEfficiencyColor(stats.avgEfficiency),
      change: '+5.2%',
      trend: 'up'
    },
    {
      id: 'speed',
      label: 'Average Speed',
      value: stats.avgSpeed.toFixed(1),
      unit: 'km/h',
      icon: '🏃',
      color: getSpeedColor(stats.avgSpeed),
      change: '+2.1%',
      trend: 'up'
    },
    {
      id: 'vehicles',
      label: 'Active Vehicles',
      value: stats.totalVehicles,
      unit: '',
      icon: '🚗',
      color: '#667eea',
      change: `${stats.activeFlows} flows`,
      trend: 'neutral'
    },
    {
      id: 'completed',
      label: 'Completed Journeys',
      value: stats.completedJourneys,
      unit: '',
      icon: '✅',
      color: '#10B981',
      change: '+12',
      trend: 'up'
    },
    {
      id: 'distance',
      label: 'Total Distance',
      value: stats.totalDistance.toFixed(1),
      unit: 'km',
      icon: '🛣️',
      color: '#3B82F6',
      change: '+24.5 km',
      trend: 'up'
    },
    {
      id: 'travelTime',
      label: 'Avg Travel Time',
      value: (stats.avgTravelTime / 60).toFixed(1),
      unit: 'min',
      icon: '⏱️',
      color: '#F59E0B',
      change: '-1.2 min',
      trend: 'down'
    },
    {
      id: 'congestion',
      label: 'Network Congestion',
      value: stats.networkCongestion.toFixed(0),
      unit: '%',
      icon: '🚦',
      color: getCongestionColor(stats.networkCongestion),
      change: '-3.5%',
      trend: 'down'
    },
    {
      id: 'throughput',
      label: 'System Throughput',
      value: (stats.totalVehicles * stats.avgSpeed / 60).toFixed(0),
      unit: 'v/min',
      icon: '📊',
      color: '#8B5CF6',
      change: '+8.3%',
      trend: 'up'
    }
  ];

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div style={styles.headerLeft}>
          <h2 style={styles.title}>Real-time System Statistics</h2>
          <p style={styles.subtitle}>Key Performance Indicators</p>
        </div>
        <div style={styles.headerRight}>
          <div style={styles.liveIndicator}>
            <div style={styles.liveDot}></div>
            <span style={styles.liveText}>LIVE</span>
          </div>
        </div>
      </div>

      <div style={styles.kpiGrid}>
        {kpis.map((kpi) => (
          <div key={kpi.id} style={styles.kpiCard}>
            <div style={styles.kpiHeader}>
              <div style={styles.kpiIcon}>{kpi.icon}</div>
              <div style={styles.kpiLabel}>{kpi.label}</div>
            </div>
            <div style={styles.kpiBody}>
              <div style={{
                ...styles.kpiValue,
                color: kpi.color
              }}>
                {kpi.value}
                {kpi.unit && <span style={styles.kpiUnit}>{kpi.unit}</span>}
              </div>
              <div style={styles.kpiChange}>
                <span style={{
                  ...styles.trendIcon,
                  color: kpi.trend === 'up' ? '#10B981' : kpi.trend === 'down' ? '#EF4444' : '#6B7280'
                }}>
                  {kpi.trend === 'up' ? '↗' : kpi.trend === 'down' ? '↘' : '→'}
                </span>
                <span style={styles.changeText}>{kpi.change}</span>
              </div>
            </div>
            <div style={{
              ...styles.kpiProgress,
              background: `linear-gradient(90deg, ${kpi.color} 0%, ${kpi.color}40 100%)`
            }}></div>
          </div>
        ))}
      </div>

      {/* Summary Cards */}
      <div style={styles.summarySection}>
        <div style={styles.summaryCard}>
          <div style={styles.summaryHeader}>
            <span style={styles.summaryIcon}>📈</span>
            <span style={styles.summaryTitle}>Performance Summary</span>
          </div>
          <div style={styles.summaryContent}>
            <div style={styles.summaryItem}>
              <span style={styles.summaryLabel}>Network Health:</span>
              <span style={{
                ...styles.summaryValue,
                color: stats.avgEfficiency > 35 ? '#10B981' : '#F59E0B'
              }}>
                {stats.avgEfficiency > 35 ? 'Excellent' : stats.avgEfficiency > 25 ? 'Good' : 'Fair'}
              </span>
            </div>
            <div style={styles.summaryItem}>
              <span style={styles.summaryLabel}>Traffic Flow:</span>
              <span style={{
                ...styles.summaryValue,
                color: stats.networkCongestion < 50 ? '#10B981' : '#EF4444'
              }}>
                {stats.networkCongestion < 50 ? 'Smooth' : 'Congested'}
              </span>
            </div>
            <div style={styles.summaryItem}>
              <span style={styles.summaryLabel}>System Status:</span>
              <span style={{
                ...styles.summaryValue,
                color: '#10B981'
              }}>
                Operational
              </span>
            </div>
          </div>
        </div>

        <div style={styles.summaryCard}>
          <div style={styles.summaryHeader}>
            <span style={styles.summaryIcon}>🎯</span>
            <span style={styles.summaryTitle}>Optimization Goals</span>
          </div>
          <div style={styles.summaryContent}>
            <div style={styles.goalItem}>
              <div style={styles.goalLabel}>Efficiency Target</div>
              <div style={styles.progressBar}>
                <div style={{
                  ...styles.progressFill,
                  width: `${(stats.avgEfficiency / 50) * 100}%`,
                  background: getEfficiencyColor(stats.avgEfficiency)
                }}></div>
              </div>
              <div style={styles.goalValue}>{stats.avgEfficiency.toFixed(1)} / 50 km/h</div>
            </div>
            <div style={styles.goalItem}>
              <div style={styles.goalLabel}>Congestion Reduction</div>
              <div style={styles.progressBar}>
                <div style={{
                  ...styles.progressFill,
                  width: `${Math.max(0, 100 - stats.networkCongestion)}%`,
                  background: getCongestionColor(stats.networkCongestion)
                }}></div>
              </div>
              <div style={styles.goalValue}>{(100 - stats.networkCongestion).toFixed(0)}% Clear</div>
            </div>
          </div>
        </div>
      </div>

      {/* CSS Animations */}
      <style>{`
        @keyframes pulse-live {
          0%, 100% { opacity: 1; transform: scale(1); }
          50% { opacity: 0.7; transform: scale(0.95); }
        }
      `}</style>
    </div>
  );
};

const styles = {
  container: {
    padding: '24px',
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    borderRadius: '16px',
    marginBottom: '24px'
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '24px'
  },
  headerLeft: {
    flex: 1
  },
  title: {
    margin: 0,
    fontSize: '24px',
    fontWeight: '700',
    color: 'white',
    marginBottom: '4px'
  },
  subtitle: {
    margin: 0,
    fontSize: '14px',
    color: 'rgba(255, 255, 255, 0.8)'
  },
  headerRight: {
    display: 'flex',
    alignItems: 'center'
  },
  liveIndicator: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '8px 16px',
    background: 'rgba(255, 255, 255, 0.2)',
    borderRadius: '20px',
    backdropFilter: 'blur(10px)'
  },
  liveDot: {
    width: '10px',
    height: '10px',
    borderRadius: '50%',
    background: '#10B981',
    animation: 'pulse-live 2s ease-in-out infinite'
  },
  liveText: {
    fontSize: '12px',
    fontWeight: '700',
    color: 'white',
    letterSpacing: '1px'
  },
  kpiGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
    gap: '16px',
    marginBottom: '24px'
  },
  kpiCard: {
    position: 'relative',
    background: 'white',
    borderRadius: '12px',
    padding: '20px',
    overflow: 'hidden',
    transition: 'transform 0.2s',
    cursor: 'pointer'
  },
  kpiHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    marginBottom: '16px'
  },
  kpiIcon: {
    fontSize: '28px'
  },
  kpiLabel: {
    fontSize: '13px',
    fontWeight: '600',
    color: '#6B7280',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  kpiBody: {
    marginBottom: '12px'
  },
  kpiValue: {
    fontSize: '32px',
    fontWeight: '800',
    lineHeight: '1',
    marginBottom: '8px'
  },
  kpiUnit: {
    fontSize: '16px',
    fontWeight: '600',
    marginLeft: '4px',
    opacity: 0.7
  },
  kpiChange: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px'
  },
  trendIcon: {
    fontSize: '16px',
    fontWeight: '700'
  },
  changeText: {
    fontSize: '13px',
    color: '#6B7280',
    fontWeight: '500'
  },
  kpiProgress: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: '4px'
  },
  summarySection: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
    gap: '16px'
  },
  summaryCard: {
    background: 'white',
    borderRadius: '12px',
    padding: '20px'
  },
  summaryHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    marginBottom: '16px',
    paddingBottom: '12px',
    borderBottom: '2px solid #F3F4F6'
  },
  summaryIcon: {
    fontSize: '24px'
  },
  summaryTitle: {
    fontSize: '16px',
    fontWeight: '700',
    color: '#1F2937'
  },
  summaryContent: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px'
  },
  summaryItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  summaryLabel: {
    fontSize: '14px',
    color: '#6B7280',
    fontWeight: '500'
  },
  summaryValue: {
    fontSize: '14px',
    fontWeight: '700'
  },
  goalItem: {
    marginBottom: '12px'
  },
  goalLabel: {
    fontSize: '13px',
    color: '#6B7280',
    fontWeight: '600',
    marginBottom: '8px'
  },
  progressBar: {
    height: '8px',
    background: '#F3F4F6',
    borderRadius: '4px',
    overflow: 'hidden',
    marginBottom: '6px'
  },
  progressFill: {
    height: '100%',
    transition: 'width 0.5s ease',
    borderRadius: '4px'
  },
  goalValue: {
    fontSize: '12px',
    color: '#9CA3AF',
    textAlign: 'right'
  }
};

export default StatisticsDashboard;
