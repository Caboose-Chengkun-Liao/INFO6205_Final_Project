import React, { useState, useCallback } from 'react';
import { simulationAPI } from '../services/api';

/**
 * One-click demo mode — automatically initializes simulation,
 * creates realistic traffic flows, and cycles through signal modes.
 */
const DemoController = ({ onStateChange }) => {
  const [demoRunning, setDemoRunning] = useState(false);
  const [demoPhase, setDemoPhase] = useState('');

  const sleep = (ms) => new Promise(r => setTimeout(r, ms));

  const startDemo = useCallback(async () => {
    if (demoRunning) return;
    setDemoRunning(true);

    try {
      // Phase 1: Initialize
      setDemoPhase('Initializing...');
      await simulationAPI.initialize();
      await sleep(500);

      // Phase 2: Start simulation
      setDemoPhase('Starting simulation...');
      await simulationAPI.start();
      onStateChange?.('RUNNING');
      await sleep(1000);

      // Phase 3: Create morning rush flows
      setDemoPhase('Creating morning rush traffic...');
      const rushFlows = [
        // Heavy commuter corridors
        { entryPoint: 'W2', destination: 'E1', numberOfCars: 40 },  // Lee Hwy → Key Bridge (to DC)
        { entryPoint: 'W1', destination: 'E2', numberOfCars: 35 },  // Rt 50 → Memorial Bridge
        { entryPoint: 'N1', destination: 'S2', numberOfCars: 30 },  // North → Pentagon
        { entryPoint: 'W3', destination: 'E3', numberOfCars: 25 },  // Columbia Pike → 14th St Bridge
        { entryPoint: 'N2', destination: 'E1', numberOfCars: 20 },  // Washington Blvd → DC
        { entryPoint: 'S1', destination: 'N3', numberOfCars: 15 },  // South → North through-traffic
      ];

      for (const flow of rushFlows) {
        try {
          await simulationAPI.createFlow(flow);
          await sleep(300);
        } catch (e) {
          console.warn('Flow creation skipped:', e.message);
        }
      }

      // Phase 4: Let it run with FIXED signals
      setDemoPhase('Running FIXED signal mode...');
      await simulationAPI.setSignalMode('FIXED_TIME');
      await sleep(5000);

      // Phase 5: Add more traffic
      setDemoPhase('Adding cross-town traffic...');
      const crossTownFlows = [
        { entryPoint: 'W2', destination: 'E3', numberOfCars: 20 },
        { entryPoint: 'N3', destination: 'S1', numberOfCars: 18 },
        { entryPoint: 'E1', destination: 'W1', numberOfCars: 15 },
        { entryPoint: 'S3', destination: 'N1', numberOfCars: 12 },
      ];

      for (const flow of crossTownFlows) {
        try {
          await simulationAPI.createFlow(flow);
          await sleep(200);
        } catch (e) {
          console.warn('Flow creation skipped:', e.message);
        }
      }

      // Phase 6: Switch to ADAPTIVE
      await sleep(4000);
      setDemoPhase('Switching to ADAPTIVE signals...');
      await simulationAPI.setSignalMode('TRAFFIC_ADAPTIVE');
      await sleep(6000);

      // Phase 7: Switch to INTELLIGENT
      setDemoPhase('Switching to INTELLIGENT signals...');
      await simulationAPI.setSignalMode('LEARNING_BASED');
      await sleep(4000);

      // Phase 8: Running
      setDemoPhase('Demo running — observe traffic patterns');

    } catch (err) {
      console.error('Demo error:', err);
      setDemoPhase('Demo error — check console');
    }
  }, [demoRunning, onStateChange]);

  const stopDemo = useCallback(async () => {
    try {
      await simulationAPI.stop();
      await simulationAPI.reset();
      onStateChange?.('STOPPED');
    } catch (e) {
      // ignore
    }
    setDemoRunning(false);
    setDemoPhase('');
  }, [onStateChange]);

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <span style={styles.title}>Demo Mode</span>
        {demoPhase && <span style={styles.phase}>{demoPhase}</span>}
      </div>
      <div style={styles.buttons}>
        {!demoRunning ? (
          <button onClick={startDemo} style={styles.startBtn}>
            Start Demo
          </button>
        ) : (
          <button onClick={stopDemo} style={styles.stopBtn}>
            Stop Demo
          </button>
        )}
      </div>
    </div>
  );
};

const styles = {
  container: {
    background: 'rgba(255,255,255,0.92)',
    backdropFilter: 'saturate(180%) blur(16px)',
    borderRadius: 12,
    padding: '12px 16px',
    boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    marginBottom: 8,
  },
  title: {
    fontWeight: 700,
    fontSize: 14,
    color: '#1D1D1F',
  },
  phase: {
    fontSize: 12,
    color: '#0071E3',
    fontWeight: 500,
  },
  buttons: {
    display: 'flex',
    gap: 8,
  },
  startBtn: {
    background: 'linear-gradient(135deg, #30D158, #28a745)',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    padding: '8px 20px',
    fontSize: 13,
    fontWeight: 600,
    cursor: 'pointer',
    width: '100%',
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
    width: '100%',
  },
};

export default DemoController;
