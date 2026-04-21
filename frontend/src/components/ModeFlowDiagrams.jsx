import React, { useEffect, useState, useRef } from 'react';

/**
 * Animated step-by-step workflow combining MAP context + ALGORITHM logic.
 * Each step shows a mini-intersection visualization alongside the algorithm code.
 * Professional SVG iconography — no emojis.
 */

// ==================== SVG ICON LIBRARY ====================

const Icon = ({ name, size = 24, color = 'currentColor' }) => {
  const icons = {
    clock: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinecap="round">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 2" />
      </g>
    ),
    branch: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinecap="round">
        <circle cx="6" cy="6" r="2" />
        <circle cx="18" cy="6" r="2" />
        <circle cx="12" cy="18" r="2" />
        <path d="M8 6h4m4 0h-4M12 8v8" />
      </g>
    ),
    stop: (
      <g stroke={color} strokeWidth="2" fill="none">
        <circle cx="12" cy="12" r="9" />
        <path d="M6 6l12 12" strokeLinecap="round" />
      </g>
    ),
    signal: (
      <g stroke={color} strokeWidth="2" fill="none">
        <rect x="7" y="3" width="10" height="18" rx="2" />
        <circle cx="12" cy="7" r="1.5" fill={color} />
        <circle cx="12" cy="12" r="1.5" opacity="0.3" />
        <circle cx="12" cy="17" r="1.5" opacity="0.3" />
      </g>
    ),
    infinity: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinecap="round">
        <path d="M8 12c-3 0-4-2-4-4s1-4 4-4c2 0 4 2 6 4s4 4 6 4 4-2 4-4-1-4-4-4c-2 0-4 2-6 4" />
      </g>
    ),
    sensor: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinecap="round">
        <path d="M12 2a10 10 0 0 0-7 3" opacity="0.3" />
        <path d="M12 6a6 6 0 0 0-4 2" opacity="0.6" />
        <path d="M12 10a2 2 0 0 0-1.5 0.6" />
        <circle cx="12" cy="14" r="1.5" fill={color} />
      </g>
    ),
    chart: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinecap="round">
        <path d="M4 20V10M10 20V4M16 20v-8M22 20v-14" />
        <path d="M2 20h22" />
      </g>
    ),
    formula: (
      <g stroke={color} strokeWidth="2" fill="none">
        <rect x="3" y="5" width="18" height="14" rx="2" />
        <path d="M7 12h4M14 9l3 6M17 9l-3 6" strokeLinecap="round" />
      </g>
    ),
    shield: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinejoin="round">
        <path d="M12 3l8 3v6c0 4-3 8-8 9-5-1-8-5-8-9V6l8-3z" />
      </g>
    ),
    split: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinecap="round">
        <path d="M12 4v4" />
        <path d="M12 8l-6 6v6" />
        <path d="M12 8l6 6v6" />
        <circle cx="12" cy="4" r="1" fill={color} />
      </g>
    ),
    apply: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinecap="round">
        <path d="M4 12l5 5L20 6" />
      </g>
    ),
    sparkle: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinecap="round">
        <path d="M12 3v4M12 17v4M3 12h4M17 12h4M5.6 5.6l2.8 2.8M15.6 15.6l2.8 2.8M5.6 18.4l2.8-2.8M15.6 8.4l2.8-2.8" />
      </g>
    ),
    eye: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinecap="round">
        <path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7z" />
        <circle cx="12" cy="12" r="3" />
      </g>
    ),
    target: (
      <g stroke={color} strokeWidth="2" fill="none">
        <circle cx="12" cy="12" r="9" />
        <circle cx="12" cy="12" r="5" />
        <circle cx="12" cy="12" r="1.5" fill={color} />
      </g>
    ),
    dice: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinejoin="round">
        <rect x="4" y="4" width="16" height="16" rx="3" />
        <circle cx="9" cy="9" r="1.2" fill={color} />
        <circle cx="15" cy="15" r="1.2" fill={color} />
        <circle cx="12" cy="12" r="1.2" fill={color} />
      </g>
    ),
    bolt: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinejoin="round">
        <path d="M13 3L4 14h7l-1 7 9-11h-7l1-7z" />
      </g>
    ),
    brain: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinecap="round">
        <path d="M12 3a4 4 0 0 0-4 4v1a4 4 0 0 0-2 7 3 3 0 0 0 3 3h6a3 3 0 0 0 3-3 4 4 0 0 0-2-7V7a4 4 0 0 0-4-4z" />
        <path d="M12 3v18" opacity="0.4" />
      </g>
    ),
    save: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinejoin="round">
        <path d="M5 3h11l3 3v15H5z" />
        <path d="M8 3v6h8V3" />
        <rect x="8" y="13" width="8" height="6" fill="none" />
      </g>
    ),
    trending: (
      <g stroke={color} strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round">
        <path d="M3 17l6-6 4 4 8-8" />
        <path d="M14 7h7v7" />
      </g>
    ),
  };

  return (
    <svg width={size} height={size} viewBox="0 0 24 24">
      {icons[name] || icons.signal}
    </svg>
  );
};

// ==================== MAP VISUALIZATION COMPONENTS ====================

