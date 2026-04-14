import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import './MetricsChart.css';

const MetricsChart = ({ data }) => {
  return (
    <div className="metrics-chart">
      <h2>Efficiency Trend</h2>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(0,0,0,0.06)" />
          <XAxis
            dataKey="timestamp"
            type="number"
            domain={['dataMin', 'dataMax']}
            tickFormatter={(value) => {
              // timestamp is simulation seconds — format as m:ss
              const s = Math.floor(value);
              const mm = Math.floor(s / 60);
              const ss = s % 60;
              return `${mm}:${ss.toString().padStart(2, '0')}`;
            }}
            tick={{ fill: '#86868B', fontSize: 12 }}
          />
          <YAxis tick={{ fill: '#86868B', fontSize: 12 }} />
          <Tooltip
            labelFormatter={(value) => {
              const s = Math.floor(value);
              const mm = Math.floor(s / 60);
              const ss = s % 60;
              return `Sim time ${mm}:${ss.toString().padStart(2, '0')}`;
            }}
            contentStyle={{
              background: 'rgba(255,255,255,0.95)',
              border: '1px solid rgba(0,0,0,0.06)',
              borderRadius: '12px',
              boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
              fontSize: '13px'
            }}
          />
          <Legend wrapperStyle={{ fontSize: '13px' }} />
          <Line
            type="monotone"
            dataKey="efficiency"
            stroke="#0071E3"
            name="Efficiency"
            strokeWidth={2.5}
            dot={{ r: 3, fill: '#0071E3' }}
            activeDot={{ r: 5, fill: '#0071E3' }}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default MetricsChart;
