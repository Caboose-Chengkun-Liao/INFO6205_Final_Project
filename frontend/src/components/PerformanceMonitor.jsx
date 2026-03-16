import React, { useState, useEffect } from 'react';
import api from '../services/api';
import websocketService from '../services/websocket';

/**
 * Real-time Performance Monitoring Dashboard
 * Displays efficiency metrics and traffic statistics
 */
const PerformanceMonitor = () => {
  const [metrics, setMetrics] = useState(null);
  const [efficiencyTrend, setEfficiencyTrend] = useState([]);
  const [isExpanded, setIsExpanded] = useState(false);

  useEffect(() => {
    // Load initial metrics
    loadMetrics();
    loadEfficiencyTrend();

    // Subscribe to WebSocket for real-time updates
    const unsubscribe = websocketService.subscribe('/topic/simulation', (data) => {
      if (data.metrics) {
        setMetrics(data.metrics);
      }
    });

    // Refresh data periodically
    const interval = setInterval(() => {
      loadMetrics();
      loadEfficiencyTrend();
    }, 5000);

    return () => {
      if (unsubscribe) unsubscribe();
      clearInterval(interval);
    };
  }, []);

  const loadMetrics = async () => {
    try {
      const response = await api.get('/simulation/metrics');
      setMetrics(response.data);
    } catch (error) {
      console.error('Failed to load metrics:', error);
    }
  };

  const loadEfficiencyTrend = async () => {
    try {
      const response = await api.get('/simulation/efficiency/trend?count=10');
      setEfficiencyTrend(response.data || []);
    } catch (error) {
      console.error('Failed to load efficiency trend:', error);
    }
  };

  // Calculate trend direction
  const getTrendDirection = () => {
    if (efficiencyTrend.length < 2) return 'neutral';
    const latest = efficiencyTrend[efficiencyTrend.length - 1]?.efficiency || 0;
    const previous = efficiencyTrend[efficiencyTrend.length - 2]?.efficiency || 0;
    if (latest > previous) return 'up';
    if (latest < previous) return 'down';
    return 'neutral';
  };

  const trend = getTrendDirection();
  const latestEfficiency = efficiencyTrend[efficiencyTrend.length - 1]?.efficiency || 0;

  return (
    <div style={styles.container}>
      {/* Compact View */}
      {!isExpanded && (
        <div
          onClick={() => setIsExpanded(true)}
          style={styles.compactView}
          title="Click to expand performance monitor"
        >
          <div style={styles.compactIcon}>📊</div>
          <div style={styles.compactMetrics}>
            <div style={styles.compactValue}>
              {latestEfficiency.toFixed(1)}
              <span style={styles.compactUnit}>km/h</span>
            </div>
            <div style={styles.compactLabel}>Efficiency</div>
          </div>
          <div style={{
            ...styles.trendIndicator,
            color: trend === 'up' ? '#10B981' : trend === 'down' ? '#EF4444' : '#6B7280'
          }}>
            {trend === 'up' ? '↗' : trend === 'down' ? '↘' : '→'}
          </div>
        </div>
      )}

      {/* Expanded View */}
      {isExpanded && (
        <div style={styles.expandedView}>
          <div style={styles.header}>
            <h3 style={styles.title}>Performance Monitor</h3>
            <button
              onClick={() => setIsExpanded(false)}
              style={styles.closeButton}
            >
              −
            </button>
          </div>

          {metrics && (
            <div style={styles.metricsGrid}>
              {/* Efficiency */}
              <div style={styles.metricCard}>
                <div style={styles.metricIcon}>⚡</div>
                <div style={styles.metricContent}>
                  <div style={styles.metricLabel}>Efficiency</div>
                  <div style={styles.metricValue}>
                    {metrics.averageEfficiency?.toFixed(2) || '0.00'}
                    <span style={styles.metricUnit}>km/h</span>
                  </div>
                </div>
              </div>

              {/* Average Speed */}
              <div style={styles.metricCard}>
                <div style={styles.metricIcon}>🏃</div>
                <div style={styles.metricContent}>
                  <div style={styles.metricLabel}>Avg Speed</div>
                  <div style={styles.metricValue}>
                    {metrics.averageSpeed?.toFixed(1) || '0.0'}
                    <span style={styles.metricUnit}>km/h</span>
                  </div>
                </div>
              </div>

              {/* Total Distance */}
              <div style={styles.metricCard}>
                <div style={styles.metricIcon}>🛣️</div>
                <div style={styles.metricContent}>
                  <div style={styles.metricLabel}>Total Distance</div>
                  <div style={styles.metricValue}>
                    {metrics.totalDistance?.toFixed(1) || '0.0'}
                    <span style={styles.metricUnit}>km</span>
                  </div>
                </div>
              </div>

              {/* Travel Time */}
              <div style={styles.metricCard}>
                <div style={styles.metricIcon}>⏱️</div>
                <div style={styles.metricContent}>
                  <div style={styles.metricLabel}>Avg Travel Time</div>
                  <div style={styles.metricValue}>
                    {(metrics.averageTravelTime / 60)?.toFixed(1) || '0.0'}
                    <span style={styles.metricUnit}>min</span>
                  </div>
                </div>
              </div>

              {/* Active Vehicles */}
              <div style={styles.metricCard}>
                <div style={styles.metricIcon}>🚗</div>
                <div style={styles.metricContent}>
                  <div style={styles.metricLabel}>Active Vehicles</div>
                  <div style={styles.metricValue}>
                    {metrics.totalVehicles || 0}
                  </div>
                </div>
              </div>

              {/* Completed */}
              <div style={styles.metricCard}>
                <div style={styles.metricIcon}>✅</div>
                <div style={styles.metricContent}>
                  <div style={styles.metricLabel}>Completed</div>
                  <div style={styles.metricValue}>
                    {metrics.completedFlows || 0}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Efficiency Trend Chart */}
          {efficiencyTrend.length > 0 && (
            <div style={styles.chartSection}>
              <h4 style={styles.chartTitle}>Efficiency Trend</h4>
              <div style={styles.chart}>
                {efficiencyTrend.map((record, index) => {
                  const maxEfficiency = Math.max(...efficiencyTrend.map(r => r.efficiency), 50);
                  const height = (record.efficiency / maxEfficiency) * 100;

                  return (
                    <div key={index} style={styles.chartBarContainer}>
                      <div
                        style={{
                          ...styles.chartBar,
                          height: `${height}%`,
                          background: height > 70 ? '#10B981' : height > 40 ? '#F59E0B' : '#EF4444'
                        }}
                        title={`${record.efficiency.toFixed(1)} km/h`}
                      />
                    </div>
                  );
                })}
              </div>
              <div style={styles.chartLegend}>
                <span>← Past</span>
                <span>Recent →</span>
              </div>
            </div>
          )}

          {/* Status Indicator */}
          <div style={styles.statusBar}>
            <div style={styles.statusDot}></div>
            <span style={styles.statusText}>Live Monitoring Active</span>
          </div>
        </div>
      )}

      {/* CSS Animation */}
      <style>{`
        @keyframes pulse-dot {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.5; }
        }
      `}</style>
    </div>
  );
};

const styles = {
  container: {
    position: 'fixed',
    top: '120px',
    right: '32px',
    zIndex: 999
  },
  compactView: {
    background: 'white',
    borderRadius: '12px',
    padding: '16px 20px',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
    cursor: 'pointer',
    transition: 'all 0.3s',
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
    minWidth: '200px'
  },
  compactIcon: {
    fontSize: '28px'
  },
  compactMetrics: {
    flex: 1
  },
  compactValue: {
    fontSize: '24px',
    fontWeight: '700',
    color: '#1F2937',
    lineHeight: '1'
  },
  compactUnit: {
    fontSize: '14px',
    fontWeight: '500',
    color: '#6B7280',
    marginLeft: '4px'
  },
  compactLabel: {
    fontSize: '12px',
    color: '#9CA3AF',
    marginTop: '4px',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  trendIndicator: {
    fontSize: '24px',
    fontWeight: 'bold'
  },
  expandedView: {
    background: 'white',
    borderRadius: '16px',
    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
    width: '420px',
    maxHeight: '600px',
    overflow: 'auto'
  },
  header: {
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    color: 'white',
    padding: '20px 24px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    position: 'sticky',
    top: 0,
    zIndex: 1
  },
  title: {
    margin: 0,
    fontSize: '18px',
    fontWeight: '600'
  },
  closeButton: {
    background: 'rgba(255, 255, 255, 0.2)',
    border: 'none',
    color: 'white',
    width: '32px',
    height: '32px',
    borderRadius: '8px',
    fontSize: '24px',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  },
  metricsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(2, 1fr)',
    gap: '12px',
    padding: '20px'
  },
  metricCard: {
    background: 'linear-gradient(135deg, #F9FAFB 0%, #F3F4F6 100%)',
    borderRadius: '12px',
    padding: '16px',
    display: 'flex',
    gap: '12px',
    alignItems: 'center',
    border: '1px solid #E5E7EB'
  },
  metricIcon: {
    fontSize: '28px'
  },
  metricContent: {
    flex: 1
  },
  metricLabel: {
    fontSize: '11px',
    color: '#6B7280',
    textTransform: 'uppercase',
    letterSpacing: '0.5px',
    marginBottom: '4px'
  },
  metricValue: {
    fontSize: '20px',
    fontWeight: '700',
    color: '#1F2937',
    lineHeight: '1'
  },
  metricUnit: {
    fontSize: '12px',
    fontWeight: '500',
    color: '#9CA3AF',
    marginLeft: '4px'
  },
  chartSection: {
    padding: '20px',
    borderTop: '1px solid #E5E7EB'
  },
  chartTitle: {
    margin: '0 0 16px 0',
    fontSize: '14px',
    fontWeight: '600',
    color: '#374151'
  },
  chart: {
    height: '120px',
    display: 'flex',
    alignItems: 'flex-end',
    gap: '4px',
    padding: '10px',
    background: '#F9FAFB',
    borderRadius: '8px'
  },
  chartBarContainer: {
    flex: 1,
    height: '100%',
    display: 'flex',
    alignItems: 'flex-end'
  },
  chartBar: {
    width: '100%',
    borderRadius: '3px 3px 0 0',
    transition: 'height 0.3s ease',
    cursor: 'pointer'
  },
  chartLegend: {
    display: 'flex',
    justifyContent: 'space-between',
    marginTop: '8px',
    fontSize: '11px',
    color: '#9CA3AF'
  },
  statusBar: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '16px 20px',
    background: '#F0FDF4',
    borderTop: '1px solid #BBF7D0'
  },
  statusDot: {
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    background: '#10B981',
    animation: 'pulse-dot 2s ease-in-out infinite'
  },
  statusText: {
    fontSize: '13px',
    color: '#065F46',
    fontWeight: '500'
  }
};

export default PerformanceMonitor;
