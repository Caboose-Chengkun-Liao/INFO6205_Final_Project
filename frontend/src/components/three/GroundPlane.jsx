import React, { useMemo, useEffect } from 'react';
import { Sky } from '@react-three/drei';
import * as THREE from 'three';

// ─── geographic feature data in graph-space coordinates ───────────────────────
// Derived from boundary/intersection node positions + real Arlington geography.

/** Potomac River — runs NE→SE along Arlington's eastern border */
const POTOMAC = [
  // Virginia shoreline (Key Bridge area northward, off-map)
  [3.6, 6.5], [5.0, 6.5], [5.8, 7.2],
  // off-map NE corner
  [12, 8], [12, -11],
  // back along the Virginia shoreline south→north
  [9.5, -9.3],            // Crystal City South (bS3)
  [9.6, -7.0],
  [9.8, -5.0],            // near 14th St Bridge (bE3)
  [9.6, -3.2],
  [9.2, -1.2],            // near Arlington Ridge Rd (bE5)
  [9.1, 0.6],             // near Memorial Bridge (bE2)
  [8.4, 2.2],
  [7.4, 4.0],
  [6.2, 5.4],
  [5.2, 6.1],             // near GW Pkwy North (bE4)
  [4.3, 6.4],             // Key Bridge (bE1)
];

/** Four Mile Run creek — flows SW→SE through southern Arlington */
const FOUR_MILE_RUN_CL = [   // centre-line points
  [-6.0, -3.5],
  [-4.8, -4.2],
  [-3.5, -5.0],
  [-2.2, -5.8],
  [-1.0, -6.3],
  [ 0.2, -6.8],
  [ 0.9, -7.6],   // Shirlington Circle (n62)
  [ 1.4, -8.6],
  [ 2.5, -9.3],   // empties into Potomac south of Pentagon
];

/** Arlington National Cemetery — large green space between Rosslyn & Pentagon */
const ARLINGTON_CEMETERY = [
  [3.4, 4.1], [4.4, 4.5], [5.5, 4.3], [6.8, 3.5],
  [7.4, 2.0], [7.5, 0.5], [7.0, -0.8], [5.8, -1.5],
  [4.0, -1.3], [3.3, 0.0], [3.2, 2.0],
];

/** Pentagon building — regular pentagon shape */
const pentagonPoly = () => {
  const cx = 8.35, cy = -2.7, r = 0.85;
  return Array.from({ length: 5 }, (_, i) => {
    const a = (i * 2 * Math.PI / 5) - Math.PI / 2;
    return [cx + r * Math.cos(a), cy + r * Math.sin(a)];
  });
};

/** Bluemont Park — NW Arlington */
const BLUEMONT_PARK = [
  [-5.6, 0.6], [-4.0, 0.6], [-3.8, 1.2],
  [-4.0, 2.6], [-5.4, 2.8], [-5.8, 1.8],
];

/** Quincy Park — Clarendon north */
const QUINCY_PARK = [
  [-1.8, 2.8], [ 0.4, 2.8], [ 0.6, 4.2],
  [-0.4, 4.6], [-1.6, 4.2],
];

/** Long Branch Nature Center — south Arlington */
const LONG_BRANCH = [
  [-2.6, -4.2], [-0.8, -4.2], [-0.6, -5.4],
  [-1.4, -5.8], [-2.8, -5.6],
];

/** Shirlington Park — south of Shirlington Circle */
const SHIRLINGTON_PARK = [
  [-0.6, -7.2], [ 2.2, -7.0], [ 2.4, -8.2],
  [ 0.6, -8.6], [-0.8, -8.2],
];

/** Reagan National Airport — far south */
const REAGAN_AIRPORT = [
  [2.8, -9.5], [7.0, -9.5], [7.2, -7.5],
  [5.5, -6.8], [3.0, -7.2],
];

/** Rosslyn waterfront plaza — small urban park near Key Bridge */
const ROSSLYN_PLAZA = [
  [4.0, 4.8], [5.0, 4.8], [5.2, 5.4],
  [4.6, 5.8], [3.8, 5.4],
];

/** Barcroft Park — southwest along Four Mile Run */
const BARCROFT_PARK = [
  [-6.2, -2.6], [-4.6, -2.4], [-4.4, -3.8],
  [-5.0, -4.4], [-6.4, -3.8],
];

// ─── canvas builder ────────────────────────────────────────────────────────────

