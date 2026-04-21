import React from 'react';

const SignalGridPanel = ({ signals = [] }) => {
  const getColor = (state) => {
    switch (state) {
      case 'GREEN': return '#30D158';
      case 'YELLOW': return '#FFD60A';
      case 'RED': return '#FF453A';
      default: return '#D1D1D6';
    }
  };

  const getGlow = (state) => {
    switch (state) {
      case 'GREEN': return '0 0 8px rgba(48,209,88,0.5)';
      case 'YELLOW': return '0 0 8px rgba(255,214,10,0.5)';
      case 'RED': return '0 0 8px rgba(255,69,58,0.5)';
      default: return 'none';
    }
  };

  /**
   * Derive per-direction state from backend payload.
   * Backend sends { direction, state, remainingTime } where `state` applies to the
   * `direction` currently active — the other direction is implicitly RED.
   */
  const deriveDirectionStates = (signal) => {
    const activeDir = signal.direction; // EAST_WEST | NORTH_SOUTH
    const activeState = signal.state;   // GREEN | YELLOW | RED | ALL_RED
    const remaining = signal.remainingTime ?? null;

    const inactiveState = activeState === 'ALL_RED' ? 'RED' : 'RED';

    if (activeDir === 'EAST_WEST') {
      return {
        ewState: activeState === 'ALL_RED' ? 'RED' : activeState,
        nsState: inactiveState,
        ewTimer: remaining,
        nsTimer: null,
      };
    }
    if (activeDir === 'NORTH_SOUTH') {
      return {
        nsState: activeState === 'ALL_RED' ? 'RED' : activeState,
        ewState: inactiveState,
        nsTimer: remaining,
        ewTimer: null,
      };
    }
    return { ewState: 'RED', nsState: 'RED', ewTimer: null, nsTimer: null };
  };

  if (!signals || signals.length === 0) {
    return (
      <div style={s.container}>
        <h2 style={s.title}>Intersection Signal Status</h2>
        <div style={s.empty}>
          <p style={s.emptyText}>No signal data available. Initialize the simulation to view signals.</p>
        </div>
      </div>
    );
  }

  return (
    <div style={s.container}>
      <div style={s.header}>
        <h2 style={s.title}>Intersection Signal Status</h2>
        <span style={s.count}>{signals.length} intersections</span>
      </div>

      <div style={s.grid}>
        {signals.map((signal, index) => {
          const { ewState, nsState, ewTimer, nsTimer } = deriveDirectionStates(signal);
          return (
          <div key={signal.nodeId || index} style={s.card}>
            <div style={s.cardHeader}>
              <span style={s.nodeId}>Node {signal.nodeId}</span>
              {signal.nodeName && (
                <span style={s.nodeName}>{signal.nodeName}</span>
              )}
            </div>

            <div style={s.signalRow}>
              {/* N-S Direction */}
              <div style={s.direction}>
                <span style={s.dirLabel}>N-S</span>
                <div style={s.lights}>
                  <div style={{
                    ...s.light,
                    background: nsState === 'RED' ? '#FF453A' : '#E5E5EA',
                    boxShadow: nsState === 'RED' ? getGlow('RED') : 'none',
                  }}/>
                  <div style={{
                    ...s.light,
                    background: nsState === 'YELLOW' ? '#FFD60A' : '#E5E5EA',
                    boxShadow: nsState === 'YELLOW' ? getGlow('YELLOW') : 'none',
                  }}/>
                  <div style={{
                    ...s.light,
                    background: nsState === 'GREEN' ? '#30D158' : '#E5E5EA',
                    boxShadow: nsState === 'GREEN' ? getGlow('GREEN') : 'none',
                  }}/>
                </div>
                <span style={{...s.timer, color: getColor(nsState)}}>
                  {nsTimer != null ? `${nsTimer}s` : '--'}
                </span>
              </div>

              {/* Divider */}
              <div style={s.divider}/>

              {/* E-W Direction */}
              <div style={s.direction}>
                <span style={s.dirLabel}>E-W</span>
                <div style={s.lights}>
                  <div style={{
                    ...s.light,
                    background: ewState === 'RED' ? '#FF453A' : '#E5E5EA',
                    boxShadow: ewState === 'RED' ? getGlow('RED') : 'none',
                  }}/>
                  <div style={{
                    ...s.light,
                    background: ewState === 'YELLOW' ? '#FFD60A' : '#E5E5EA',
                    boxShadow: ewState === 'YELLOW' ? getGlow('YELLOW') : 'none',
                  }}/>
                  <div style={{
                    ...s.light,
                    background: ewState === 'GREEN' ? '#30D158' : '#E5E5EA',
                    boxShadow: ewState === 'GREEN' ? getGlow('GREEN') : 'none',
                  }}/>
                </div>
                <span style={{...s.timer, color: getColor(ewState)}}>
                  {ewTimer != null ? `${ewTimer}s` : '--'}
                </span>
              </div>
            </div>
          </div>
          );
        })}
      </div>
    </div>
  );
};

const s = {
  container: {
    background: '#FFFFFF',
    borderRadius: '24px',
    padding: '28px 32px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
    border: '1px solid rgba(0,0,0,0.06)',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px',
  },
  title: {
    margin: 0,
    fontSize: '22px',
    fontWeight: '600',
    color: '#1D1D1F',
    letterSpacing: '-0.26px',
  },
  count: {
    fontSize: '13px',
    color: '#86868B',
    fontWeight: '500',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
    gap: '12px',
  },
  card: {
    background: '#F5F5F7',
    borderRadius: '16px',
    padding: '16px',
    border: '1px solid rgba(0,0,0,0.04)',
  },
  cardHeader: {
    marginBottom: '12px',
    paddingBottom: '10px',
    borderBottom: '1px solid rgba(0,0,0,0.06)',
  },
  nodeId: {
    display: 'block',
    fontSize: '14px',
    fontWeight: '600',
    color: '#1D1D1F',
  },
  nodeName: {
    display: 'block',
    fontSize: '11px',
    color: '#86868B',
    marginTop: '2px',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  signalRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '16px',
  },
  direction: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '6px',
  },
  dirLabel: {
    fontSize: '11px',
    fontWeight: '600',
    color: '#86868B',
    textTransform: 'uppercase',
    letterSpacing: '0.4px',
  },
  lights: {
    display: 'flex',
    gap: '4px',
  },
  light: {
    width: '12px',
    height: '12px',
    borderRadius: '50%',
    transition: 'all 0.3s ease',
  },
  timer: {
    fontSize: '13px',
    fontWeight: '700',
    fontFamily: 'var(--apple-mono, monospace)',
  },
  divider: {
    width: '1px',
    height: '40px',
    background: 'rgba(0,0,0,0.06)',
  },
  empty: {
    padding: '40px',
    textAlign: 'center',
  },
  emptyText: {
    fontSize: '14px',
    color: '#86868B',
  },
};

export default SignalGridPanel;
