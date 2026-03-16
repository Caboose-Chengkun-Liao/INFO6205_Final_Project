import React, { useState } from 'react';
import api from '../services/api';

/**
 * Traffic Flow Control Panel
 * Create and manage traffic flows in the network
 */
const TrafficFlowPanel = ({ nodes, onFlowCreated }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [formData, setFormData] = useState({
    entryPoint: '',
    destination: '',
    numberOfCars: 10
  });
  const [isCreating, setIsCreating] = useState(false);
  const [message, setMessage] = useState(null);

  // Get boundary nodes for entry/exit points
  const boundaryNodes = nodes.filter(n => n.type === 'BOUNDARY');

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.entryPoint || !formData.destination) {
      setMessage({ type: 'error', text: 'Please select both entry and destination points' });
      return;
    }

    if (formData.entryPoint === formData.destination) {
      setMessage({ type: 'error', text: 'Entry and destination must be different' });
      return;
    }

    setIsCreating(true);
    setMessage(null);

    try {
      const response = await api.post('/simulation/flows', {
        entryPoint: formData.entryPoint,
        destination: formData.destination,
        numberOfCars: parseInt(formData.numberOfCars)
      });

      if (response.data.success) {
        setMessage({
          type: 'success',
          text: `Traffic flow created! ID: ${response.data.flowId}`
        });

        // Reset form
        setFormData({
          entryPoint: '',
          destination: '',
          numberOfCars: 10
        });

        // Notify parent component
        if (onFlowCreated) {
          onFlowCreated(response.data);
        }

        // Auto-hide success message after 3 seconds
        setTimeout(() => setMessage(null), 3000);
      }
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.response?.data?.error || 'Failed to create traffic flow'
      });
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <div style={styles.container}>
      {/* Toggle Button */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        style={styles.toggleButton}
        title="Traffic Flow Control"
      >
        <span style={styles.toggleIcon}>🚗</span>
        {isExpanded ? '−' : '+'}
      </button>

      {/* Expanded Panel */}
      {isExpanded && (
        <div style={styles.panel}>
          <div style={styles.header}>
            <h3 style={styles.title}>Create Traffic Flow</h3>
            <button
              onClick={() => setIsExpanded(false)}
              style={styles.closeButton}
            >
              ×
            </button>
          </div>

          <form onSubmit={handleSubmit} style={styles.form}>
            {/* Entry Point Selection */}
            <div style={styles.formGroup}>
              <label style={styles.label}>Entry Point</label>
              <select
                value={formData.entryPoint}
                onChange={(e) => setFormData({ ...formData, entryPoint: e.target.value })}
                style={styles.select}
                required
              >
                <option value="">Select entry point...</option>
                {boundaryNodes.map(node => (
                  <option key={node.id} value={node.id}>
                    {node.id} - {node.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Destination Selection */}
            <div style={styles.formGroup}>
              <label style={styles.label}>Destination</label>
              <select
                value={formData.destination}
                onChange={(e) => setFormData({ ...formData, destination: e.target.value })}
                style={styles.select}
                required
              >
                <option value="">Select destination...</option>
                {boundaryNodes.map(node => (
                  <option key={node.id} value={node.id}>
                    {node.id} - {node.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Number of Cars */}
            <div style={styles.formGroup}>
              <label style={styles.label}>
                Number of Vehicles: <strong>{formData.numberOfCars}</strong>
              </label>
              <input
                type="range"
                min="1"
                max="50"
                value={formData.numberOfCars}
                onChange={(e) => setFormData({ ...formData, numberOfCars: e.target.value })}
                style={styles.slider}
              />
              <div style={styles.sliderLabels}>
                <span>1</span>
                <span>25</span>
                <span>50</span>
              </div>
            </div>

            {/* Message Display */}
            {message && (
              <div style={{
                ...styles.message,
                background: message.type === 'success' ? '#D1FAE5' : '#FEE2E2',
                color: message.type === 'success' ? '#065F46' : '#991B1B'
              }}>
                {message.text}
              </div>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isCreating}
              style={{
                ...styles.submitButton,
                opacity: isCreating ? 0.7 : 1,
                cursor: isCreating ? 'not-allowed' : 'pointer'
              }}
            >
              {isCreating ? 'Creating...' : '🚀 Create Traffic Flow'}
            </button>
          </form>

          {/* Info Box */}
          <div style={styles.infoBox}>
            <div style={styles.infoIcon}>ℹ️</div>
            <div style={styles.infoText}>
              Traffic flows follow the shortest path from entry to destination using Dijkstra's algorithm.
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
    bottom: '32px',
    left: '32px',
    zIndex: 1000
  },
  toggleButton: {
    width: '60px',
    height: '60px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    border: 'none',
    color: 'white',
    fontSize: '24px',
    fontWeight: 'bold',
    cursor: 'pointer',
    boxShadow: '0 4px 12px rgba(102, 126, 234, 0.4)',
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
    left: '0',
    width: '380px',
    background: 'white',
    borderRadius: '16px',
    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
    overflow: 'hidden',
    animation: 'slideUp 0.3s ease-out'
  },
  header: {
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
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
    justifyContent: 'center',
    transition: 'background 0.2s'
  },
  form: {
    padding: '24px'
  },
  formGroup: {
    marginBottom: '20px'
  },
  label: {
    display: 'block',
    fontSize: '14px',
    fontWeight: '600',
    color: '#374151',
    marginBottom: '8px'
  },
  select: {
    width: '100%',
    padding: '12px 16px',
    fontSize: '14px',
    border: '2px solid #E5E7EB',
    borderRadius: '8px',
    background: 'white',
    color: '#1F2937',
    cursor: 'pointer',
    transition: 'all 0.2s',
    outline: 'none'
  },
  slider: {
    width: '100%',
    height: '6px',
    borderRadius: '3px',
    background: '#E5E7EB',
    outline: 'none',
    cursor: 'pointer'
  },
  sliderLabels: {
    display: 'flex',
    justifyContent: 'space-between',
    marginTop: '8px',
    fontSize: '12px',
    color: '#9CA3AF'
  },
  message: {
    padding: '12px 16px',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '500',
    marginBottom: '16px'
  },
  submitButton: {
    width: '100%',
    padding: '14px 24px',
    background: 'linear-gradient(135deg, #10B981 0%, #059669 100%)',
    border: 'none',
    borderRadius: '10px',
    color: 'white',
    fontSize: '16px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s',
    boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)'
  },
  infoBox: {
    display: 'flex',
    gap: '12px',
    padding: '16px 24px',
    background: '#F0F9FF',
    borderTop: '1px solid #E0F2FE'
  },
  infoIcon: {
    fontSize: '20px',
    flexShrink: 0
  },
  infoText: {
    fontSize: '13px',
    color: '#0369A1',
    lineHeight: '1.5'
  }
};

export default TrafficFlowPanel;