function drawCompassRose(ctx, cx, cy, r) {
  // Background disc
  ctx.save();
  ctx.beginPath();
  ctx.arc(cx, cy, r + 5, 0, Math.PI * 2);
  ctx.fillStyle = 'rgba(255,255,255,0.86)';
  ctx.fill();
  ctx.strokeStyle = 'rgba(0,0,0,0.14)';
  ctx.lineWidth = 1;
  ctx.stroke();
  ctx.restore();

  const arrow = (angle, len, color) => {
    const tx = cx + Math.cos(angle) * len;
    const ty = cy + Math.sin(angle) * len;
    const lx = cx + Math.cos(angle + Math.PI / 2) * r * 0.22;
    const ly = cy + Math.sin(angle + Math.PI / 2) * r * 0.22;
    const rx = cx + Math.cos(angle - Math.PI / 2) * r * 0.22;
    const ry = cy + Math.sin(angle - Math.PI / 2) * r * 0.22;
    ctx.beginPath();
    ctx.moveTo(tx, ty);
    ctx.lineTo(lx, ly);
    ctx.lineTo(cx, cy);
    ctx.lineTo(rx, ry);
    ctx.closePath();
    ctx.fillStyle = color;
    ctx.fill();
  };

  // N red, others gray
  arrow(-Math.PI / 2, r * 0.74, '#d42b2b');
  arrow( Math.PI / 2, r * 0.74, '#aaaaaa');
  arrow( 0,           r * 0.74, '#aaaaaa');
  arrow( Math.PI,     r * 0.74, '#aaaaaa');

  // Centre dot
  ctx.beginPath();
  ctx.arc(cx, cy, r * 0.13, 0, Math.PI * 2);
  ctx.fillStyle = '#ffffff';
  ctx.fill();
  ctx.strokeStyle = '#888';
  ctx.lineWidth = 0.8;
  ctx.stroke();

  // Cardinal labels
  const dirs = [
    { angle: -Math.PI / 2, label: 'N', major: true  },
    { angle:  Math.PI / 2, label: 'S', major: false },
    { angle:  0,           label: 'E', major: false },
    { angle:  Math.PI,     label: 'W', major: false },
  ];
  const ld = r + 14;
  dirs.forEach(({ angle, label, major }) => {
    ctx.save();
    ctx.font      = `bold ${major ? 15 : 11}px Arial`;
    ctx.fillStyle = major ? '#c01010' : '#444';
    ctx.textAlign     = 'center';
    ctx.textBaseline  = 'middle';
    ctx.fillText(label, cx + Math.cos(angle) * ld, cy + Math.sin(angle) * ld);
    ctx.restore();
  });
}

