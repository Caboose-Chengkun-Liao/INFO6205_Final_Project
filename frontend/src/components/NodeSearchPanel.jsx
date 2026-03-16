import React, { useState, useEffect } from 'react';

/**
 * Node Search and Filter Panel
 * Search nodes by ID/name and filter by type
 */
const NodeSearchPanel = ({ nodes, onNodeSelect }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('ALL');
  const [filteredNodes, setFilteredNodes] = useState([]);

  useEffect(() => {
    if (!nodes) return;

    let filtered = nodes;

    // Filter by type
    if (filterType !== 'ALL') {
      filtered = filtered.filter(node => node.type === filterType);
    }

    // Filter by search term
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(node =>
        node.id.toLowerCase().includes(term) ||
        (node.name && node.name.toLowerCase().includes(term))
      );
    }

    setFilteredNodes(filtered);
  }, [nodes, searchTerm, filterType]);

  const handleNodeClick = (node) => {
    if (onNodeSelect) {
      onNodeSelect(node);
    }
    // Optionally close panel after selection
    // setIsExpanded(false);
  };

  const handleClearSearch = () => {
    setSearchTerm('');
    setFilterType('ALL');
  };

  // Count nodes by type
  const intersectionCount = nodes?.filter(n => n.type === 'INTERSECTION').length || 0;
  const boundaryCount = nodes?.filter(n => n.type === 'BOUNDARY').length || 0;

  return (
    <div style={styles.container}>
      {/* Toggle Button */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        style={styles.toggleButton}
        title="Search Nodes"
      >
        <span style={styles.toggleIcon}>🔍</span>
        {isExpanded ? '−' : '+'}
      </button>

      {/* Expanded Panel */}
      {isExpanded && (
        <div style={styles.panel}>
          <div style={styles.header}>
            <h3 style={styles.title}>Node Search</h3>
            <button
              onClick={() => setIsExpanded(false)}
              style={styles.closeButton}
            >
              ×
            </button>
          </div>

          {/* Search Input */}
          <div style={styles.searchSection}>
            <div style={styles.searchInputWrapper}>
              <span style={styles.searchIcon}>🔍</span>
              <input
                type="text"
                placeholder="Search by ID or name..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={styles.searchInput}
              />
              {searchTerm && (
                <button
                  onClick={handleClearSearch}
                  style={styles.clearButton}
                >
                  ×
                </button>
              )}
            </div>
          </div>

          {/* Filter Buttons */}
          <div style={styles.filterSection}>
            <button
              onClick={() => setFilterType('ALL')}
              style={{
                ...styles.filterButton,
                background: filterType === 'ALL' ? '#667eea' : '#F3F4F6',
                color: filterType === 'ALL' ? 'white' : '#6B7280'
              }}
            >
              All ({nodes?.length || 0})
            </button>
            <button
              onClick={() => setFilterType('INTERSECTION')}
              style={{
                ...styles.filterButton,
                background: filterType === 'INTERSECTION' ? '#2563EB' : '#F3F4F6',
                color: filterType === 'INTERSECTION' ? 'white' : '#6B7280'
              }}
            >
              Intersections ({intersectionCount})
            </button>
            <button
              onClick={() => setFilterType('BOUNDARY')}
              style={{
                ...styles.filterButton,
                background: filterType === 'BOUNDARY' ? '#DC2626' : '#F3F4F6',
                color: filterType === 'BOUNDARY' ? 'white' : '#6B7280'
              }}
            >
              Boundaries ({boundaryCount})
            </button>
          </div>

          {/* Results Count */}
          <div style={styles.resultsCount}>
            {filteredNodes.length} {filteredNodes.length === 1 ? 'result' : 'results'}
            {searchTerm && ` for "${searchTerm}"`}
          </div>

          {/* Node List */}
          <div style={styles.nodeList}>
            {filteredNodes.length > 0 ? (
              filteredNodes.map(node => (
                <div
                  key={node.id}
                  onClick={() => handleNodeClick(node)}
                  style={styles.nodeItem}
                >
                  <div style={styles.nodeIcon}>
                    {node.type === 'INTERSECTION' ? '🚦' : '🚪'}
                  </div>
                  <div style={styles.nodeInfo}>
                    <div style={styles.nodeId}>{node.id}</div>
                    {node.name && (
                      <div style={styles.nodeName}>{node.name}</div>
                    )}
                    <div style={styles.nodeType}>
                      <span
                        style={{
                          ...styles.typeBadge,
                          background: node.type === 'INTERSECTION'
                            ? '#DBEAFE'
                            : '#FEE2E2',
                          color: node.type === 'INTERSECTION'
                            ? '#1E40AF'
                            : '#991B1B'
                        }}
                      >
                        {node.type}
                      </span>
                    </div>
                  </div>
                  <div style={styles.coordinates}>
                    <div style={styles.coordLabel}>X: {node.x?.toFixed(2)}</div>
                    <div style={styles.coordLabel}>Y: {node.y?.toFixed(2)}</div>
                  </div>
                </div>
              ))
            ) : (
              <div style={styles.emptyState}>
                <div style={styles.emptyIcon}>🔍</div>
                <div style={styles.emptyText}>
                  {searchTerm
                    ? `No nodes found for "${searchTerm}"`
                    : 'No nodes match the current filters'}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

const styles = {
  container: {
    position: 'fixed',
    top: '120px',
    left: '32px',
    zIndex: 1000
  },
  toggleButton: {
    width: '60px',
    height: '60px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)',
    border: 'none',
    color: 'white',
    fontSize: '24px',
    fontWeight: 'bold',
    cursor: 'pointer',
    boxShadow: '0 4px 12px rgba(245, 158, 11, 0.4)',
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
    top: '70px',
    left: '0',
    width: '380px',
    maxHeight: '600px',
    background: 'white',
    borderRadius: '16px',
    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
    overflow: 'hidden',
    display: 'flex',
    flexDirection: 'column'
  },
  header: {
    background: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)',
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
  searchSection: {
    padding: '16px 20px'
  },
  searchInputWrapper: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center'
  },
  searchIcon: {
    position: 'absolute',
    left: '12px',
    fontSize: '18px',
    pointerEvents: 'none'
  },
  searchInput: {
    flex: 1,
    padding: '12px 40px 12px 44px',
    fontSize: '14px',
    border: '2px solid #E5E7EB',
    borderRadius: '12px',
    outline: 'none',
    transition: 'border-color 0.2s'
  },
  clearButton: {
    position: 'absolute',
    right: '8px',
    width: '28px',
    height: '28px',
    borderRadius: '50%',
    border: 'none',
    background: '#EF4444',
    color: 'white',
    fontSize: '18px',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  },
  filterSection: {
    display: 'flex',
    gap: '8px',
    padding: '0 20px 16px 20px'
  },
  filterButton: {
    flex: 1,
    padding: '10px 12px',
    border: 'none',
    borderRadius: '8px',
    fontSize: '12px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s',
    whiteSpace: 'nowrap'
  },
  resultsCount: {
    padding: '8px 20px',
    fontSize: '13px',
    color: '#6B7280',
    fontWeight: '500',
    background: '#F9FAFB',
    borderTop: '1px solid #E5E7EB',
    borderBottom: '1px solid #E5E7EB'
  },
  nodeList: {
    flex: 1,
    overflowY: 'auto',
    padding: '12px'
  },
  nodeItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px',
    background: '#F9FAFB',
    borderRadius: '12px',
    marginBottom: '8px',
    cursor: 'pointer',
    transition: 'all 0.2s',
    border: '2px solid transparent'
  },
  nodeIcon: {
    fontSize: '24px',
    flexShrink: 0
  },
  nodeInfo: {
    flex: 1,
    minWidth: 0
  },
  nodeId: {
    fontSize: '16px',
    fontWeight: '700',
    color: '#1F2937',
    marginBottom: '2px'
  },
  nodeName: {
    fontSize: '12px',
    color: '#6B7280',
    marginBottom: '4px',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap'
  },
  nodeType: {
    marginTop: '4px'
  },
  typeBadge: {
    display: 'inline-block',
    padding: '2px 8px',
    borderRadius: '4px',
    fontSize: '10px',
    fontWeight: '600',
    textTransform: 'uppercase'
  },
  coordinates: {
    display: 'flex',
    flexDirection: 'column',
    gap: '2px',
    fontSize: '11px',
    color: '#9CA3AF',
    textAlign: 'right'
  },
  coordLabel: {
    fontFamily: 'monospace'
  },
  emptyState: {
    padding: '48px 24px',
    textAlign: 'center'
  },
  emptyIcon: {
    fontSize: '64px',
    marginBottom: '16px',
    opacity: 0.3
  },
  emptyText: {
    fontSize: '14px',
    color: '#9CA3AF',
    lineHeight: '1.5'
  }
};

export default NodeSearchPanel;
