/* ==========================================================================
   ECHOLINK INTERACTIVE TACTICAL PHONE MOCKUP SIMULATOR
   ========================================================================== */

(function initDeviceSimulator() {
  const mockupFrame = document.getElementById('phoneMockup');
  const screenContent = document.getElementById('phoneScreenContent');
  const islandNotch = document.getElementById('phoneNotch');

  const tabIos = document.getElementById('tabDeviceIos');
  const tabAndroid = document.getElementById('tabDeviceAndroid');

  const featureCards = document.querySelectorAll('.sim-feature-card');

  // Audio Click Feedback
  function playTacticalBeep(freq = 880, duration = 0.08) {
    try {
      const actx = new (window.AudioContext || window.webkitAudioContext)();
      const osc = actx.createOscillator();
      const gain = actx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, actx.currentTime);
      gain.gain.setValueAtTime(0.08, actx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, actx.currentTime + duration);
      osc.connect(gain);
      gain.connect(actx.destination);
      osc.start();
      osc.stop(actx.currentTime + duration);
    } catch (e) {}
  }

  // 1. Device Frame Switcher (iOS vs Android)
  if (tabIos && tabAndroid && mockupFrame) {
    tabIos.addEventListener('click', () => {
      tabIos.classList.add('active');
      tabAndroid.classList.remove('active');
      mockupFrame.style.borderRadius = '46px';
      mockupFrame.style.borderColor = '#253342';
      if (islandNotch) islandNotch.style.display = 'block';
      if (window.showTacticalToast) window.showTacticalToast('SWIFTUI / iOS 16+ VIEWPORT LOADED');
    });

    tabAndroid.addEventListener('click', () => {
      tabAndroid.classList.add('active');
      tabIos.classList.remove('active');
      mockupFrame.style.borderRadius = '24px';
      mockupFrame.style.borderColor = '#131e28';
      if (islandNotch) islandNotch.style.display = 'none';
      if (window.showTacticalToast) window.showTacticalToast('JETPACK COMPOSE / ANDROID VIEWPORT LOADED');
    });
  }

  // 2. Interactive Screens Inside Mockup
  const screens = {
    radar: `
      <div class="mockup-top-bar">
        <span>ECHOLINK // RADAR</span>
        <span style="color:#00ff9d;">● DISCOVERY ON</span>
      </div>
      <div class="mockup-radar-circle">
        <div class="mockup-radar-sweep"></div>
        <div class="mockup-blip" style="top:32px; left:110px;"></div>
        <div class="mockup-blip" style="top:118px; left:45px; background:#00ff9d;"></div>
        <div class="mockup-blip" style="top:90px; right:35px; background:#ffb800;"></div>
      </div>
      <div style="font-family:var(--text-mono); font-size:0.75rem; color:var(--text-secondary); text-align:center;">
        3 PEERS DETECTED NEARBY
      </div>
      <div style="display:flex; flex-direction:column; gap:0.5rem; margin-top:0.75rem;">
        <div style="background:#0b131b; border:1px solid rgba(0,255,157,0.2); padding:0.55rem; border-radius:6px; display:flex; justify-content:space-between; align-items:center;">
          <span style="font-size:0.75rem; font-weight:600;">S23 Ultra [Host]</span>
          <span style="font-size:0.65rem; color:#00ff9d;">-38 dBm</span>
        </div>
        <div style="background:#0b131b; border:1px solid rgba(0,229,255,0.2); padding:0.55rem; border-radius:6px; display:flex; justify-content:space-between; align-items:center;">
          <span style="font-size:0.75rem; font-weight:600;">iPhone 15 Pro</span>
          <span style="font-size:0.65rem; color:#00e5ff;">-42 dBm</span>
        </div>
      </div>
    `,

    intercom: `
      <div class="mockup-top-bar">
        <span id="pttStatusLabel" style="color:#00ff9d;">STANDBY // RX ONLY</span>
        <span style="color:#00e5ff;">CHACHA20-POLY</span>
      </div>
      <div style="text-align:center; margin-top:1.5rem;">
        <div style="font-size:1.15rem; font-weight:800; color:#fff;" id="pttPeerLabel">SQUAD ALPHA [ROOM]</div>
        <div style="font-family:var(--text-mono); font-size:0.75rem; color:#8da4b5;">HOST: YOU // 3 PEERS</div>
      </div>
      
      <!-- Tactile PTT Button -->
      <div class="mockup-ptt-button" id="mockupPttBtn">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
          <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
        </svg>
        <span style="margin-top:0.35rem; font-size:0.75rem;" id="pttBtnText">HOLD TO TALK</span>
      </div>

      <!-- Real-time VU Meter Bar -->
      <div style="background:#04070a; border:1px solid rgba(0,255,157,0.2); border-radius:4px; height:8px; width:100%; overflow:hidden;">
        <div id="mockupVuBar" style="height:100%; width:15%; background:#00ff9d; transition:width 0.1s ease;"></div>
      </div>
      <div style="display:flex; justify-content:space-between; font-family:var(--text-mono); font-size:0.65rem; color:#4e6575; margin-top:0.3rem;">
        <span>NOISE GATE: -46dB</span>
        <span>LATENCY: 9ms</span>
      </div>
    `,

    routing: `
      <div class="mockup-top-bar">
        <span>HOST ROUTING MATRIX</span>
        <span style="color:#ffb800;">HOST ACTIVE</span>
      </div>
      <div style="padding:0.75rem 0;">
        <div style="font-size:0.85rem; font-weight:700; color:#fff; margin-bottom:0.4rem;">VOICE TRANSMIT TARGET:</div>
        <div style="display:flex; gap:0.5rem; margin-bottom:1rem;">
          <button id="simBroadcastBtn" style="flex:1; padding:0.55rem; background:#00ff9d; color:#030608; border:none; border-radius:4px; font-family:var(--text-mono); font-size:0.7rem; font-weight:700; cursor:pointer;">
            TALK TO ALL (3)
          </button>
          <button id="simOneOnOneBtn" style="flex:1; padding:0.55rem; background:#0e161e; color:#8da4b5; border:1px solid rgba(0,255,157,0.2); border-radius:4px; font-family:var(--text-mono); font-size:0.7rem; cursor:pointer;">
            1-ON-1 PEER
          </button>
        </div>

        <div style="font-size:0.75rem; color:#8da4b5; margin-bottom:0.5rem; font-family:var(--text-mono);">CONNECTED ROOM MEMBERS:</div>
        <div style="display:flex; flex-direction:column; gap:0.45rem;">
          <div style="background:#0e161e; border:1px solid #00ff9d; padding:0.5rem 0.75rem; border-radius:6px; display:flex; justify-content:space-between; align-items:center;">
            <span style="font-size:0.8rem; font-weight:600;">S23 Ultra [Lead]</span>
            <span style="font-size:0.65rem; color:#00ff9d;">BROADCAST</span>
          </div>
          <div style="background:#0e161e; border:1px solid rgba(255,255,255,0.1); padding:0.5rem 0.75rem; border-radius:6px; display:flex; justify-content:space-between; align-items:center;">
            <span style="font-size:0.8rem; font-weight:600;">iPhone 15 Pro</span>
            <span style="font-size:0.65rem; color:#8da4b5;">STANDBY</span>
          </div>
        </div>
      </div>
    `,

    history: `
      <div class="mockup-top-bar">
        <span>DEVICE HISTORY</span>
        <span style="color:#ff3366; cursor:pointer;" id="simClearHistoryBtn">CLEAR ALL</span>
      </div>
      <div style="display:flex; flex-direction:column; gap:0.65rem; margin-top:0.75rem;" id="simHistoryList">
        <div class="history-item" style="background:#0e161e; border:1px solid rgba(0,255,157,0.15); padding:0.65rem; border-radius:6px; display:flex; justify-content:space-between; align-items:center;">
          <div>
            <div style="font-size:0.8rem; font-weight:700; color:#fff;" class="peer-title">Tactical Unit 1</div>
            <div style="font-size:0.65rem; font-family:var(--text-mono); color:#8da4b5;">192.168.1.6 // LAST SEEN 2M AGO</div>
          </div>
          <div style="display:flex; gap:0.4rem;">
            <button class="rename-btn" style="background:none; border:none; color:#00e5ff; cursor:pointer; font-size:0.85rem;">✏️</button>
            <button class="delete-btn" style="background:none; border:none; color:#ff3366; cursor:pointer; font-size:0.95rem;">×</button>
          </div>
        </div>
        <div class="history-item" style="background:#0e161e; border:1px solid rgba(0,255,157,0.15); padding:0.65rem; border-radius:6px; display:flex; justify-content:space-between; align-items:center;">
          <div>
            <div style="font-size:0.8rem; font-weight:700; color:#fff;" class="peer-title">Echo Point Recon</div>
            <div style="font-size:0.65rem; font-family:var(--text-mono); color:#8da4b5;">192.168.1.14 // LAST SEEN 10M AGO</div>
          </div>
          <div style="display:flex; gap:0.4rem;">
            <button class="rename-btn" style="background:none; border:none; color:#00e5ff; cursor:pointer; font-size:0.85rem;">✏️</button>
            <button class="delete-btn" style="background:none; border:none; color:#ff3366; cursor:pointer; font-size:0.95rem;">×</button>
          </div>
        </div>
      </div>
    `
  };

  function setScreen(screenKey) {
    if (!screenContent || !screens[screenKey]) return;
    screenContent.innerHTML = screens[screenKey];
    bindScreenInteractions(screenKey);
  }

  function bindScreenInteractions(screenKey) {
    if (screenKey === 'intercom') {
      const pttBtn = document.getElementById('mockupPttBtn');
      const pttStatus = document.getElementById('pttStatusLabel');
      const vuBar = document.getElementById('mockupVuBar');
      const pttText = document.getElementById('pttBtnText');

      if (pttBtn) {
        const startTransmit = () => {
          pttBtn.classList.add('pressing');
          playTacticalBeep(1200, 0.05);
          if (pttStatus) {
            pttStatus.textContent = '● TX ACTIVE (TRANSMITTING)';
            pttStatus.style.color = '#ff3366';
          }
          if (pttText) pttText.textContent = 'TRANSMITTING';
          if (vuBar) vuBar.style.width = '94%';
        };

        const stopTransmit = () => {
          pttBtn.classList.remove('pressing');
          playTacticalBeep(600, 0.06);
          if (pttStatus) {
            pttStatus.textContent = 'STANDBY // RX ONLY';
            pttStatus.style.color = '#00ff9d';
          }
          if (pttText) pttText.textContent = 'HOLD TO TALK';
          if (vuBar) vuBar.style.width = '12%';
        };

        pttBtn.addEventListener('mousedown', startTransmit);
        window.addEventListener('mouseup', stopTransmit);
        pttBtn.addEventListener('touchstart', (e) => { e.preventDefault(); startTransmit(); });
        pttBtn.addEventListener('touchend', (e) => { e.preventDefault(); stopTransmit(); });
      }
    } else if (screenKey === 'routing') {
      const bBtn = document.getElementById('simBroadcastBtn');
      const oBtn = document.getElementById('simOneOnOneBtn');
      if (bBtn && oBtn) {
        bBtn.addEventListener('click', () => {
          bBtn.style.background = '#00ff9d';
          bBtn.style.color = '#030608';
          oBtn.style.background = '#0e161e';
          oBtn.style.color = '#8da4b5';
          playTacticalBeep(950, 0.04);
          if (window.showTacticalToast) window.showTacticalToast('HOST ROUTING: BROADCAST TO ALL PEERS');
        });
        oBtn.addEventListener('click', () => {
          oBtn.style.background = '#00e5ff';
          oBtn.style.color = '#030608';
          bBtn.style.background = '#0e161e';
          bBtn.style.color = '#8da4b5';
          playTacticalBeep(1100, 0.04);
          if (window.showTacticalToast) window.showTacticalToast('HOST ROUTING: 1-ON-1 SELECTIVE MODE');
        });
      }
    } else if (screenKey === 'history') {
      const deleteBtns = screenContent.querySelectorAll('.delete-btn');
      deleteBtns.forEach((btn) => {
        btn.addEventListener('click', (e) => {
          const item = e.target.closest('.history-item');
          if (item) item.remove();
          playTacticalBeep(450, 0.05);
          if (window.showTacticalToast) window.showTacticalToast('PEER REMOVED FROM HISTORY');
        });
      });

      const renameBtns = screenContent.querySelectorAll('.rename-btn');
      renameBtns.forEach((btn) => {
        btn.addEventListener('click', (e) => {
          const item = e.target.closest('.history-item');
          const title = item ? item.querySelector('.peer-title') : null;
          if (title) {
            const newName = prompt('Enter new tactical name for device:', title.textContent);
            if (newName && newName.trim()) {
              title.textContent = newName.trim();
              playTacticalBeep(1000, 0.05);
              if (window.showTacticalToast) window.showTacticalToast(`DEVICE RENAMED TO: ${newName.trim()}`);
            }
          }
        });
      });

      const clearBtn = document.getElementById('simClearHistoryBtn');
      if (clearBtn) {
        clearBtn.addEventListener('click', () => {
          const list = document.getElementById('simHistoryList');
          if (list) list.innerHTML = '<div style="font-family:var(--text-mono); font-size:0.75rem; color:#4e6575; text-align:center; padding:1.5rem;">HISTORY CLEARED</div>';
          playTacticalBeep(400, 0.08);
          if (window.showTacticalToast) window.showTacticalToast('CONNECTED PEER HISTORY CLEARED');
        });
      }
    }
  }

  // Feature Card Clicks
  featureCards.forEach((card) => {
    card.addEventListener('click', () => {
      featureCards.forEach((c) => c.classList.remove('active'));
      card.classList.add('active');
      const target = card.getAttribute('data-screen');
      setScreen(target);
    });
  });

  // Default Screen
  setScreen('intercom');
})();