function buildMapTexture(coordBounds, width, depth) {
  const PX   = 110;
  const W    = Math.round(width * PX);
  const H    = Math.round(depth * PX);
  const canvas = document.createElement('canvas');
  canvas.width = W; canvas.height = H;
  const ctx = canvas.getContext('2d');
  const { minX, maxX, minY, maxY } = coordBounds;

  /** graph-space → canvas pixels */
  const toC = (x, y) => [
    ((x - minX) / (maxX - minX)) * W,
    H - ((y - minY) / (maxY - minY)) * H,
  ];

  const fillPoly = (pts, fill, stroke, lw = 0) => {
    if (!pts.length) return;
    ctx.beginPath();
    ctx.moveTo(...toC(...pts[0]));
    for (let i = 1; i < pts.length; i++) ctx.lineTo(...toC(...pts[i]));
    ctx.closePath();
    if (fill)   { ctx.fillStyle = fill; ctx.fill(); }
    if (stroke) { ctx.strokeStyle = stroke; ctx.lineWidth = lw; ctx.stroke(); }
  };

  /** Draws a wide "river / creek" stroke from a centre-line */
  const drawCreek = (pts, color, worldWidth) => {
    const pw = worldWidth * PX;
    ctx.save();
    ctx.beginPath();
    ctx.moveTo(...toC(...pts[0]));
    for (let i = 1; i < pts.length; i++) ctx.lineTo(...toC(...pts[i]));
    ctx.strokeStyle = color;
    ctx.lineWidth   = pw;
    ctx.lineCap     = 'round';
    ctx.lineJoin    = 'round';
    ctx.stroke();
    ctx.restore();
  };

  // ── 1. Urban base ───────────────────────────────────────────────
  ctx.fillStyle = '#ece6d8';
  ctx.fillRect(0, 0, W, H);

  // Subtle noise-like texture (avoids flat/boring look)
  for (let i = 0; i < 6000; i++) {
    const rx = Math.random() * W;
    const ry = Math.random() * H;
    ctx.fillStyle = `rgba(0,0,0,${Math.random() * 0.025})`;
    ctx.fillRect(rx, ry, 2, 2);
  }

  // ── 2. Potomac River ────────────────────────────────────────────
  fillPoly(POTOMAC, '#7ab8d4', null);

  // Gentle wave pattern on river
  ctx.save();
  ctx.globalAlpha = 0.12;
  for (let y = 0; y < H; y += 18) {
    ctx.beginPath();
    for (let x = 0; x < W; x += 4) {
      const py = y + Math.sin(x * 0.06) * 5;
      x === 0 ? ctx.moveTo(x, py) : ctx.lineTo(x, py);
    }
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 1.5;
    ctx.stroke();
  }
  ctx.restore();

  // Potomac shoreline edge
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(...toC(...POTOMAC[0]));
  for (let i = 1; i < 8; i++) ctx.lineTo(...toC(...POTOMAC[i]));
  ctx.strokeStyle = '#5a9ab8';
  ctx.lineWidth = 3;
  ctx.stroke();
  ctx.restore();

  // ── 3. Four Mile Run ────────────────────────────────────────────
  drawCreek(FOUR_MILE_RUN_CL, '#7ab8d4', 0.32);          // main water
  drawCreek(FOUR_MILE_RUN_CL, 'rgba(255,255,255,0.35)', 0.12); // highlight

  // ── 4. Parks (draw largest first) ──────────────────────────────
  const PARK_FILL   = '#8fc478';
  const PARK_STROKE = '#70a860';

  // Arlington National Cemetery (largest)
  fillPoly(ARLINGTON_CEMETERY, '#98cc80', PARK_STROKE, 2);

  // Headstones texture inside cemetery
  ctx.save();
  ctx.globalAlpha = 0.18;
  const [cemX0, cemY0] = toC(3.4, 4.1);
  const [cemX1, cemY1] = toC(7.5, -1.5);
  for (let gy = Math.min(cemY0, cemY1); gy < Math.max(cemY0, cemY1); gy += 14) {
    for (let gx = Math.min(cemX0, cemX1); gx < Math.max(cemX0, cemX1); gx += 12) {
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(gx, gy, 3, 5);
    }
  }
  ctx.restore();

  fillPoly(BLUEMONT_PARK,   PARK_FILL, PARK_STROKE, 2);
  fillPoly(QUINCY_PARK,     PARK_FILL, PARK_STROKE, 2);
  fillPoly(LONG_BRANCH,     PARK_FILL, PARK_STROKE, 2);
  fillPoly(SHIRLINGTON_PARK,PARK_FILL, PARK_STROKE, 2);
  fillPoly(BARCROFT_PARK,   PARK_FILL, PARK_STROKE, 2);
  fillPoly(ROSSLYN_PLAZA,   '#a8d488', PARK_STROKE, 1.5);

  // Park tree dot texture
  const treePatch = (pts) => {
    const xs = pts.map(p => p[0]), ys = pts.map(p => p[1]);
    const bx0 = Math.min(...xs), bx1 = Math.max(...xs);
    const by0 = Math.min(...ys), by1 = Math.max(...ys);
    ctx.save();
    ctx.globalAlpha = 0.35;
    for (let i = 0; i < 80; i++) {
      const rx = bx0 + Math.random() * (bx1 - bx0);
      const ry = by0 + Math.random() * (by1 - by0);
      const [cx2, cy2] = toC(rx, ry);
      const r = 3 + Math.random() * 4;
      ctx.beginPath();
      ctx.arc(cx2, cy2, r, 0, Math.PI * 2);
      ctx.fillStyle = '#5a9444';
      ctx.fill();
    }
    ctx.restore();
  };
  [BLUEMONT_PARK, QUINCY_PARK, LONG_BRANCH, SHIRLINGTON_PARK, BARCROFT_PARK, ARLINGTON_CEMETERY].forEach(treePatch);

  // ── 5. Reagan National Airport ──────────────────────────────────
  fillPoly(REAGAN_AIRPORT, '#d8d2c4', '#c0bab0', 2);

  // Runway stripes
  ctx.save();
  ctx.globalAlpha = 0.6;
  const [apt0x, apt0y] = toC(3.5, -9.5);
  const [apt1x, apt1y] = toC(6.5, -7.2);
  ctx.fillStyle = '#b8b2a4';
  ctx.fillRect(Math.min(apt0x, apt1x), Math.min(apt0y, apt1y),
    Math.abs(apt1x - apt0x) * 0.15, Math.abs(apt1y - apt0y));
  ctx.fillRect(Math.min(apt0x, apt1x) + Math.abs(apt1x - apt0x) * 0.45,
    Math.min(apt0y, apt1y),
    Math.abs(apt1x - apt0x) * 0.15, Math.abs(apt1y - apt0y));
  ctx.restore();

  // ── 6. Pentagon building ────────────────────────────────────────
  fillPoly(pentagonPoly(), '#c0b8a4', '#a0988a', 3);

  // Pentagon inner ring
  const pInner = pentagonPoly().map(([x, y]) => {
    const cx = 8.35, cy = -2.7;
    const scale = 0.55;
    return [cx + (x - cx) * scale, cy + (y - cy) * scale];
  });
  fillPoly(pInner, '#d8d2c4', '#a0988a', 2);

  // Pentagon center courtyard
  const pCore = pentagonPoly().map(([x, y]) => {
    const cx = 8.35, cy = -2.7;
    const scale = 0.18;
    return [cx + (x - cx) * scale, cy + (y - cy) * scale];
  });
  fillPoly(pCore, '#98cc80', null);

  // ── 7. Potomac riverbank highlights ────────────────────────────
  ctx.save();
  ctx.globalAlpha = 0.5;
  const grad = ctx.createLinearGradient(...toC(3.5, 6.5), ...toC(10, 0));
  grad.addColorStop(0, '#7ab8d4');
  grad.addColorStop(1, 'rgba(122,184,212,0)');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, W, H);
  ctx.restore();

  // ── 8. Map border vignette ──────────────────────────────────────
  const vg = ctx.createRadialGradient(W/2, H/2, Math.min(W, H)*0.3, W/2, H/2, Math.max(W, H)*0.75);
  vg.addColorStop(0, 'rgba(0,0,0,0)');
  vg.addColorStop(1, 'rgba(0,0,0,0.18)');
  ctx.fillStyle = vg;
  ctx.fillRect(0, 0, W, H);

  // ── 9. Compass rose (bottom-left corner) ────────────────────────
  const roseR  = 26;
  const rosePad = 18;
  drawCompassRose(ctx, rosePad + roseR, H - rosePad - roseR, roseR);

  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  return texture;
}

