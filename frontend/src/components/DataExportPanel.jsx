import React, { useState } from 'react';
import api from '../services/api';

/**
 * Data Export Panel
 * Export simulation data in CSV or JSON format
 */
const DataExportPanel = () => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [exportStatus, setExportStatus] = useState(null);

  const exportOptions = [
    {
      id: 'metrics',
      name: 'Performance Metrics',
      icon: '📊',
      description: 'Export efficiency, speed, and travel time data',
      formats: ['csv', 'json']
    },
    {
      id: 'flows',
      name: 'Traffic Flows',
      icon: '🚗',
      description: 'Export all traffic flow records',
      formats: ['csv', 'json']
    },
    {
      id: 'signals',
      name: 'Signal States',
      icon: '🚦',
      description: 'Export traffic light timing and states',
      formats: ['csv', 'json']
    },
    {
      id: 'network',
      name: 'Network Graph',
      icon: '🗺️',
      description: 'Export road network topology',
      formats: ['json']
    },
    {
      id: 'efficiency_trend',
      name: 'Efficiency Trend',
      icon: '📈',
      description: 'Export historical efficiency data',
      formats: ['csv', 'json']
    }
  ];

  const handleExport = async (dataType, format) => {
    setIsExporting(true);
    setExportStatus({ type: 'info', message: `正在导出 ${dataType}...` });

    try {
      let data;
      let filename;

      // Fetch data based on type
      switch (dataType) {
        case 'metrics':
          const metricsRes = await api.get('/simulation/metrics');
          data = metricsRes.data;
          filename = `traffic_metrics_${Date.now()}`;
          break;

        case 'flows':
          const flowsRes = await api.get('/simulation/flows');
          data = flowsRes.data;
          filename = `traffic_flows_${Date.now()}`;
          break;

        case 'signals':
          const signalsRes = await api.get('/simulation/signals');
          data = signalsRes.data;
          filename = `signal_states_${Date.now()}`;
          break;

        case 'network':
          const networkRes = await api.get('/simulation/graph');
          data = networkRes.data;
          filename = `network_graph_${Date.now()}`;
          break;

        case 'efficiency_trend':
          const trendRes = await api.get('/simulation/efficiency/trend?count=100');
          data = trendRes.data;
          filename = `efficiency_trend_${Date.now()}`;
          break;

        default:
          throw new Error('Unknown data type');
      }

      // Generate file content based on format
      let content;
      let mimeType;
      let extension;

      if (format === 'csv') {
        content = convertToCSV(data);
        mimeType = 'text/csv';
        extension = 'csv';
      } else {
        content = JSON.stringify(data, null, 2);
        mimeType = 'application/json';
        extension = 'json';
      }

      // Download file
      downloadFile(content, `${filename}.${extension}`, mimeType);

      setExportStatus({
        type: 'success',
        message: `✅ 成功导出 ${filename}.${extension}`
      });

      setTimeout(() => setExportStatus(null), 3000);
    } catch (error) {
      console.error('Export failed:', error);
      setExportStatus({
        type: 'error',
        message: `❌ 导出失败: ${error.message}`
      });

      setTimeout(() => setExportStatus(null), 5000);
    } finally {
      setIsExporting(false);
    }
  };

  const convertToCSV = (data) => {
    if (!data) return '';

    // Handle array of objects
    if (Array.isArray(data)) {
      if (data.length === 0) return '';

      const headers = Object.keys(data[0]);
      const csvRows = [
        headers.join(','),
        ...data.map(row =>
          headers.map(header => {
            const value = row[header];
            // Escape quotes and wrap in quotes if contains comma
            const stringValue = String(value).replace(/"/g, '""');
            return stringValue.includes(',') ? `"${stringValue}"` : stringValue;
          }).join(',')
        )
      ];

      return csvRows.join('\n');
    }

    // Handle single object
    const entries = Object.entries(data);
    return entries.map(([key, value]) => `${key},${value}`).join('\n');
  };

  const downloadFile = (content, filename, mimeType) => {
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handleExportAll = async () => {
    setIsExporting(true);
    setExportStatus({ type: 'info', message: '正在导出全部数据...' });

    try {
      const timestamp = Date.now();
      const allData = {};

      // Fetch all data
      const [metricsRes, flowsRes, signalsRes, networkRes, trendRes] = await Promise.all([
        api.get('/simulation/metrics').catch(() => ({ data: null })),
        api.get('/simulation/flows').catch(() => ({ data: [] })),
        api.get('/simulation/signals').catch(() => ({ data: [] })),
        api.get('/simulation/graph').catch(() => ({ data: null })),
        api.get('/simulation/efficiency/trend?count=100').catch(() => ({ data: [] }))
      ]);

      allData.metrics = metricsRes.data;
      allData.flows = flowsRes.data;
      allData.signals = signalsRes.data;
      allData.network = networkRes.data;
      allData.efficiencyTrend = trendRes.data;
      allData.exportTime = new Date().toISOString();

      const content = JSON.stringify(allData, null, 2);
      downloadFile(content, `traffic_data_complete_${timestamp}.json`, 'application/json');

      setExportStatus({
        type: 'success',
        message: '✅ 成功导出全部数据'
      });

      setTimeout(() => setExportStatus(null), 3000);
    } catch (error) {
      setExportStatus({
        type: 'error',
        message: `❌ 导出失败: ${error.message}`
      });

      setTimeout(() => setExportStatus(null), 5000);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div style={styles.container}>
      {/* Toggle Button */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        style={styles.toggleButton}
        title="Export Data"
      >
        <span style={styles.toggleIcon}>💾</span>
        {isExpanded ? '−' : '+'}
      </button>

      {/* Expanded Panel */}
      {isExpanded && (
        <div style={styles.panel}>
          <div style={styles.header}>
            <h3 style={styles.title}>Data Export</h3>
            <button
              onClick={() => setIsExpanded(false)}
              style={styles.closeButton}
            >
              ×
            </button>
          </div>

          {/* Export Status */}
          {exportStatus && (
            <div style={{
              ...styles.statusMessage,
              background: exportStatus.type === 'success' ? '#D1FAE5' :
                         exportStatus.type === 'error' ? '#FEE2E2' : '#DBEAFE',
              color: exportStatus.type === 'success' ? '#065F46' :
                    exportStatus.type === 'error' ? '#991B1B' : '#1E40AF'
            }}>
              {exportStatus.message}
            </div>
          )}

          {/* Export Options */}
          <div style={styles.exportList}>
            {exportOptions.map(option => (
              <div key={option.id} style={styles.exportItem}>
                <div style={styles.exportInfo}>
                  <div style={styles.exportIcon}>{option.icon}</div>
                  <div style={styles.exportDetails}>
                    <div style={styles.exportName}>{option.name}</div>
                    <div style={styles.exportDescription}>{option.description}</div>
                  </div>
                </div>
                <div style={styles.exportActions}>
                  {option.formats.map(format => (
                    <button
                      key={format}
                      onClick={() => handleExport(option.id, format)}
                      disabled={isExporting}
                      style={{
                        ...styles.exportButton,
                        opacity: isExporting ? 0.6 : 1
                      }}
                    >
                      {format.toUpperCase()}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>

          {/* Export All Button */}
          <div style={styles.footer}>
            <button
              onClick={handleExportAll}
              disabled={isExporting}
              style={{
                ...styles.exportAllButton,
                opacity: isExporting ? 0.6 : 1
              }}
            >
              {isExporting ? '导出中...' : '📦 导出全部数据 (JSON)'}
            </button>
          </div>

          {/* Info Box */}
          <div style={styles.infoBox}>
            <div style={styles.infoIcon}>💡</div>
            <div style={styles.infoText}>
              CSV格式适合Excel分析，JSON格式保留完整数据结构
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const styles = {
  container: {
    position: 'fixed',
    bottom: '110px',
    right: '32px',
    zIndex: 1000
  },
  toggleButton: {
    width: '60px',
    height: '60px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%)',
    border: 'none',
    color: 'white',
    fontSize: '24px',
    fontWeight: 'bold',
    cursor: 'pointer',
    boxShadow: '0 4px 12px rgba(139, 92, 246, 0.4)',
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
    background: 'linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%)',
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
  statusMessage: {
    padding: '12px 20px',
    fontSize: '13px',
    fontWeight: '600',
    margin: '16px 20px 0 20px',
    borderRadius: '8px'
  },
  exportList: {
    flex: 1,
    overflowY: 'auto',
    padding: '16px 20px'
  },
  exportItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '16px',
    background: '#F9FAFB',
    borderRadius: '12px',
    marginBottom: '12px',
    border: '1px solid #E5E7EB'
  },
  exportInfo: {
    display: 'flex',
    gap: '12px',
    flex: 1,
    minWidth: 0
  },
  exportIcon: {
    fontSize: '24px',
    flexShrink: 0
  },
  exportDetails: {
    flex: 1,
    minWidth: 0
  },
  exportName: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#1F2937',
    marginBottom: '4px'
  },
  exportDescription: {
    fontSize: '12px',
    color: '#6B7280',
    lineHeight: '1.4'
  },
  exportActions: {
    display: 'flex',
    gap: '8px'
  },
  exportButton: {
    padding: '6px 12px',
    background: 'linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%)',
    border: 'none',
    borderRadius: '6px',
    color: 'white',
    fontSize: '11px',
    fontWeight: '700',
    cursor: 'pointer',
    transition: 'all 0.2s',
    whiteSpace: 'nowrap'
  },
  footer: {
    padding: '16px 20px',
    borderTop: '1px solid #E5E7EB'
  },
  exportAllButton: {
    width: '100%',
    padding: '14px 24px',
    background: 'linear-gradient(135deg, #EC4899 0%, #DB2777 100%)',
    border: 'none',
    borderRadius: '10px',
    color: 'white',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s',
    boxShadow: '0 4px 12px rgba(236, 72, 153, 0.3)'
  },
  infoBox: {
    display: 'flex',
    gap: '12px',
    padding: '16px 20px',
    background: '#FEF3C7',
    borderTop: '1px solid #FDE68A'
  },
  infoIcon: {
    fontSize: '20px',
    flexShrink: 0
  },
  infoText: {
    fontSize: '12px',
    color: '#92400E',
    lineHeight: '1.5'
  }
};

export default DataExportPanel;
