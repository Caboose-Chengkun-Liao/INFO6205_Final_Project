import React, { useState, useEffect } from 'react';
import { simulationAPI } from '../services/api';
import './ControlPanel.css';

const ControlPanel = ({ onStateChange, currentTime: propCurrentTime, simulationState: propSimulationState }) => {
  const [state, setState] = useState('STOPPED');
  const [currentTime, setCurrentTime] = useState(0);
  const [loading, setLoading] = useState(false);

  // 使用从WebSocket接收的数据更新本地状态
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
      console.error('初始化失败:', error);
      alert('初始化失败: ' + (error.response?.data?.error || error.message));
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
      console.error('启动失败:', error);
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
      console.error('暂停失败:', error);
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
      console.error('停止失败:', error);
    } finally {
      setLoading(false);
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
      console.error('重置失败:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="control-panel">
      <h2>仿真控制面板</h2>

      <div className="status-display">
        <div className="status-item">
          <span className="label">状态:</span>
          <span className={`value state-${state.toLowerCase()}`}>{state}</span>
        </div>
        <div className="status-item">
          <span className="label">仿真时间:</span>
          <span className="value">{formatTime(currentTime)}</span>
        </div>
      </div>

      <div className="control-buttons">
        <button
          onClick={handleInitialize}
          disabled={loading || state !== 'STOPPED'}
          className="btn btn-primary"
        >
          初始化
        </button>

        <button
          onClick={handleStart}
          disabled={loading || state === 'RUNNING' || state === 'STOPPED'}
          className="btn btn-success"
        >
          启动
        </button>

        <button
          onClick={handlePause}
          disabled={loading || state !== 'RUNNING'}
          className="btn btn-warning"
        >
          暂停
        </button>

        <button
          onClick={handleStop}
          disabled={loading || state === 'STOPPED'}
          className="btn btn-danger"
        >
          停止
        </button>

        <button
          onClick={handleReset}
          disabled={loading || state === 'STOPPED'}
          className="btn btn-secondary"
        >
          重置
        </button>
      </div>
    </div>
  );
};

export default ControlPanel;
