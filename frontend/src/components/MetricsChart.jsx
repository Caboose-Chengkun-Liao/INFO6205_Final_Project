import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import './MetricsChart.css';

const MetricsChart = ({ data }) => {
  return (
    <div className="metrics-chart">
      <h2>Efficiency Trend</h2>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="timestamp"
            tickFormatter={(value) => {
              const date = new Date(value);
              return `${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
            }}
          />
          <YAxis />
          <Tooltip
            labelFormatter={(value) => new Date(value).toLocaleTimeString()}
          />
          <Legend />
          <Line
            type="monotone"
            dataKey="efficiency"
            stroke="#8884d8"
            name="Efficiency"
            strokeWidth={2}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default MetricsChart;
