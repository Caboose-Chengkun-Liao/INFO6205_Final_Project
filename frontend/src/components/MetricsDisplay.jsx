import React from 'react';
import './MetricsDisplay.css';

const MetricsDisplay = ({ metrics }) => {
  if (!metrics) {
    return (
      <div className="metrics-display">
        <h2>Performance Metrics</h2>
        <p>No data available</p>
      </div>
    );
  }

  return (
    <div className="metrics-display">
      <h2>Performance Metrics</h2>
      <div className="metrics-grid">
        <div className="metric-card">
          <div className="metric-label">Efficiency</div>
          <div className="metric-value">{metrics.efficiency?.toFixed(2) || '0.00'}</div>
          <div className="metric-unit">km/h</div>
        </div>

        <div className="metric-card">
          <div className="metric-label">Throughput</div>
          <div className="metric-value">{metrics.throughput || 0}</div>
          <div className="metric-unit">vehicles</div>
        </div>

        <div className="metric-card">
          <div className="metric-label">Avg Speed</div>
          <div className="metric-value">{metrics.avgSpeed?.toFixed(1) || '0.0'}</div>
          <div className="metric-unit">km/h</div>
        </div>

        <div className="metric-card">
          <div className="metric-label">Avg Travel Time</div>
          <div className="metric-value">{(metrics.avgTravelTime / 60)?.toFixed(1) || '0.0'}</div>
          <div className="metric-unit">minutes</div>
        </div>

        <div className="metric-card">
          <div className="metric-label">Active Flows</div>
          <div className="metric-value">{metrics.activeFlowCount || 0}</div>
          <div className="metric-unit">flows</div>
        </div>

        <div className="metric-card">
          <div className="metric-label">Completed Flows</div>
          <div className="metric-value">{metrics.completedFlowCount || 0}</div>
          <div className="metric-unit">flows</div>
        </div>
      </div>
    </div>
  );
};

export default MetricsDisplay;
