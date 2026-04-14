import React, { useState, useEffect } from 'react';
import api from '../services/api';
import websocketService from '../services/websocket';

/**
 * Signal Control Panel - Apple-style inline segmented control for mode switching
 */
const SignalControlPanel = ({ onSignalsUpdate }) => {
  const [currentMode, setCurrentMode] = useState('FIXED');
  const [isChanging, setIsChanging] = useState(false);

  const modes = [
    { id: 'FIXED', name: 'Fixed Timing', desc: 'Traditional fixed cycle' },
    { id: 'ADAPTIVE', name: 'Adaptive', desc: 'Real-time traffic responsive' },
    { id: 'INTELLIGENT', name: 'AI-Optimized', desc: 'Machine learning control' },
  ];

  useEffect(() => {
    loadSignals();

    const unsubscribe = websocketService.subscribe('/topic/signals', (data) => {
      if (data.signals && onSignalsUpdate) {
        onSignalsUpdate(data.signals);
      }
      if (data.mode) {
        setCurrentMode(data.mode);
      }
    });

    const interval = setInterval(loadSignals, 3000);

    return () => {
      if (unsubscribe) unsubscribe();
      clearInterval(interval);
    };
  }, []);

  const loadSignals = async () => {
    try {
      const response = await api.get('/simulation/signals');
      if (onSignalsUpdate) {
        onSignalsUpdate(response.data || []);
      }
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

  return (
    <div style={s.container}>
      <div style={s.header}>
        <h2 style={s.title}>Signal Control Mode</h2>
        <p style={s.subtitle}>Select the traffic signal optimization strategy</p>
      </div>

      {/* Segmented Control */}
      <div style={s.segmentedOuter}>
        {modes.map((mode) => {
          const isActive = currentMode === mode.id;
          return (
            <button
              key={mode.id}
              onClick={() => handleModeChange(mode.id)}
              disabled={isChanging}
              style={{
                ...s.segment,
                ...(isActive ? s.segmentActive : {}),
                opacity: isChanging ? 0.6 : 1,
              }}
            >
              <span style={{
                ...s.segmentName,
                ...(isActive ? s.segmentNameActive : {}),
              }}>{mode.name}</span>
              <span style={{
                ...s.segmentDesc,
                ...(isActive ? s.segmentDescActive : {}),
              }}>{mode.desc}</span>
            </button>
          );
        })}
      </div>
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
  segmentedOuter: {
    display: 'flex',
    gap: '8px',
    background: '#F5F5F7',
    borderRadius: '16px',
    padding: '4px',
  },
  segment: {
    flex: 1,
    padding: '14px 16px',
    border: 'none',
    borderRadius: '12px',
    background: 'transparent',
    cursor: 'pointer',
    transition: 'all 0.25s ease',
    textAlign: 'center',
    fontFamily: 'inherit',
  },
  segmentActive: {
    background: '#FFFFFF',
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
  },
  segmentName: {
    display: 'block',
    fontSize: '14px',
    fontWeight: '600',
    color: '#86868B',
    marginBottom: '2px',
    transition: 'color 0.2s',
  },
  segmentNameActive: {
    color: '#0071E3',
  },
  segmentDesc: {
    display: 'block',
    fontSize: '11px',
    color: '#AEAEB2',
    fontWeight: '400',
    transition: 'color 0.2s',
  },
  segmentDescActive: {
    color: '#86868B',
  },
};

export default SignalControlPanel;