/** A mini intersection with 4 approaches, traffic lights, and optional vehicles */
const MiniIntersection = ({
  greenDir = 'EW',
  vehicles = { N: 0, S: 0, E: 0, W: 0 },
  highlight = null,
  color = '#0071E3',
  scale = 1,
}) => {
  const size = 200 * scale;
  const cx = size / 2;
  const cy = size / 2;
  const roadW = 36 * scale;

  const renderVehicles = (dir, count, direction) => {
    if (!count) return null;
    const dots = [];
    const spacing = 14 * scale;
    for (let i = 0; i < Math.min(count, 4); i++) {
      let x = cx, y = cy;
      if (dir === 'N') { x = cx - 6 * scale; y = 18 * scale + i * spacing; }
      if (dir === 'S') { x = cx + 6 * scale; y = size - 18 * scale - i * spacing; }
      if (dir === 'W') { x = 18 * scale + i * spacing; y = cy + 6 * scale; }
      if (dir === 'E') { x = size - 18 * scale - i * spacing; y = cy - 6 * scale; }
      dots.push(
        <rect
          key={`${dir}-${i}`}
          x={x - 4 * scale}
          y={y - 3 * scale}
          width={8 * scale}
          height={6 * scale}
          rx={1}
          fill="#30D158"
          opacity={0.9 - i * 0.15}
        />
      );
    }
    return dots;
  };

  const signalFor = (dir) => {
    const isGreen = greenDir === 'EW' ? (dir === 'E' || dir === 'W') : (dir === 'N' || dir === 'S');
    return isGreen ? '#30D158' : '#FF453A';
  };

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
      {/* Background */}
      <rect width={size} height={size} fill="#f5f5f7" rx={8} />

      {/* Roads */}
      <rect x={cx - roadW / 2} y={0} width={roadW} height={size} fill="#d4d4d8" />
      <rect x={0} y={cy - roadW / 2} width={size} height={roadW} fill="#d4d4d8" />

      {/* Lane dividers */}
      <line x1={cx} y1={0} x2={cx} y2={cy - roadW / 2} stroke="#fff" strokeWidth="1" strokeDasharray="4 4" />
      <line x1={cx} y1={cy + roadW / 2} x2={cx} y2={size} stroke="#fff" strokeWidth="1" strokeDasharray="4 4" />
      <line x1={0} y1={cy} x2={cx - roadW / 2} y2={cy} stroke="#fff" strokeWidth="1" strokeDasharray="4 4" />
      <line x1={cx + roadW / 2} y1={cy} x2={size} y2={cy} stroke="#fff" strokeWidth="1" strokeDasharray="4 4" />

      {/* Vehicles */}
      {renderVehicles('N', vehicles.N, 'vertical')}
      {renderVehicles('S', vehicles.S, 'vertical')}
      {renderVehicles('E', vehicles.E, 'horizontal')}
      {renderVehicles('W', vehicles.W, 'horizontal')}

      {/* Center intersection */}
      <rect
        x={cx - roadW / 2}
        y={cy - roadW / 2}
        width={roadW}
        height={roadW}
        fill={highlight ? color : '#b8b8bc'}
        opacity={highlight ? 0.3 : 0.5}
      />

      {/* Traffic light poles */}
      {/* North pole */}
      <circle cx={cx - roadW / 2 - 8 * scale} cy={cy - roadW / 2 - 8 * scale} r={5 * scale} fill={signalFor('N')} />
      {/* South pole */}
      <circle cx={cx + roadW / 2 + 8 * scale} cy={cy + roadW / 2 + 8 * scale} r={5 * scale} fill={signalFor('S')} />
      {/* East pole */}
      <circle cx={cx + roadW / 2 + 8 * scale} cy={cy - roadW / 2 - 8 * scale} r={5 * scale} fill={signalFor('E')} />
      {/* West pole */}
      <circle cx={cx - roadW / 2 - 8 * scale} cy={cy + roadW / 2 + 8 * scale} r={5 * scale} fill={signalFor('W')} />

      {/* Center node marker if highlighted */}
      {highlight && (
        <circle cx={cx} cy={cy} r={8 * scale} fill={color} opacity="0.9">
          <animate attributeName="r" values={`${8 * scale};${12 * scale};${8 * scale}`} dur="1.5s" repeatCount="indefinite" />
          <animate attributeName="opacity" values="0.9;0.3;0.9" dur="1.5s" repeatCount="indefinite" />
        </circle>
      )}
    </svg>
  );
};

/** Signal timing bar — horizontal timeline showing green/yellow/red phases */
const SignalTimingBar = ({ green = 20, yellow = 3, red = 23, label = 'Cycle', color = '#FF453A' }) => {
  const total = green + yellow + red;
  return (
    <div style={{ background: '#fff', borderRadius: 10, padding: '14px 16px', border: '1px solid #eaeaea' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, alignItems: 'baseline' }}>
        <span style={{ fontSize: 11, fontWeight: 700, color: '#86868B', letterSpacing: 0.5 }}>{label}</span>
        <span style={{ fontSize: 11, color: color, fontWeight: 600 }}>{total}s total</span>
      </div>
      <div style={{ display: 'flex', height: 18, borderRadius: 4, overflow: 'hidden' }}>
        <div style={{ flex: green, background: '#30D158', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 10, fontWeight: 700 }}>{green}s</div>
        <div style={{ flex: yellow, background: '#FFD60A', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#000', fontSize: 10, fontWeight: 700 }}>{yellow}s</div>
        <div style={{ flex: red, background: '#FF453A', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 10, fontWeight: 700 }}>{red}s</div>
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 9, color: '#86868B', marginTop: 4 }}>
        <span>GREEN</span><span>YELLOW</span><span>RED</span>
      </div>
    </div>
  );
};

