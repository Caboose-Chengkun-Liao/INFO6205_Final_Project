import React, { useState, useEffect } from 'react';
import { simulationAPI } from '../services/api';
import './ControlPanel.css';

const SPEED_OPTIONS = [1, 2, 3, 5, 10];

const ControlPanel = ({ onStateChange, currentTime: propCurrentTime, simulationState: propSimulationState }) => {
  const [state, setState] = useState('STOPPED');
  const [currentTime, setCurrentTime] = useState(0);
  const [loading, setLoading] = useState(false);
  const [speed, setSpeed] = useState(1);

  // Update local state with data received from WebSocket
  useEffect(() => {
    if (propSimulationState) {
      setState(propSimulationState);
    }
  }, [propSimulationState]);

  useEffect(() => {
    if (propCurrentTime !== undefined) {
      setCurrentTime(propCurrentTime);
    }
  }, [propCurrentTime]);

  const formatTime = (seconds) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handleInitialize = async () => {
    setLoading(true);
    try {
      const response = await simulationAPI.initialize();
      alert(response.data.message);
      setState('INITIALIZED');
      if (onStateChange) onStateChange('INITIALIZED');
    } catch (error) {
      console.error('Initialization failed:', error);
      alert('Initialization failed: ' + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
    }
  };

  const handleStart = async () => {
    setLoading(true);
    try {
      await simulationAPI.start();
      setState('RUNNING');
      if (onStateChange) onStateChange('RUNNING');
    } catch (error) {
      console.error('Start failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const handlePause = async () => {
    setLoading(true);
    try {
      await simulationAPI.pause();
      setState('PAUSED');
      if (onStateChange) onStateChange('PAUSED');
    } catch (error) {
      console.error('Pause failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleStop = async () => {
    setLoading(true);
    try {
      await simulationAPI.stop();
      setState('STOPPED');
      setCurrentTime(0);
      if (onStateChange) onStateChange('STOPPED');
    } catch (error) {
      console.error('Stop failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSpeedChange = async (multiplier) => {
    try {
      await simulationAPI.setSpeed(multiplier);
      setSpeed(multiplier);
    } catch (error) {
      console.error('Speed change failed:', error);
    }
  };

  const handleReset = async () => {
    setLoading(true);
    try {
      await simulationAPI.reset();
      setState('INITIALIZED');
      setCurrentTime(0);
      if (onStateChange) onStateChange('INITIALIZED');
    } catch (error) {
      console.error('Reset failed:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="control-panel">
      <h2>Simulation Control</h2>

      <div className="status-display">
        <div className="status-item">
          <span className="label">Status</span>
          <span className={`value state-${state.toLowerCase()}`}>{state}</span>
        </div>
        <div className="status-item">
          <span className="label">Time</span>
          <span className="value">{formatTime(currentTime)}</span>
        </div>
        <div className="status-item">
          <span className="label">Speed</span>
          <span className="value speed-value">{speed}x</span>
        </div>
      </div>

      <div className="speed-control">
        <span className="speed-label">Simulation Speed</span>
        <div className="speed-buttons">
          {SPEED_OPTIONS.map((s) => (
            <button
              key={s}
              onClick={() => handleSpeedChange(s)}
              className={`btn-speed ${speed === s ? 'active' : ''}`}
              disabled={state === 'STOPPED'}
            >
              {s}x
            </button>
          ))}
        </div>
      </div>

      <div className="control-buttons">
        <button
          onClick={handleInitialize}
          disabled={loading || state !== 'STOPPED'}
          className="btn btn-primary"
        >
          Initialize
        </button>

        <button
          onClick={handleStart}
          disabled={loading || state === 'RUNNING' || state === 'STOPPED'}
          className="btn btn-success"
        >
          Start
        </button>

        <button
          onClick={handlePause}
          disabled={loading || state !== 'RUNNING'}
          className="btn btn-warning"
        >
          Pause
        </button>

        <button
          onClick={handleStop}
          disabled={loading || state === 'STOPPED'}
          className="btn btn-danger"
        >
          Stop
        </button>

        <button
          onClick={handleReset}
          disabled={loading || state === 'STOPPED'}
          className="btn btn-secondary"
        >
          Reset
        </button>
      </div>
    </div>
  );
};

export default ControlPanel;
