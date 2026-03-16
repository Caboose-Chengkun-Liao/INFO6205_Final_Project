import React from 'react';
import './MetricsDisplay.css';

const MetricsDisplay = ({ metrics }) => {
  if (!metrics) {
    return (
      <div className="metrics-display">
        <h2>性能指标</h2>
        <p>暂无数据</p>
      </div>
    );
  }

  return (
    <div className="metrics-display">
      <h2>性能指标</h2>
      <div className="metrics-grid">
        <div className="metric-card">
          <div className="metric-label">效率值</div>
          <div className="metric-value">{metrics.efficiency?.toFixed(2) || '0.00'}</div>
          <div className="metric-unit">km/h</div>
        </div>

        <div className="metric-card">
          <div className="metric-label">吞吐量</div>
          <div className="metric-value">{metrics.throughput || 0}</div>
          <div className="metric-unit">车辆</div>
        </div>

        <div className="metric-card">
          <div className="metric-label">平均速度</div>
          <div className="metric-value">{metrics.avgSpeed?.toFixed(1) || '0.0'}</div>
          <div className="metric-unit">km/h</div>
        </div>

        <div className="metric-card">
          <div className="metric-label">平均旅行时间</div>
          <div className="metric-value">{(metrics.avgTravelTime / 60)?.toFixed(1) || '0.0'}</div>
          <div className="metric-unit">分钟</div>
        </div>

        <div className="metric-card">
          <div className="metric-label">活跃流</div>
          <div className="metric-value">{metrics.activeFlowCount || 0}</div>
          <div className="metric-unit">条</div>
        </div>

        <div className="metric-card">
          <div className="metric-label">已完成流</div>
          <div className="metric-value">{metrics.completedFlowCount || 0}</div>
          <div className="metric-unit">条</div>
        </div>
      </div>
    </div>
  );
};

export default MetricsDisplay;
