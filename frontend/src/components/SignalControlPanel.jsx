import React, { useState, useEffect } from 'react';
import api from '../services/api';
import websocketService from '../services/websocket';

/**
 * Signal Light Control Panel
 * Switch traffic signal optimization modes and view signal states
 */
const SignalControlPanel = () => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [currentMode, setCurrentMode] = useState('FIXED');
  const [signals, setSignals] = useState([]);
  const [isChanging, setIsChanging] = useState(false);

  // Signal optimization modes
  const modes = [
    {
      id: 'FIXED',
      name: 'Fixed Timing',
      icon: '🕐',
      description: 'Traditional fixed cycle signals',
      color: '#6B7280'
    },
    {
      id: 'ADAPTIVE',
      name: 'Adaptive',
      icon: '⚡',
      description: 'Adjusts based on real-time traffic',
      color: '#F59E0B'
    },
    {
      id: 'INTELLIGENT',
      name: 'AI-Optimized',
      icon: '🧠',
      description: 'Machine learning optimization',
      color: '#10B981'
    }
  ];

  useEffect(() => {
    // Load initial signal states
    loadSignals();

    // Subscribe to WebSocket for real-time signal updates
    const unsubscribe = websocketService.subscribe('/topic/signals', (data) => {
      if (data.signals) {
        setSignals(data.signals);
      }
      if (data.mode) {
        setCurrentMode(data.mode);
      }
    });

    // Refresh signals periodically
    const interval = setInterval(loadSignals, 3000);

    return () => {
      if (unsubscribe) unsubscribe();
      clearInterval(interval);
    };
  }, []);

  const loadSignals = async () => {
    try {
      const response = await api.get('/simulation/signals');
      setSignals(response.data || []);
    } catch (error) {
      console.error('Failed to load signals:', error);
    }
  };

  const handleModeChange = async (mode) => {
    if (isChanging || mode === currentMode) return;

    setIsChanging(true);
    try {
      await api.post(`/simulation/signals/mode?mode=${mode}`);
      setCurrentMode(mode);
    } catch (error) {
      console.error('Failed to change mode:', error);
    } finally {
      setIsChanging(false);
    }
  };

  // Get signal state color
  const getSignalColor = (state) => {
    switch (state) {
      case 'GREEN':
        return '#10B981';
      case 'YELLOW':
        return '#F59E0B';
      case 'RED':
        return '#EF4444';
      default:
        return '#6B7280';
    }
  };

  return (
    <div style={styles.container}>
      {/* Toggle Button */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        style={{
          ...styles.toggleButton,
          background: modes.find(m => m.id === currentMode)?.color || '#6B7280'
        }}
        title="Signal Control"
      >
        <span style={styles.toggleIcon}>🚦</span>
        {isExpanded ? '−' : '+'}
      </button>

      {/* Expanded Panel */}
      {isExpanded && (
        <div style={styles.panel}>
          <div style={styles.header}>
            <h3 style={styles.title}>Signal Light Control</h3>
            <button
              onClick={() => setIsExpanded(false)}
              style={styles.closeButton}
            >
              ×
            </button>
          </div>

          {/* Mode Selection */}
          <div style={styles.section}>
            <h4 style={styles.sectionTitle}>Optimization Mode</h4>
            <div style={styles.modeGrid}>
              {modes.map(mode => (
                <button
                  key={mode.id}
                  onClick={() => handleModeChange(mode.id)}
                  disabled={isChanging}
                  style={{
                    ...styles.modeCard,
                    border: currentMode === mode.id
                      ? `3px solid ${mode.color}`
                      : '2px solid #E5E7EB',
                    opacity: isChanging ? 0.6 : 1
                  }}
                >
                  <div style={styles.modeIcon}>{mode.icon}</div>
                  <div style={styles.modeName}>{mode.name}</div>
                  <div style={styles.modeDescription}>{mode.description}</div>
                  {currentMode === mode.id && (
                    <div style={{
                      ...styles.activeIndicator,
                      background: mode.color
                    }}>
                      Active
                    </div>
                  )}
                </button>
              ))}
            </div>
          </div>

          {/* Signal States */}
          <div style={styles.section}>
            <h4 style={styles.sectionTitle}>Intersection Signals</h4>
            <div style={styles.signalList}>
              {signals.length > 0 ? (
                signals.map((signal, index) => (
                  <div key={signal.nodeId || index} style={styles.signalItem}>
                    <div style={styles.signalNodeInfo}>
                      <div style={styles.signalNodeId}>{signal.nodeId}</div>
                      <div style={styles.signalNodeName}>{signal.nodeName}</div>
                    </div>
                    <div style={styles.signalStates}>
                      <div style={styles.signalDirection}>
                        <span style={styles.directionLabel}>N-S:</span>
                        <div
                          style={{
                            ...styles.signalLight,
                            background: getSignalColor(signal.nsState)
                          }}
                          title={signal.nsState}
                        />
                        <span style={styles.timerText}>
                          {signal.nsTimer}s
                        </span>
                      </div>
                      <div style={styles.signalDirection}>
                        <span style={styles.directionLabel}>E-W:</span>
                        <div
                          style={{
                            ...styles.signalLight,
                            background: getSignalColor(signal.ewState)
                          }}
                          title={signal.ewState}
                        />
                        <span style={styles.timerText}>
                          {signal.ewTimer}s
                        </span>
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div style={styles.emptyState}>
                  <div style={styles.emptyIcon}>🚦</div>
                  <div style={styles.emptyText}>No signal data available</div>
                </div>
              )}
            </div>
          </div>

          {/* Legend */}
          <div style={styles.legend}>
            <div style={styles.legendItem}>
              <div style={{ ...styles.legendDot, background: '#10B981' }}></div>
              <span>Green</span>
            </div>
            <div style={styles.legendItem}>
              <div style={{ ...styles.legendDot, background: '#F59E0B' }}></div>
              <span>Yellow</span>
            </div>
            <div style={styles.legendItem}>
              <div style={{ ...styles.legendDot, background: '#EF4444' }}></div>
              <span>Red</span>
            </div>
          </div>
        </div>
      )}

      {/* CSS Animation */}
      <style>{`
        @keyframes pulse-signal {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.6; }
        }
      `}</style>
    </div>
  );
};

const styles = {
  container: {
    position: 'fixed',
    bottom: '32px',
    right: '32px',
    zIndex: 1000
  },
  toggleButton: {
    width: '60px',
    height: '60px',
    borderRadius: '50%',
    border: 'none',
    color: 'white',
    fontSize: '24px',
    fontWeight: 'bold',
    cursor: 'pointer',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.2)',
    transition: 'all 0.3s',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative'
  },
  toggleIcon: {
    position: 'absolute',
    top: '8px',
    left: '8px',
    fontSize: '16px'
  },
  panel: {
    position: 'absolute',
    bottom: '70px',
    right: '0',
    width: '420px',
    maxHeight: '600px',
    background: 'white',
    borderRadius: '16px',
    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
    overflow: 'hidden',
    display: 'flex',
    flexDirection: 'column'
  },
  header: {
    background: 'linear-gradient(135deg, #059669 0%, #047857 100%)',
    color: 'white',
    padding: '20px 24px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center'
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
  section: {
    padding: '20px 24px',
    borderBottom: '1px solid #E5E7EB'
  },
  sectionTitle: {
    margin: '0 0 16px 0',
    fontSize: '14px',
    fontWeight: '600',
    color: '#374151',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  modeGrid: {
    display: 'grid',
    gridTemplateColumns: '1fr',
    gap: '12px'
  },
  modeCard: {
    position: 'relative',
    padding: '16px',
    borderRadius: '12px',
    background: 'white',
    cursor: 'pointer',
    transition: 'all 0.2s',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    textAlign: 'center'
  },
  modeIcon: {
    fontSize: '32px',
    marginBottom: '8px'
  },
  modeName: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#1F2937',
    marginBottom: '4px'
  },
  modeDescription: {
    fontSize: '12px',
    color: '#6B7280',
    lineHeight: '1.4'
  },
  activeIndicator: {
    position: 'absolute',
    top: '8px',
    right: '8px',
    padding: '4px 8px',
    borderRadius: '12px',
    fontSize: '10px',
    color: 'white',
    fontWeight: '600',
    textTransform: 'uppercase'
  },
  signalList: {
    maxHeight: '200px',
    overflowY: 'auto',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px'
  },
  signalItem: {
    padding: '12px',
    background: '#F9FAFB',
    borderRadius: '8px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  signalNodeInfo: {
    flex: '0 0 auto'
  },
  signalNodeId: {
    fontSize: '14px',
    fontWeight: '700',
    color: '#1F2937'
  },
  signalNodeName: {
    fontSize: '11px',
    color: '#6B7280'
  },
  signalStates: {
    display: 'flex',
    gap: '16px'
  },
  signalDirection: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px'
  },
  directionLabel: {
    fontSize: '11px',
    color: '#6B7280',
    fontWeight: '500'
  },
  signalLight: {
    width: '16px',
    height: '16px',
    borderRadius: '50%',
    boxShadow: '0 0 8px rgba(0, 0, 0, 0.2)',
    animation: 'pulse-signal 2s ease-in-out infinite'
  },
  timerText: {
    fontSize: '12px',
    color: '#374151',
    fontWeight: '600',
    minWidth: '28px'
  },
  emptyState: {
    padding: '32px',
    textAlign: 'center'
  },
  emptyIcon: {
    fontSize: '48px',
    marginBottom: '12px',
    opacity: 0.3
  },
  emptyText: {
    fontSize: '14px',
    color: '#9CA3AF'
  },
  legend: {
    display: 'flex',
    justifyContent: 'center',
    gap: '20px',
    padding: '16px 24px',
    background: '#F9FAFB',
    borderTop: '1px solid #E5E7EB'
  },
  legendItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    fontSize: '12px',
    color: '#6B7280'
  },
  legendDot: {
    width: '12px',
    height: '12px',
    borderRadius: '50%'
  }
};

export default SignalControlPanel;