/** Q-Table visualization — small matrix of state × action values */
const QTableView = ({ activeState = 'MEDIUM', activeAction = 2, color = '#30D158' }) => {
  const states = ['LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'];
  const actions = [15, 20, 30, 45, 60];
  return (
    <div style={{ background: '#fff', borderRadius: 10, padding: 14, border: '1px solid #eaeaea' }}>
      <div style={{ fontSize: 11, fontWeight: 700, color: '#86868B', marginBottom: 8, letterSpacing: 0.5 }}>Q-TABLE</div>
      <div style={{ display: 'grid', gridTemplateColumns: 'auto repeat(5, 1fr)', gap: 3, fontSize: 10 }}>
        <div />
        {actions.map(a => (
          <div key={a} style={{ textAlign: 'center', fontWeight: 600, color: '#555', padding: '2px 0' }}>{a}s</div>
        ))}
        {states.map((s, si) => (
          <React.Fragment key={s}>
            <div style={{ fontWeight: 600, color: '#555', padding: '4px 6px 4px 0', textAlign: 'right', fontSize: 9 }}>{s}</div>
            {actions.map((a, ai) => {
              const isActive = s === activeState && ai === activeAction;
              const isRow = s === activeState;
              return (
                <div
                  key={a}
                  style={{
                    background: isActive ? color : (isRow ? '#fafafa' : '#f5f5f7'),
                    color: isActive ? '#fff' : '#86868B',
                    borderRadius: 3,
                    padding: '4px 2px',
                    textAlign: 'center',
                    fontWeight: isActive ? 700 : 400,
                    fontFamily: 'SF Mono, Menlo, monospace',
                    fontSize: 9,
                    boxShadow: isActive ? `0 0 0 2px ${color}66` : 'none',
                  }}
                >
                  {isActive ? '★' : (Math.random() * 2 - 1).toFixed(1)}
                </div>
              );
            })}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
};

/** Flow ratio bar chart — shows y1 and y2 as stacked bars */
const FlowRatioChart = ({ y1 = 0.45, y2 = 0.32, color = '#FF9F0A' }) => {
  return (
    <div style={{ background: '#fff', borderRadius: 10, padding: 14, border: '1px solid #eaeaea' }}>
      <div style={{ fontSize: 11, fontWeight: 700, color: '#86868B', marginBottom: 10, letterSpacing: 0.5 }}>FLOW RATIOS</div>
      {[{ name: 'y₁ (primary)', val: y1 }, { name: 'y₂ (cross)', val: y2 }].map(item => (
        <div key={item.name} style={{ marginBottom: 8 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#555', marginBottom: 3 }}>
            <span>{item.name}</span>
            <span style={{ fontFamily: 'SF Mono, monospace', fontWeight: 700, color }}>{item.val.toFixed(2)}</span>
          </div>
          <div style={{ height: 6, background: '#f0f0f2', borderRadius: 3, overflow: 'hidden' }}>
            <div style={{ width: `${item.val * 100}%`, height: '100%', background: color, borderRadius: 3 }} />
          </div>
        </div>
      ))}
      <div style={{ borderTop: '1px dashed #e0e0e0', marginTop: 8, paddingTop: 8, display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#555' }}>
        <span>Σy (saturation)</span>
        <span style={{ fontFamily: 'SF Mono, monospace', fontWeight: 700 }}>{(y1 + y2).toFixed(2)}</span>
      </div>
    </div>
  );
};

/** Reward indicator */
const RewardIndicator = ({ value = 3, color = '#30D158' }) => {
  const isPositive = value >= 0;
  const c = isPositive ? '#30D158' : '#FF453A';
  return (
    <div style={{ background: '#fff', borderRadius: 10, padding: 14, border: '1px solid #eaeaea' }}>
      <div style={{ fontSize: 11, fontWeight: 700, color: '#86868B', marginBottom: 10, letterSpacing: 0.5 }}>REWARD r</div>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'center', gap: 6 }}>
        <span style={{ fontSize: 40, fontWeight: 800, color: c, fontFamily: 'SF Mono, Menlo, monospace' }}>
          {isPositive ? '+' : ''}{value}
        </span>
        <span style={{ fontSize: 12, color: '#86868B' }}>vehicles</span>
      </div>
      <div style={{ fontSize: 10, textAlign: 'center', color: '#86868B', marginTop: 6 }}>
        {isPositive ? 'queue decreased — good action' : 'queue increased — bad action'}
      </div>
    </div>
  );
};

/** Epsilon-greedy decision split */
const EpsilonSplit = ({ epsilon = 0.1, chose = 'exploit', color = '#30D158' }) => {
  return (
    <div style={{ background: '#fff', borderRadius: 10, padding: 14, border: '1px solid #eaeaea' }}>
      <div style={{ fontSize: 11, fontWeight: 700, color: '#86868B', marginBottom: 10, letterSpacing: 0.5 }}>ε-GREEDY (ε = {epsilon})</div>
      <div style={{ display: 'flex', gap: 6 }}>
        <div style={{
          flex: 1,
          padding: 10,
          background: chose === 'explore' ? color : '#f5f5f7',
          color: chose === 'explore' ? '#fff' : '#86868B',
          borderRadius: 8,
          textAlign: 'center',
          fontWeight: 700,
          fontSize: 11,
          transition: 'all 0.3s',
        }}>
          EXPLORE
          <div style={{ fontSize: 10, fontWeight: 400, marginTop: 2, opacity: 0.85 }}>
            random (prob {epsilon})
          </div>
        </div>
        <div style={{
          flex: 1,
          padding: 10,
          background: chose === 'exploit' ? color : '#f5f5f7',
          color: chose === 'exploit' ? '#fff' : '#86868B',
          borderRadius: 8,
          textAlign: 'center',
          fontWeight: 700,
          fontSize: 11,
          transition: 'all 0.3s',
        }}>
          EXPLOIT
          <div style={{ fontSize: 10, fontWeight: 400, marginTop: 2, opacity: 0.85 }}>
            best Q (prob {(1 - epsilon).toFixed(1)})
          </div>
        </div>
      </div>
    </div>
  );
};

/** State-bucket discretization bar */
const StateBuckets = ({ activeIdx = 1, color = '#30D158' }) => {
  const buckets = [
    { name: 'LOW', range: '≤5', sample: 2 },
    { name: 'MEDIUM', range: '6–15', sample: 12 },
    { name: 'HIGH', range: '16–30', sample: 22 },
    { name: 'VERY_HIGH', range: '>30', sample: 45 },
  ];
  return (
    <div style={{ background: '#fff', borderRadius: 10, padding: 14, border: '1px solid #eaeaea' }}>
      <div style={{ fontSize: 11, fontWeight: 700, color: '#86868B', marginBottom: 10, letterSpacing: 0.5 }}>STATE DISCRETIZATION</div>
      <div style={{ display: 'flex', gap: 4 }}>
        {buckets.map((b, i) => {
          const isActive = i === activeIdx;
          return (
            <div key={b.name} style={{
              flex: 1,
              padding: '10px 6px',
              background: isActive ? color : '#f5f5f7',
              color: isActive ? '#fff' : '#86868B',
              borderRadius: 6,
              textAlign: 'center',
              transition: 'all 0.3s',
              boxShadow: isActive ? `0 4px 12px ${color}55` : 'none',
              transform: isActive ? 'translateY(-2px)' : 'none',
            }}>
              <div style={{ fontSize: 9, fontWeight: 700 }}>{b.name}</div>
              <div style={{ fontSize: 10, fontFamily: 'SF Mono, monospace', marginTop: 2 }}>{b.range}</div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

/** Action space — visual bar with 5 choices */
const ActionSpace = ({ chosenIdx = 2, color = '#30D158' }) => {
  const actions = [15, 20, 30, 45, 60];
  return (
    <div style={{ background: '#fff', borderRadius: 10, padding: 14, border: '1px solid #eaeaea' }}>
      <div style={{ fontSize: 11, fontWeight: 700, color: '#86868B', marginBottom: 10, letterSpacing: 0.5 }}>ACTION SPACE (green duration)</div>
      <div style={{ display: 'flex', gap: 6, justifyContent: 'space-between' }}>
        {actions.map((a, i) => {
          const isChosen = i === chosenIdx;
          return (
            <div key={a} style={{
              flex: 1,
              textAlign: 'center',
              padding: '12px 4px',
              background: isChosen ? color : '#f5f5f7',
              color: isChosen ? '#fff' : '#86868B',
              borderRadius: 8,
              transition: 'all 0.3s',
              boxShadow: isChosen ? `0 4px 14px ${color}66` : 'none',
              transform: isChosen ? 'translateY(-3px) scale(1.05)' : 'none',
            }}>
              <div style={{ fontSize: 16, fontWeight: 800 }}>{a}</div>
              <div style={{ fontSize: 9, opacity: 0.8 }}>sec</div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

/** Webster formula display with highlighted terms */
const WebsterFormula = ({ color = '#FF9F0A' }) => (
  <div style={{ background: '#1d1d1f', borderRadius: 10, padding: '18px 20px' }}>
    <div style={{ color: '#86868B', fontSize: 10, fontWeight: 700, letterSpacing: 1, marginBottom: 10 }}>WEBSTER OPTIMAL CYCLE (1958)</div>
    <div style={{ color: '#fff', fontFamily: 'SF Mono, Menlo, monospace', fontSize: 20, textAlign: 'center', lineHeight: 1.4 }}>
      C<sub>0</sub> = <span style={{ color }}>(1.5L + 5)</span> / <span style={{ color: '#FFD60A' }}>(1 − Σyᵢ)</span>
    </div>
    <div style={{ color: '#888', fontSize: 11, marginTop: 10, display: 'flex', justifyContent: 'space-between' }}>
      <span><span style={{ color }}>L</span> = lost time (start + all-red)</span>
      <span><span style={{ color: '#FFD60A' }}>Σyᵢ</span> = flow ratio sum</span>
    </div>
  </div>
);

/** Bellman equation display */
const BellmanFormula = ({ color = '#30D158' }) => (
  <div style={{ background: '#1d1d1f', borderRadius: 10, padding: '18px 20px' }}>
    <div style={{ color: '#86868B', fontSize: 10, fontWeight: 700, letterSpacing: 1, marginBottom: 10 }}>BELLMAN UPDATE EQUATION</div>
    <div style={{ color: '#fff', fontFamily: 'SF Mono, Menlo, monospace', fontSize: 15, textAlign: 'center', lineHeight: 1.8 }}>
      Q(s,a) ← Q(s,a) + <span style={{ color }}>α</span>[<span style={{ color: '#FFD60A' }}>r</span> + <span style={{ color }}>γ</span>·max Q(s′,a′) − Q(s,a)]
    </div>
    <div style={{ color: '#888', fontSize: 11, marginTop: 10, display: 'flex', justifyContent: 'space-around' }}>
      <span><span style={{ color }}>α</span>=0.1 learning rate</span>
      <span><span style={{ color }}>γ</span>=0.9 discount</span>
      <span><span style={{ color: '#FFD60A' }}>r</span> = reward</span>
    </div>
  </div>
);

/** Range clamp indicator */
const RangeIndicator = ({ min = 40, max = 180, value = 85, color = '#FF9F0A' }) => {
  const pct = ((value - min) / (max - min)) * 100;
  return (
    <div style={{ background: '#fff', borderRadius: 10, padding: 14, border: '1px solid #eaeaea' }}>
      <div style={{ fontSize: 11, fontWeight: 700, color: '#86868B', marginBottom: 10, letterSpacing: 0.5 }}>CYCLE RANGE CLAMP</div>
      <div style={{ position: 'relative', height: 40 }}>
        <div style={{ position: 'absolute', top: 14, left: 0, right: 0, height: 10, background: `linear-gradient(90deg, ${color}33, ${color})`, borderRadius: 5 }} />
        <div style={{ position: 'absolute', top: 10, left: `${pct}%`, transform: 'translateX(-50%)', width: 18, height: 18, borderRadius: '50%', background: color, border: '3px solid #fff', boxShadow: `0 2px 10px ${color}88` }} />
        <div style={{ position: 'absolute', bottom: 0, left: 0, fontSize: 10, color: '#86868B' }}>{min}s (min)</div>
        <div style={{ position: 'absolute', bottom: 0, right: 0, fontSize: 10, color: '#86868B' }}>{max}s (max)</div>
        <div style={{ position: 'absolute', top: -4, left: `${pct}%`, transform: 'translateX(-50%)', fontSize: 11, fontWeight: 700, color }}>
          {value}s
        </div>
      </div>
    </div>
  );
};

/** Learning curve graph */
const LearningCurve = ({ color = '#30D158' }) => {
  const points = [];
  for (let i = 0; i <= 40; i++) {
    const x = i * 5;
    const y = 160 - 140 * (1 - Math.exp(-i / 15)) - Math.random() * 8;
    points.push(`${x},${y}`);
  }
  return (
    <div style={{ background: '#fff', borderRadius: 10, padding: 14, border: '1px solid #eaeaea' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 8 }}>
        <span style={{ fontSize: 11, fontWeight: 700, color: '#86868B', letterSpacing: 0.5 }}>CONVERGENCE</span>
        <span style={{ fontSize: 10, color }}>Policy quality ↑ over time</span>
      </div>
      <svg viewBox="0 0 220 170" width="100%" height="100">
        {/* Grid lines */}
        <line x1="0" y1="40" x2="220" y2="40" stroke="#f0f0f2" />
        <line x1="0" y1="80" x2="220" y2="80" stroke="#f0f0f2" />
        <line x1="0" y1="120" x2="220" y2="120" stroke="#f0f0f2" />
        <line x1="0" y1="160" x2="220" y2="160" stroke="#e0e0e2" />
        {/* Area under curve */}
        <polygon
          points={`0,160 ${points.join(' ')} 220,160`}
          fill={color}
          opacity="0.12"
        />
        {/* Curve */}
        <polyline
          points={points.join(' ')}
          fill="none"
          stroke={color}
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <text x="5" y="35" fontSize="9" fill="#86868B">optimal</text>
        <text x="160" y="165" fontSize="9" fill="#86868B">episodes →</text>
      </svg>
    </div>
  );
};

/** Mode check branch display */
const BranchView = ({ modes = ['FIXED_TIME', 'TRAFFIC_ADAPTIVE', 'GREEN_WAVE'], selectedIdx = 0, color = '#FF453A' }) => (
  <div style={{ background: '#fff', borderRadius: 10, padding: 14, border: '1px solid #eaeaea' }}>
    <div style={{ fontSize: 11, fontWeight: 700, color: '#86868B', marginBottom: 10, letterSpacing: 0.5 }}>switch(mode)</div>
    {modes.map((m, i) => {
      const isSelected = i === selectedIdx;
      return (
        <div key={m} style={{
          display: 'flex',
          alignItems: 'center',
          gap: 10,
          padding: '6px 10px',
          borderRadius: 6,
          background: isSelected ? color : '#fafafa',
          color: isSelected ? '#fff' : '#555',
          marginBottom: 4,
          fontFamily: 'SF Mono, monospace',
          fontSize: 11,
          fontWeight: isSelected ? 700 : 500,
          transition: 'all 0.3s',
        }}>
          <span style={{ opacity: 0.6 }}>case</span>
          <span style={{ flex: 1 }}>{m}</span>
          {isSelected && <span>✓ matched</span>}
        </div>
      );
    })}
  </div>
);

// ==================== MODE DEFINITIONS ====================

const MODES = [
  {
    key: 'fixed',
    title: 'FIXED TIMING',
    subtitle: 'Static Fixed-Cycle Control',
    color: '#FF453A',
    gradientStart: '#FF453A',
    gradientEnd: '#cc3333',
    softBg: 'rgba(255,69,58,0.06)',
    description: 'The simplest baseline — signal cycles never change, regardless of traffic conditions.',
    complexity: 'O(1)',
    paper: 'Traditional traffic engineering',
    formula: 'No optimization — cycle fixed at boot time',
    steps: [
      {
        icon: 'clock',
        title: 'Trigger',
        subtitle: 'optimizeSignals() invoked',
        detail: 'Called automatically every 30 simulation seconds by the scheduler. The controller checks which mode is active.',
        code: 'public void optimizeSignals() {\n    switch (mode) {\n        case FIXED_TIME:\n            // entry here\n    }\n}',
        highlight: '30s interval',
        visual: (color) => (
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
            <div style={{ flex: 1, minWidth: 180 }}>
              <MiniIntersection greenDir="EW" vehicles={{ N: 1, S: 2, E: 1, W: 1 }} scale={0.85} />
            </div>
            <div style={{ flex: 1, minWidth: 180 }}>
              <SignalTimingBar color={color} />
            </div>
          </div>
        ),
      },
      {
        icon: 'branch',
        title: 'Match Mode',
        subtitle: 'case FIXED_TIME matched',
        detail: 'The switch statement routes to the FIXED_TIME branch. This mode has a dedicated case but no logic.',
        code: 'case FIXED_TIME:\n    // 固定时长模式，不做优化\n    break;',
        highlight: 'No-op branch',
        visual: (color) => <BranchView selectedIdx={0} color={color} />,
      },
      {
        icon: 'stop',
        title: 'Skip Optimization',
        subtitle: 'break — no changes',
        detail: 'No sensors are read, no math is computed, no state updates. The function simply exits.',
        code: '// Nothing to compute\n// No data collection\n// No state changes\nreturn;',
        highlight: 'Zero CPU cost',
        visual: (color) => (
          <div style={{ background: '#fff', borderRadius: 10, padding: '32px 20px', textAlign: 'center', border: `2px dashed ${color}`, opacity: 0.85 }}>
            <Icon name="stop" size={48} color={color} />
            <div style={{ marginTop: 10, fontSize: 13, color: '#555', fontWeight: 600 }}>No data read • No computation • No update</div>
          </div>
        ),
      },
      {
        icon: 'signal',
        title: 'Fixed Cycle Continues',
        subtitle: 'All intersections use initial timing',
        detail: 'Every node in the network keeps its pre-configured cycle forever. Green=20s, Yellow=3s, Red=23s per phase.',
        code: '// At boot time for each node:\nfor (Node n : intersectionNodes) {\n    n.getTrafficLight()\n        .setGreenDuration(20);\n}',
        highlight: '20s green everywhere',
        visual: (color) => (
          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1 }}><MiniIntersection greenDir="EW" vehicles={{ N: 3, S: 2, E: 0, W: 1 }} scale={0.75} /></div>
            <div style={{ flex: 1 }}><MiniIntersection greenDir="NS" vehicles={{ N: 0, S: 1, E: 2, W: 3 }} scale={0.75} /></div>
            <div style={{ flex: 1 }}><MiniIntersection greenDir="EW" vehicles={{ N: 2, S: 0, E: 1, W: 1 }} scale={0.75} /></div>
          </div>
        ),
      },
      {
        icon: 'infinity',
        title: 'Forever',
        subtitle: 'Cycle never changes',
        detail: 'Same timing for the entire simulation. Works as a reliable baseline, but can\'t respond to congestion or peak hours.',
        code: 'while (simulationRunning) {\n    light.update();      // tick down\n    // NO re-optimization\n    // cycle length unchanged\n}',
        highlight: 'Steady baseline',
        visual: (color) => (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
            {['t=0', 't=1h', 't=24h'].map((label, i) => (
              <div key={label} style={{ background: '#fff', borderRadius: 10, padding: 12, border: '1px solid #eaeaea', textAlign: 'center' }}>
                <div style={{ fontSize: 10, fontWeight: 700, color: '#86868B', letterSpacing: 1, marginBottom: 6 }}>{label}</div>
                <SignalTimingBar color={color} />
              </div>
            ))}
          </div>
        ),
      },
    ],
  },
  {
    key: 'adaptive',
    title: 'TRAFFIC ADAPTIVE',
    subtitle: 'Webster Formula Optimization',
    color: '#FF9F0A',
    gradientStart: '#FF9F0A',
    gradientEnd: '#cc7a00',
    softBg: 'rgba(255,159,10,0.06)',
    description: 'Mathematically computes optimal cycle time from real-time traffic demand using Webster\'s 1958 formula.',
    complexity: 'O(V) per optimization',
    paper: 'F.V. Webster, "Traffic Signal Settings" (1958)',
    formula: 'C₀ = (1.5L + 5) / (1 − Σyᵢ)',
    steps: [
      {
        icon: 'sensor',
        title: 'Collect Data',
        subtitle: 'Read live sensor readings',
        detail: 'At each intersection, count the waiting vehicles and currently active flows traversing the node.',
        code: 'int waiting = flowManager\n    .getWaitingFlowsAtNode(node);\nint active = countActiveFlows(node);',
        highlight: 'Live traffic data',
        visual: (color) => (
          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <MiniIntersection greenDir="EW" vehicles={{ N: 4, S: 3, E: 1, W: 2 }} highlight color={color} scale={0.9} />
            </div>
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 8, justifyContent: 'center' }}>
              <div style={{ background: '#fff', borderRadius: 10, padding: 12, border: '1px solid #eaeaea' }}>
                <div style={{ fontSize: 10, color: '#86868B', fontWeight: 700, letterSpacing: 0.5, marginBottom: 4 }}>WAITING</div>
                <div style={{ fontSize: 24, fontWeight: 800, color }}>10 <span style={{ fontSize: 12, color: '#86868B' }}>vehicles</span></div>
              </div>
              <div style={{ background: '#fff', borderRadius: 10, padding: 12, border: '1px solid #eaeaea' }}>
                <div style={{ fontSize: 10, color: '#86868B', fontWeight: 700, letterSpacing: 0.5, marginBottom: 4 }}>ACTIVE FLOWS</div>
                <div style={{ fontSize: 24, fontWeight: 800, color }}>4 <span style={{ fontSize: 12, color: '#86868B' }}>flows</span></div>
              </div>
            </div>
          </div>
        ),
      },
      {
        icon: 'chart',
        title: 'Compute Flow Ratios',
        subtitle: 'y = demand / saturation',
        detail: 'Saturation flow is 0.5 car/s (1800/hour). Current demand / saturation gives the traffic pressure for each phase.',
        code: 'double saturation = 0.5;\ndouble demand = (waiting+active)/60;\ndouble y1 = demand / saturation;\ndouble y2 = y1 * 0.7;',
        highlight: 'Two-phase model',
        visual: (color) => <FlowRatioChart y1={0.47} y2={0.33} color={color} />,
      },
      {
        icon: 'formula',
        title: 'Apply Webster Formula',
        subtitle: 'Derive optimal cycle',
        detail: 'Webster\'s formula from 1958 gives the minimum-delay cycle length given flow ratios and lost time.',
        code: 'double L = 4.0 + allRed * 2;\ndouble C = (1.5 * L + 5)\n    / (1 - (y1 + y2));',
        highlight: 'Math-derived',
        visual: (color) => <WebsterFormula color={color} />,
      },
      {
        icon: 'shield',
        title: 'Clamp to Safe Range',
        subtitle: '40s ≤ C ≤ 180s',
        detail: 'Cycles shorter than 40s waste time on transitions; longer than 180s frustrate drivers.',
        code: 'C = Math.max(40,\n        Math.min(180, C));',
        highlight: 'Safety bounds',
        visual: (color) => <RangeIndicator min={40} max={180} value={85} color={color} />,
      },
      {
        icon: 'split',
        title: 'Allocate Green Time',
        subtitle: 'Proportional to flow',
        detail: 'Busier direction gets proportionally more green time — the heart of traffic-responsive control.',
        code: 'double effective = C - L;\nint green = (int) Math.round(\n    effective * y1 / (y1 + y2));',
        highlight: 'Proportional split',
        visual: (color) => (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <div>
              <div style={{ fontSize: 10, fontWeight: 700, color: '#86868B', marginBottom: 6, letterSpacing: 0.5 }}>BEFORE</div>
              <SignalTimingBar green={20} yellow={3} red={23} color={color} />
            </div>
            <div>
              <div style={{ fontSize: 10, fontWeight: 700, color, marginBottom: 6, letterSpacing: 0.5 }}>AFTER (adaptive)</div>
              <SignalTimingBar green={48} yellow={3} red={34} color={color} />
            </div>
          </div>
        ),
      },
      {
        icon: 'apply',
        title: 'Apply to Signal',
        subtitle: 'Push new timing to intersection',
        detail: 'The new green duration is applied immediately. Effect takes hold at the next phase change.',
        code: 'light.adjustGreenDuration(green);',
        highlight: 'Instant push',
        visual: (color) => (
          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1 }}><MiniIntersection greenDir="EW" vehicles={{ N: 3, S: 2, E: 1, W: 1 }} highlight color={color} scale={0.85} /></div>
            <div style={{ flex: 1 }}>
              <SignalTimingBar green={48} yellow={3} red={34} label="Updated Cycle" color={color} />
            </div>
          </div>
        ),
      },
      {
        icon: 'sparkle',
        title: 'Adapted',
        subtitle: 'Cycle matches demand',
        detail: 'High-demand corridors get long green phases, quiet streets get short ones. Re-optimizes every 30 seconds.',
        code: '// Next iteration at t+30s:\n// - remeasure demand\n// - recompute Webster cycle\n// - adjust if traffic changed',
        highlight: 'Dynamic equilibrium',
        visual: (color) => (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
            <div style={{ background: '#fff', borderRadius: 10, padding: 10, border: '1px solid #eaeaea' }}>
              <div style={{ fontSize: 9, color: '#86868B', fontWeight: 700, marginBottom: 4 }}>LIGHT</div>
              <SignalTimingBar green={22} yellow={3} red={35} color={color} />
            </div>
            <div style={{ background: '#fff', borderRadius: 10, padding: 10, border: `2px solid ${color}` }}>
              <div style={{ fontSize: 9, color, fontWeight: 700, marginBottom: 4 }}>MEDIUM</div>
              <SignalTimingBar green={48} yellow={3} red={34} color={color} />
            </div>
            <div style={{ background: '#fff', borderRadius: 10, padding: 10, border: '1px solid #eaeaea' }}>
              <div style={{ fontSize: 9, color: '#86868B', fontWeight: 700, marginBottom: 4 }}>HEAVY</div>
              <SignalTimingBar green={90} yellow={3} red={50} color={color} />
            </div>
          </div>
        ),
      },
    ],
  },
  {
    key: 'greenwave',
    title: 'GREEN WAVE',
    subtitle: 'Corridor-Coordinated Offsets',
    color: '#30D158',
    gradientStart: '#30D158',
    gradientEnd: '#1a8a33',
    softBg: 'rgba(48,209,88,0.06)',
    description: 'Synchronize a chain of intersections so a vehicle traveling at the design speed never stops for a red.',
    complexity: 'O(V) once at setup, then free',
    paper: 'Little, "Synchronization of Traffic Signals" (1966)',
    formula: 'offset_i = (cumulative_distance_i / design_speed) mod cycle',
    steps: [
      {
        icon: 'eye',
        title: 'Identify Corridor',
        subtitle: 'Group intersections by axis',
        detail: 'Signals sharing the same Y-coordinate (EW axis) form a corridor. Only corridors with 3+ lights are worth coordinating.',
        code: 'corridors = groupBy(nodes,\n    n => round(n.y / 0.5));\n// keep only size >= 3',
        highlight: 'Y-axis clustering',
        visual: (color) => (
          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <MiniIntersection greenDir="EW" vehicles={{ N: 0, S: 0, E: 4, W: 4 }} highlight color={color} scale={0.85} />
            </div>
            <div style={{ flex: 1, display: 'flex', alignItems: 'center' }}>
              <StateBuckets activeIdx={0} color={color} />
            </div>
          </div>
        ),
      },
      {
        icon: 'target',
        title: 'Pick Design Speed',
        subtitle: 'Choose coordinated velocity',
        detail: 'All cars traveling at this speed along the corridor should catch every green. Typical urban value: 40 km/h (sim applies 2× slowdown).',
        code: 'designSpeed = 40 km/h\neffective = designSpeed / 2\n            = 20 km/h',
        highlight: 'v = 20 km/h (sim)',
        visual: (color) => <SignalTimingBar green={35} yellow={3} red={22} label="35s EW / 15s NS / 60s cycle" color={color} />,
      },
      {
        icon: 'bolt',
        title: 'Compute Offsets',
        subtitle: 'How long to reach each light',
        detail: 'For each intersection i in the corridor, offset_i = (cumulative distance from the first light) / design_speed. Gives seconds-until-green-starts.',
        code: 'for (node : corridor) {\n  cumKm += delta(node.x);\n  travelSec =\n      cumKm * 3600 / vKmh;\n  offset_i = travelSec % cycle;\n}',
        highlight: 'Cumulative travel time',
        visual: (color) => <ActionSpace chosenIdx={2} color={color} />,
      },
      {
        icon: 'signal',
        title: 'Synchronize Phases',
        subtitle: 'Align cycle start per light',
        detail: 'At sim time 0, light i should be at phase (cycle - offset_i) mod cycle. Then at time t=offset_i, it hits EW green start exactly as the car arrives.',
        code: 'phaseAtT0 =\n    (cycle - offset_i) % cycle;\nlight.synchronize(\n    phaseAtT0);',
        highlight: 'One-time setup',
        visual: (color) => (
          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1 }}><MiniIntersection greenDir="EW" vehicles={{ N: 0, S: 0, E: 4, W: 3 }} highlight color={color} scale={0.85} /></div>
            <div style={{ flex: 1 }}><SignalTimingBar green={35} yellow={3} red={22} label="Phase-aligned" color={color} /></div>
          </div>
        ),
      },
      {
        icon: 'trending',
        title: 'Cars Ride the Wave',
        subtitle: 'Continuous flow without stops',
        detail: 'Vehicles at design speed arrive at each signal exactly as it turns green. No start-stop losses on the major corridor.',
        code: '// car leaves light i at t=0\n// arrives light i+1 at t=offset_{i+1}\n// → EW green just starting\n// → passes without braking',
        highlight: 'Zero stops along corridor',
        visual: (color) => <LearningCurve color={color} />,
      },
    ],
  },
];

// ==================== MAIN COMPONENT ====================

const AUTO_PLAY_MS = 3200;

const ModeFlowDiagrams = ({ onBack }) => {
  const [modeIndex, setModeIndex] = useState(0);
  const [stepIndex, setStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(true);
  const timerRef = useRef(null);

  const mode = MODES[modeIndex];
  const step = mode.steps[stepIndex];
  const totalSteps = mode.steps.length;

  useEffect(() => {
    if (!isPlaying) return;
    timerRef.current = setTimeout(() => {
      setStepIndex(prev => (prev + 1) % totalSteps);
    }, AUTO_PLAY_MS);
    return () => clearTimeout(timerRef.current);
  }, [isPlaying, stepIndex, totalSteps]);

  const switchMode = (idx) => { setModeIndex(idx); setStepIndex(0); };
  const togglePlay = () => setIsPlaying(p => !p);
  const next = () => { setStepIndex(prev => (prev + 1) % totalSteps); setIsPlaying(false); };
  const prev = () => { setStepIndex(prev => (prev - 1 + totalSteps) % totalSteps); setIsPlaying(false); };
  const restart = () => { setStepIndex(0); setIsPlaying(true); };

  return (
    <div style={{ ...styles.container, background: mode.softBg, transition: 'background 0.8s' }}>
      <style>{animationCSS}</style>

      <div style={styles.header}>
        <button onClick={onBack} style={styles.backBtn}>← Back</button>
        <div>
          <h2 style={styles.title}>Algorithm Walkthrough</h2>
          <div style={styles.subtitle}>How each mode interacts with the road network, step by step</div>
        </div>
      </div>

      <div style={styles.modeTabs}>
        {MODES.map((m, i) => (
          <button
            key={m.key}
            onClick={() => switchMode(i)}
            style={{
              ...styles.modeTab,
              background: modeIndex === i
                ? `linear-gradient(135deg, ${m.gradientStart}, ${m.gradientEnd})`
                : '#fff',
              color: modeIndex === i ? '#fff' : '#1D1D1F',
              boxShadow: modeIndex === i ? `0 10px 30px ${m.color}55` : '0 1px 4px rgba(0,0,0,0.06)',
              transform: modeIndex === i ? 'translateY(-3px)' : 'none',
              border: modeIndex === i ? 'none' : '1px solid rgba(0,0,0,0.08)',
            }}
          >
            <div style={styles.modeTabTitle}>{m.title}</div>
            <div style={styles.modeTabSub}>{m.subtitle}</div>
          </button>
        ))}
      </div>

      <div style={styles.stage}>
        {/* Left timeline */}
        <div style={styles.timeline}>
          <div style={styles.timelineHeader}>
            <div style={styles.timelineModeTitle}>{mode.title}</div>
            <div style={styles.timelineModeSub}>{totalSteps} steps</div>
          </div>
          <div style={styles.timelineRail}>
            <div
              style={{
                ...styles.railFill,
                height: `${(stepIndex / Math.max(totalSteps - 1, 1)) * 100}%`,
                background: `linear-gradient(180deg, ${mode.gradientStart}, ${mode.gradientEnd})`,
              }}
            />
            {mode.steps.map((s, i) => {
              const isActive = i === stepIndex;
              const isPast = i < stepIndex;
              return (
                <button
                  key={i}
                  onClick={() => { setStepIndex(i); setIsPlaying(false); }}
                  style={{
                    ...styles.timelineStep,
                    opacity: isActive ? 1 : (isPast ? 0.65 : 0.35),
                  }}
                >
                  <div
                    style={{
                      ...styles.timelineDot,
                      background: (isActive || isPast)
                        ? `linear-gradient(135deg, ${mode.gradientStart}, ${mode.gradientEnd})`
                        : '#e5e5ea',
                      color: (isActive || isPast) ? '#fff' : '#86868B',
                      transform: isActive ? 'scale(1.25)' : 'scale(1)',
                      boxShadow: isActive ? `0 0 0 6px ${mode.color}33, 0 4px 14px ${mode.color}55` : 'none',
                    }}
                  >
                    {isPast ? '✓' : i + 1}
                  </div>
                  <div style={styles.timelineMeta}>
                    <div style={{
                      ...styles.timelineStepTitle,
                      fontWeight: isActive ? 700 : 500,
                      color: isActive ? mode.color : '#1D1D1F',
                    }}>{s.title}</div>
                    <div style={styles.timelineStepSub}>{s.subtitle}</div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Right stage */}
        <div style={styles.stageRight}>
          <div style={{
            ...styles.banner,
            background: `linear-gradient(135deg, ${mode.gradientStart}, ${mode.gradientEnd})`,
          }}>
            <div>
              <div style={styles.bannerTitle}>{mode.title}</div>
              <div style={styles.bannerSub}>{mode.subtitle}</div>
            </div>
            <div style={styles.bannerRight}>
              <div style={styles.bannerMeta}>
                <div style={styles.metaLabel}>COMPLEXITY</div>
                <div style={styles.metaValue}>{mode.complexity}</div>
              </div>
              <div style={styles.bannerMeta}>
                <div style={styles.metaLabel}>REFERENCE</div>
                <div style={styles.metaValue}>{mode.paper}</div>
              </div>
            </div>
          </div>

          <div style={{ ...styles.formulaBar, borderColor: mode.color }}>
            <span style={styles.formulaLabel}>FORMULA</span>
            <code style={{ ...styles.formulaCode, color: mode.color }}>{mode.formula}</code>
          </div>

          <div
            key={`${mode.key}-${stepIndex}`}
            style={{
              ...styles.stepCard,
              borderTop: `4px solid ${mode.color}`,
              animation: 'stepFadeIn 0.55s cubic-bezier(0.16, 1, 0.3, 1)',
            }}
          >
            <div style={styles.stepHeaderRow}>
              <div style={styles.stepNumber}>
                STEP <span style={{ color: mode.color, fontSize: 22, fontWeight: 800 }}>{stepIndex + 1}</span>
                <span style={{ color: '#86868B' }}> / {totalSteps}</span>
              </div>
              <div style={{ ...styles.stepBadge, background: mode.color }}>
                {step.highlight}
              </div>
            </div>

            <div style={styles.stepMain}>
              <div style={{
                ...styles.stepIcon,
                background: `linear-gradient(135deg, ${mode.gradientStart}, ${mode.gradientEnd})`,
                animation: 'iconBounceIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1)',
              }}>
                <Icon name={step.icon} size={32} color="#fff" />
              </div>
              <div style={styles.stepText}>
                <div style={styles.stepTitle}>{step.title}</div>
                <div style={styles.stepSubtitle}>{step.subtitle}</div>
                <div style={styles.stepDetail}>{step.detail}</div>
              </div>
            </div>

            {/* Visualization: map + data combined */}
            {step.visual && (
              <div style={styles.visualBox}>
                {step.visual(mode.color)}
              </div>
            )}

            <div style={styles.codeBlock}>
              <div style={styles.codeHeader}>
                <span style={styles.codeDot(0)} />
                <span style={styles.codeDot(1)} />
                <span style={styles.codeDot(2)} />
                <span style={styles.codeLabel}>algorithm.java</span>
              </div>
              <pre style={styles.codePre}>{step.code}</pre>
            </div>

            {stepIndex < totalSteps - 1 && (
              <div style={styles.flowArrow}>
                <div style={{ ...styles.flowDot, background: mode.color, animationDelay: '0s' }} />
                <div style={{ ...styles.flowDot, background: mode.color, animationDelay: '0.2s' }} />
                <div style={{ ...styles.flowDot, background: mode.color, animationDelay: '0.4s' }} />
                <div style={{ color: mode.color, fontSize: 13, fontWeight: 600, marginLeft: 8 }}>
                  Next: {mode.steps[stepIndex + 1].title}
                </div>
              </div>
            )}
          </div>

          <div style={styles.controls}>
            <button onClick={prev} style={styles.ctrlBtn} title="Previous">←</button>
            <button
              onClick={togglePlay}
              style={{
                ...styles.playBtn,
                background: `linear-gradient(135deg, ${mode.gradientStart}, ${mode.gradientEnd})`,
              }}
              title={isPlaying ? 'Pause' : 'Play'}
            >
              {isPlaying ? 'Pause' : 'Play'}
            </button>
            <button onClick={next} style={styles.ctrlBtn} title="Next">→</button>
            <button onClick={restart} style={styles.ctrlBtn} title="Restart">↻</button>

            <div style={styles.progressBox}>
              <div style={styles.progressBar}>
                <div style={{
                  ...styles.progressFill,
                  width: `${((stepIndex + 1) / totalSteps) * 100}%`,
                  background: `linear-gradient(90deg, ${mode.gradientStart}, ${mode.gradientEnd})`,
                }} />
              </div>
              <div style={styles.progressText}>
                {stepIndex + 1} / {totalSteps} · {isPlaying ? 'playing' : 'paused'}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const animationCSS = `
  @keyframes stepFadeIn {
    from { opacity: 0; transform: translateY(16px) scale(0.98); }
    to   { opacity: 1; transform: translateY(0) scale(1); }
  }
  @keyframes iconBounceIn {
    0%   { transform: scale(0) rotate(-90deg); opacity: 0; }
    60%  { transform: scale(1.1) rotate(10deg); opacity: 1; }
    100% { transform: scale(1) rotate(0); opacity: 1; }
  }
  @keyframes flowDotPulse {
    0%, 100% { transform: scale(0.6); opacity: 0.4; }
    50%      { transform: scale(1.3); opacity: 1; }
  }
`;

const styles = {
  container: { padding: '24px', minHeight: 'calc(100vh - 70px)', maxWidth: 1400, margin: '0 auto' },
  header: { display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24 },
  backBtn: { background: '#86868B', color: '#fff', border: 'none', borderRadius: 8, padding: '8px 18px', fontSize: 13, fontWeight: 600, cursor: 'pointer' },
  title: { fontSize: 24, fontWeight: 700, color: '#1D1D1F', margin: 0 },
  subtitle: { fontSize: 13, color: '#86868B', marginTop: 2 },
  modeTabs: { display: 'flex', gap: 12, marginBottom: 24 },
  modeTab: { flex: 1, padding: '16px 20px', borderRadius: 14, cursor: 'pointer', transition: 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)', textAlign: 'left', border: 'none' },
  modeTabTitle: { fontSize: 15, fontWeight: 800, letterSpacing: 0.3 },
  modeTabSub: { fontSize: 11, opacity: 0.85, marginTop: 3 },
  stage: { display: 'grid', gridTemplateColumns: '260px 1fr', gap: 20, alignItems: 'start' },
  timeline: { background: '#fff', borderRadius: 16, padding: '18px 14px', boxShadow: '0 2px 12px rgba(0,0,0,0.04)', position: 'sticky', top: 20 },
  timelineHeader: { marginBottom: 14, paddingBottom: 10, borderBottom: '1px solid #f0f0f0' },
  timelineModeTitle: { fontSize: 12, fontWeight: 700, color: '#1D1D1F', letterSpacing: 0.5 },
  timelineModeSub: { fontSize: 10, color: '#86868B', marginTop: 2 },
  timelineRail: { position: 'relative', paddingLeft: 4 },
  railFill: { position: 'absolute', left: 15, top: 14, width: 2, borderRadius: 2, transition: 'height 0.6s cubic-bezier(0.16, 1, 0.3, 1)', zIndex: 0 },
  timelineStep: { display: 'flex', alignItems: 'center', gap: 10, padding: '7px 4px', cursor: 'pointer', background: 'none', border: 'none', width: '100%', textAlign: 'left', position: 'relative', zIndex: 1, transition: 'opacity 0.3s' },
  timelineDot: { width: 26, height: 26, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: 11, flexShrink: 0, transition: 'all 0.4s cubic-bezier(0.16, 1, 0.3, 1)' },
  timelineMeta: { flex: 1, minWidth: 0 },
  timelineStepTitle: { fontSize: 12, transition: 'all 0.3s' },
  timelineStepSub: { fontSize: 10, color: '#86868B', marginTop: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  stageRight: { display: 'flex', flexDirection: 'column', gap: 14 },
  banner: { borderRadius: 14, padding: '18px 22px', color: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 18, boxShadow: '0 8px 24px rgba(0,0,0,0.1)' },
  bannerTitle: { fontSize: 20, fontWeight: 800, letterSpacing: 0.5 },
  bannerSub: { fontSize: 12, opacity: 0.9, marginTop: 2 },
  bannerRight: { display: 'flex', gap: 22 },
  bannerMeta: { textAlign: 'right' },
  metaLabel: { fontSize: 9, opacity: 0.75, letterSpacing: 1, fontWeight: 600 },
  metaValue: { fontSize: 12, fontWeight: 600, marginTop: 3 },
  formulaBar: { background: '#fff', borderLeft: '4px solid', borderRadius: 10, padding: '10px 14px', display: 'flex', alignItems: 'center', gap: 12, boxShadow: '0 1px 4px rgba(0,0,0,0.04)' },
  formulaLabel: { fontSize: 9, fontWeight: 700, color: '#86868B', letterSpacing: 1 },
  formulaCode: { fontFamily: 'SF Mono, Menlo, monospace', fontSize: 14, fontWeight: 600 },
  stepCard: { background: '#fff', borderRadius: 16, padding: '24px 28px', boxShadow: '0 4px 20px rgba(0,0,0,0.06)', position: 'relative' },
  stepHeaderRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 },
  stepNumber: { fontSize: 11, fontWeight: 700, letterSpacing: 2, color: '#86868B' },
  stepBadge: { color: '#fff', padding: '5px 12px', borderRadius: 20, fontSize: 10, fontWeight: 700, letterSpacing: 0.5 },
  stepMain: { display: 'flex', gap: 18, marginBottom: 18 },
  stepIcon: { width: 62, height: 62, borderRadius: 16, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, boxShadow: '0 8px 24px rgba(0,0,0,0.15)' },
  stepText: { flex: 1 },
  stepTitle: { fontSize: 24, fontWeight: 800, color: '#1D1D1F', letterSpacing: -0.4, lineHeight: 1.15 },
  stepSubtitle: { fontSize: 14, color: '#555', marginTop: 3, marginBottom: 8 },
  stepDetail: { fontSize: 13, color: '#555', lineHeight: 1.55 },
  visualBox: { marginBottom: 14, padding: 14, background: '#fafbfc', borderRadius: 12 },
  codeBlock: { background: '#1d1d1f', borderRadius: 10, overflow: 'hidden', marginBottom: 14 },
  codeHeader: { background: '#2a2a2d', padding: '7px 12px', display: 'flex', alignItems: 'center', gap: 6 },
  codeDot: (idx) => ({ display: 'inline-block', width: 9, height: 9, borderRadius: '50%', background: ['#ff5f57', '#ffbd2e', '#28c940'][idx] }),
  codeLabel: { color: '#888', fontSize: 10, marginLeft: 10, fontFamily: 'SF Mono, Menlo, monospace' },
  codePre: { margin: 0, padding: '12px 14px', color: '#e8e8e8', fontFamily: 'SF Mono, Menlo, monospace', fontSize: 12, lineHeight: 1.55, overflowX: 'auto' },
  flowArrow: { display: 'flex', alignItems: 'center', gap: 6, padding: '8px 12px', background: '#fafbfc', borderRadius: 8 },
  flowDot: { width: 7, height: 7, borderRadius: '50%', animation: 'flowDotPulse 1.4s ease-in-out infinite' },
  controls: { display: 'flex', alignItems: 'center', gap: 10, background: '#fff', borderRadius: 14, padding: '12px 14px', boxShadow: '0 2px 12px rgba(0,0,0,0.05)' },
  ctrlBtn: { width: 40, height: 40, borderRadius: 10, background: '#f0f0f2', color: '#1D1D1F', border: 'none', cursor: 'pointer', fontSize: 16, fontWeight: 700, transition: 'all 0.2s' },
  playBtn: { padding: '0 22px', height: 40, borderRadius: 10, color: '#fff', border: 'none', cursor: 'pointer', fontSize: 13, fontWeight: 700, letterSpacing: 0.3, transition: 'all 0.2s' },
  progressBox: { flex: 1, marginLeft: 14 },
  progressBar: { height: 6, background: '#f0f0f2', borderRadius: 3, overflow: 'hidden' },
  progressFill: { height: '100%', borderRadius: 3, transition: 'width 0.8s cubic-bezier(0.16, 1, 0.3, 1)' },
  progressText: { fontSize: 11, color: '#86868B', marginTop: 5, fontWeight: 500 },
};

export default ModeFlowDiagrams;
