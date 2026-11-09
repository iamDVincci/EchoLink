/* ==========================================================================
   ECHOLINK INTERACTIVE RADAR SWEEP ENGINE
   ========================================================================== */

(function initRadarSweep() {
  const canvas = document.getElementById('heroRadarCanvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  let width = (canvas.width = 460);
  let height = (canvas.height = 460);
  let cx = width / 2;
  let cy = height / 2;
  let radius = width / 2 - 20;

  let angle = 0;
  const speed = 0.022; // Sweep rotational speed (radians/frame)

  // Tactical Mesh Peers
  const peers = [
    { id: 'ALPHA-LEAD', name: 'S23 Ultra [Host]', dist: 0.35, angle: 0.85, status: 'TX ACTIVE', color: '#00ff9d', pinged: false },
    { id: 'ECHO-TWO', name: 'iPhone 15 Pro', dist: 0.65, angle: 2.45, status: 'ENCRYPTED', color: '#00e5ff', pinged: false },
    { id: 'BRAVO-SIX', name: 'Earbuds [BT LE]', dist: 0.82, angle: 4.15, status: 'GATE IDLE', color: '#ffb800', pinged: false },
    { id: 'GHOST-REC', name: 'Pixel 8 Node', dist: 0.52, angle: 5.60, status: 'ROOM PEER', color: '#00ff9d', pinged: false }
  ];

  // Particle pings spawned by user click
  let userPings = [];

  function drawGrid() {
    ctx.clearRect(0, 0, width, height);

    // Outer boundary glow
    ctx.save();
    ctx.beginPath();
    ctx.arc(cx, cy, radius, 0, Math.PI * 2);
    ctx.strokeStyle = 'rgba(0, 255, 157, 0.25)';
    ctx.lineWidth = 2;
    ctx.stroke();

    // Concentric range rings
    const ringCounts = 4;
    for (let i = 1; i <= ringCounts; i++) {
      let r = (radius / ringCounts) * i;
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.strokeStyle = i === ringCounts ? 'rgba(0, 255, 157, 0.4)' : 'rgba(0, 255, 157, 0.12)';
      ctx.setLineDash(i % 2 === 0 ? [4, 4] : []);
      ctx.stroke();
      ctx.setLineDash([]);

      // Ring Range Text
      ctx.font = '10px JetBrains Mono, monospace';
      ctx.fillStyle = 'rgba(0, 255, 157, 0.45)';
      ctx.fillText(`${i * 25}m`, cx + 6, cy - r + 12);
    }

    // Crosshairs
    ctx.beginPath();
    ctx.moveTo(cx - radius, cy);
    ctx.lineTo(cx + radius, cy);
    ctx.moveTo(cx, cy - radius);
    ctx.lineTo(cx, cy + radius);
    ctx.strokeStyle = 'rgba(0, 255, 157, 0.15)';
    ctx.lineWidth = 1;
    ctx.stroke();

    // Center Origin Pulse
    ctx.beginPath();
    ctx.arc(cx, cy, 3, 0, Math.PI * 2);
    ctx.fillStyle = '#00ff9d';
    ctx.shadowColor = '#00ff9d';
    ctx.shadowBlur = 8;
    ctx.fill();
    ctx.restore();
  }

  function drawSweep() {
    ctx.save();
    ctx.translate(cx, cy);

    // Phosphor sweep trail (conic gradient simulation with arcs)
    const trailSegments = 30;
    const trailAngle = 0.55; // width of trail in radians
    for (let i = 0; i < trailSegments; i++) {
      let startA = angle - (trailAngle / trailSegments) * (i + 1);
      let endA = angle - (trailAngle / trailSegments) * i;
      let alpha = (1 - i / trailSegments) * 0.22;

      ctx.beginPath();
      ctx.moveTo(0, 0);
      ctx.arc(0, 0, radius, startA, endA);
      ctx.closePath();
      ctx.fillStyle = `rgba(0, 255, 157, ${alpha})`;
      ctx.fill();
    }

    // Leading sweep line
    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.lineTo(Math.cos(angle) * radius, Math.sin(angle) * radius);
    ctx.strokeStyle = '#00ff9d';
    ctx.lineWidth = 2;
    ctx.shadowColor = '#00ff9d';
    ctx.shadowBlur = 10;
    ctx.stroke();

    ctx.restore();
  }

  function drawPeers() {
    peers.forEach((peer) => {
      let px = cx + Math.cos(peer.angle) * (peer.dist * radius);
      let py = cy + Math.sin(peer.angle) * (peer.dist * radius);

      // Check if sweep line is crossing this peer
      let diff = (angle - peer.angle) % (Math.PI * 2);
      if (diff < 0) diff += Math.PI * 2;
      let isLit = diff < 0.35;

      ctx.save();
      // Ping ripple on sweep pass
      if (isLit) {
        ctx.beginPath();
        ctx.arc(px, py, 12, 0, Math.PI * 2);
        ctx.strokeStyle = peer.color;
        ctx.lineWidth = 1.5;
        ctx.shadowColor = peer.color;
        ctx.shadowBlur = 12;
        ctx.stroke();
      }

      // Blip core
      ctx.beginPath();
      ctx.arc(px, py, isLit ? 5 : 3.5, 0, Math.PI * 2);
      ctx.fillStyle = peer.color;
      ctx.shadowColor = peer.color;
      ctx.shadowBlur = isLit ? 14 : 4;
      ctx.fill();

      // Blip tactical label
      ctx.font = '9px JetBrains Mono, monospace';
      ctx.fillStyle = isLit ? '#ffffff' : 'rgba(255, 255, 255, 0.55)';
      ctx.fillText(peer.name, px + 8, py - 4);
      ctx.fillStyle = peer.color;
      ctx.fillText(peer.status, px + 8, py + 7);

      ctx.restore();
    });
  }

  function drawUserPings() {
    for (let i = userPings.length - 1; i >= 0; i--) {
      let p = userPings[i];
      p.r += 1.8;
      p.alpha -= 0.018;

      ctx.save();
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.strokeStyle = `rgba(0, 229, 255, ${Math.max(0, p.alpha)})`;
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.restore();

      if (p.alpha <= 0) {
        userPings.splice(i, 1);
      }
    }
  }

  function animate() {
    angle = (angle + speed) % (Math.PI * 2);
    drawGrid();
    drawSweep();
    drawPeers();
    drawUserPings();

    // Update Telemetry bearing HUD text
    let degrees = Math.round((angle * 180) / Math.PI);
    const bearingEl = document.getElementById('hudBearingVal');
    if (bearingEl) bearingEl.textContent = `${degrees.toString().padStart(3, '0')}°`;

    requestAnimationFrame(animate);
  }

  // Interactive Click on Radar to Ping
  canvas.addEventListener('click', (e) => {
    const rect = canvas.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * width;
    const y = ((e.clientY - rect.top) / rect.height) * height;

    userPings.push({ x, y, r: 4, alpha: 1.0 });

    // Toast confirmation
    if (window.showTacticalToast) {
      window.showTacticalToast('TACTICAL BEACON TRANSMITTED (UDP 51112)');
    }
  });

  animate();
})();