// ─── component ────────────────────────────────────────────────────────────────

const GroundPlane = React.memo(({
  nightMode   = false,
  width       = 18.66,
  depth       = 16.19,
  coordBounds = null,
}) => {
  const mapTexture = useMemo(() => {
    if (!coordBounds) return null;
    return buildMapTexture(coordBounds, width, depth);
  }, [coordBounds, width, depth]);

  useEffect(() => () => { mapTexture?.dispose(); }, [mapTexture]);

  const gridColorMain = nightMode ? '#1a2332' : '#b8bec5';
  const gridColorSub  = nightMode ? '#111825' : '#cacfd5';

  return (
    <group>
      {mapTexture && (
        <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.02, 0]} receiveShadow>
          <planeGeometry args={[width, depth]} />
          <meshStandardMaterial
            map={mapTexture}
            transparent
            opacity={nightMode ? 0.28 : 0.9}
            toneMapped={false}
          />
        </mesh>
      )}

      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.03, 0]} receiveShadow>
        <planeGeometry args={[width + 20, depth + 20]} />
        <meshStandardMaterial color={nightMode ? '#0a0e17' : '#d8dde3'} />
      </mesh>

      <gridHelper
        args={[width + 20, Math.round((width + 20) * 2), gridColorMain, gridColorSub]}
        position={[0, -0.015, 0]}
      />

      {!nightMode && (
        <Sky
          distance={450000}
          sunPosition={[100, 50, 100]}
          inclination={0.6}
          azimuth={0.25}
          turbidity={8}
          rayleigh={0.5}
        />
      )}

      {nightMode
        ? <fog attach="fog" args={['#0a0e17', 15, 45]} />
        : <fog attach="fog" args={['#dde0e4', 28, 55]} />
      }
    </group>
  );
});

GroundPlane.displayName = 'GroundPlane';
export default GroundPlane;
