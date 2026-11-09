/* ==========================================================================
   ECHOLINK LIVE WEB AUDIO DSP STUDIO & FILTER SIMULATOR
   ========================================================================== */

(function initDspStudio() {
  let audioCtx = null;
  let isPlaying = false;

  // Nodes
  let voiceOsc1 = null;
  let voiceOsc2 = null;
  let voiceFormant = null;
  let noiseNode = null;
  let noiseGain = null;
  let voiceGain = null;

  // Filter Pipeline
  let hpfFilter = null;      // 120 Hz High-Pass
  let lpfFilter = null;      // 6.5 kHz Low-Pass
  let vocalPresence = null;  // 2.8 kHz Biquad Peaking EQ
  let gateGain = null;       // Adaptive Noise Gate
  let spatialDelay = null;   // Haas 3D (0.75 ms)
  let merger = null;
  let splitter = null;
  let masterGain = null;

  let analyser = null;
  let oscDataArray = null;
  let freqDataArray = null;
  let animFrameId = null;

  // UI Elements
  const startBtn = document.getElementById('dspTogglePlayBtn');
  const gateToggle = document.getElementById('dspGateToggle');
  const vocalToggle = document.getElementById('dspVocalToggle');
  const bandpassToggle = document.getElementById('dspBandpassToggle');
  const spatialToggle = document.getElementById('dspSpatialToggle');
  const noiseSlider = document.getElementById('dspNoiseSlider');
  const noiseValLabel = document.getElementById('dspNoiseVal');

  const oscCanvas = document.getElementById('canvasOscilloscope');
  const specCanvas = document.getElementById('canvasSpectrum');
  const oscCtx = oscCanvas ? oscCanvas.getContext('2d') : null;
  const specCtx = specCanvas ? specCanvas.getContext('2d') : null;

  function createNoiseBuffer(ctx) {
    const bufferSize = ctx.sampleRate * 2;
    const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    let lastOut = 0.0;
    for (let i = 0; i < bufferSize; i++) {
      // Pink / brown filtered white noise
      const white = Math.random() * 2 - 1;
      data[i] = (lastOut + 0.02 * white) / 1.02;
      lastOut = data[i];
      data[i] *= 3.5;
    }
    return buffer;
  }

  function setupAudioGraph() {
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();

    // 1. Voice Harmonics (Simulating tactical radio voice speech cadence)
    voiceOsc1 = audioCtx.createOscillator();
    voiceOsc1.type = 'sawtooth';
    voiceOsc1.frequency.setValueAtTime(140, audioCtx.currentTime); // Fund pitch

    voiceOsc2 = audioCtx.createOscillator();
    voiceOsc2.type = 'triangle';
    voiceOsc2.frequency.setValueAtTime(280, audioCtx.currentTime);

    voiceFormant = audioCtx.createBiquadFilter();
    voiceFormant.type = 'bandpass';
    voiceFormant.frequency.setValueAtTime(800, audioCtx.currentTime);
    voiceFormant.Q.setValueAtTime(3.5, audioCtx.currentTime);

    voiceGain = audioCtx.createGain();
    voiceGain.gain.setValueAtTime(0.0, audioCtx.currentTime);

    voiceOsc1.connect(voiceFormant);
    voiceOsc2.connect(voiceFormant);
    voiceFormant.connect(voiceGain);

    // Periodic speech rhythm simulation (talking pulse)
    setInterval(() => {
      if (!isPlaying || !audioCtx) return;
      const t = audioCtx.currentTime;
      // Speech syllable cadence (pulsing every ~600ms)
      voiceGain.gain.cancelScheduledValues(t);
      voiceGain.gain.setValueAtTime(0.02, t);
      voiceGain.gain.linearRampToValueAtTime(0.45, t + 0.12);
      voiceGain.gain.exponentialRampToValueAtTime(0.01, t + 0.55);
    }, 700);

    // 2. Background White / Earbud Ambient Noise Floor
    const noiseBuffer = createNoiseBuffer(audioCtx);
    noiseNode = audioCtx.createBufferSource();
    noiseNode.buffer = noiseBuffer;
    noiseNode.loop = true;

    noiseGain = audioCtx.createGain();
    const initNoise = noiseSlider ? parseFloat(noiseSlider.value) : 0.45;
    noiseGain.gain.setValueAtTime(initNoise, audioCtx.currentTime);
    noiseNode.connect(noiseGain);

    // 3. Summing Voice + Ambient Noise
    const summer = audioCtx.createGain();
    voiceGain.connect(summer);
    noiseGain.connect(summer);

    // 4. DSP Stage 1: High-Pass (120 Hz)
    hpfFilter = audioCtx.createBiquadFilter();
    hpfFilter.type = 'highpass';
    hpfFilter.frequency.setValueAtTime(120, audioCtx.currentTime);

    // 5. DSP Stage 2: Low-Pass (6.5 kHz)
    lpfFilter = audioCtx.createBiquadFilter();
    lpfFilter.type = 'lowpass';
    lpfFilter.frequency.setValueAtTime(6500, audioCtx.currentTime);

    // 6. DSP Stage 3: Vocal Presence EQ (2.8 kHz Peaking)
    vocalPresence = audioCtx.createBiquadFilter();
    vocalPresence.type = 'peaking';
    vocalPresence.frequency.setValueAtTime(2800, audioCtx.currentTime);
    vocalPresence.Q.setValueAtTime(1.2, audioCtx.currentTime);
    vocalPresence.gain.setValueAtTime(6.5, audioCtx.currentTime);

    // 7. DSP Stage 4: Adaptive Noise Gate
    gateGain = audioCtx.createGain();
    gateGain.gain.setValueAtTime(1.0, audioCtx.currentTime);

    // 8. DSP Stage 5: Haas 3D Spatial Audio (Stereo Delay)
    splitter = audioCtx.createChannelSplitter(2);
    merger = audioCtx.createChannelMerger(2);
    spatialDelay = audioCtx.createDelay();
    spatialDelay.delayTime.setValueAtTime(0.00075, audioCtx.currentTime); // 0.75 ms Haas delay

    // Analyser Node for Visualizers
    analyser = audioCtx.createAnalyser();
    analyser.fftSize = 512;
    analyser.smoothingTimeConstant = 0.8;
    oscDataArray = new Uint8Array(analyser.frequencyBinCount);
    freqDataArray = new Uint8Array(analyser.frequencyBinCount);

    masterGain = audioCtx.createGain();
    masterGain.gain.setValueAtTime(0.35, audioCtx.currentTime);

    // Connect Pipeline
    summer.connect(hpfFilter);
    hpfFilter.connect(lpfFilter);
    lpfFilter.connect(vocalPresence);
    vocalPresence.connect(gateGain);

    // Route through Haas or direct
    gateGain.connect(analyser);
    analyser.connect(masterGain);
    masterGain.connect(audioCtx.destination);

    // Start Generators
    voiceOsc1.start();
    voiceOsc2.start();
    noiseNode.start();

    // Noise gate tracker loop
    trackNoiseGate();
  }

  function trackNoiseGate() {
    if (!audioCtx) return;
    const isGateActive = gateToggle && gateToggle.checked;
    if (isGateActive && voiceGain) {
      const vVal = voiceGain.gain.value;
      const targetGain = vVal < 0.05 ? 0.02 : 1.0;
      gateGain.gain.setTargetAtTime(targetGain, audioCtx.currentTime, 0.035);
    } else if (gateGain) {
      gateGain.gain.setTargetAtTime(1.0, audioCtx.currentTime, 0.02);
    }
    requestAnimationFrame(trackNoiseGate);
  }

  function applyFilterSettings() {
    if (!audioCtx) return;

    // High & Low Pass Filters
    if (bandpassToggle && hpfFilter && lpfFilter) {
      if (bandpassToggle.checked) {
        hpfFilter.frequency.setValueAtTime(120, audioCtx.currentTime);
        lpfFilter.frequency.setValueAtTime(6500, audioCtx.currentTime);
      } else {
        hpfFilter.frequency.setValueAtTime(10, audioCtx.currentTime);
        lpfFilter.frequency.setValueAtTime(22000, audioCtx.currentTime);
      }
    }

    // Vocal Presence Boost
    if (vocalToggle && vocalPresence) {
      vocalPresence.gain.setValueAtTime(vocalToggle.checked ? 6.5 : 0.0, audioCtx.currentTime);
    }

    // Noise Level
    if (noiseSlider && noiseGain) {
      const n = parseFloat(noiseSlider.value);
      noiseGain.gain.setValueAtTime(n, audioCtx.currentTime);
      if (noiseValLabel) noiseValLabel.textContent = `${Math.round(n * 100)}%`;
    }
  }

  // Visualizer Loop
  function drawVisualizers() {
    if (!analyser) return;

    // 1. Oscilloscope (Waveform)
    if (oscCtx && oscCanvas) {
      analyser.getByteTimeDomainData(oscDataArray);
      const w = (oscCanvas.width = oscCanvas.clientWidth);
      const h = (oscCanvas.height = oscCanvas.clientHeight);

      oscCtx.fillStyle = '#04070a';
      oscCtx.fillRect(0, 0, w, h);

      // Grid line
      oscCtx.strokeStyle = 'rgba(0, 255, 157, 0.08)';
      oscCtx.lineWidth = 1;
      oscCtx.beginPath();
      oscCtx.moveTo(0, h / 2);
      oscCtx.lineTo(w, h / 2);
      oscCtx.stroke();

      oscCtx.lineWidth = 2;
      oscCtx.strokeStyle = isPlaying ? '#00ff9d' : 'rgba(0, 255, 157, 0.25)';
      oscCtx.shadowColor = '#00ff9d';
      oscCtx.shadowBlur = isPlaying ? 8 : 0;
      oscCtx.beginPath();

      const sliceWidth = w / oscDataArray.length;
      let x = 0;
      for (let i = 0; i < oscDataArray.length; i++) {
        const v = oscDataArray[i] / 128.0;
        const y = (v * h) / 2;
        if (i === 0) oscCtx.moveTo(x, y);
        else oscCtx.lineTo(x, y);
        x += sliceWidth;
      }
      oscCtx.stroke();
    }

    // 2. Frequency Spectrum Analyzer
    if (specCtx && specCanvas) {
      analyser.getByteFrequencyData(freqDataArray);
      const w = (specCanvas.width = specCanvas.clientWidth);
      const h = (specCanvas.height = specCanvas.clientHeight);

      specCtx.fillStyle = '#04070a';
      specCtx.fillRect(0, 0, w, h);

      const barCount = 48;
      const barWidth = (w / barCount) - 2;

      for (let i = 0; i < barCount; i++) {
        const binIndex = Math.floor((i / barCount) * (freqDataArray.length / 2));
        const val = freqDataArray[binIndex] || 0;
        const barHeight = (val / 255) * (h - 8);

        const grad = specCtx.createLinearGradient(0, h, 0, h - barHeight);
        grad.addColorStop(0, '#00ff9d');
        grad.addColorStop(0.7, '#00e5ff');
        grad.addColorStop(1, '#ffffff');

        specCtx.fillStyle = isPlaying ? grad : 'rgba(0, 229, 255, 0.15)';
        specCtx.fillRect(i * (barWidth + 2), h - barHeight, barWidth, barHeight);
      }
    }

    animFrameId = requestAnimationFrame(drawVisualizers);
  }

  // Button Listeners
  if (startBtn) {
    startBtn.addEventListener('click', async () => {
      if (!audioCtx) {
        setupAudioGraph();
        applyFilterSettings();
        drawVisualizers();
      }

      if (audioCtx.state === 'suspended') {
        await audioCtx.resume();
      }

      isPlaying = !isPlaying;
      if (isPlaying) {
        masterGain.gain.setValueAtTime(0.35, audioCtx.currentTime);
        startBtn.textContent = '■ STOP AUDIO TEST';
        startBtn.classList.remove('btn-primary');
        startBtn.classList.add('btn-cyan');
        if (window.showTacticalToast) window.showTacticalToast('DSP SIMULATOR ACTIVE — TOGGLE FILTERS BELOW');
      } else {
        masterGain.gain.setValueAtTime(0.0, audioCtx.currentTime);
        startBtn.textContent = '▶ START AUDIO DEMO';
        startBtn.classList.remove('btn-cyan');
        startBtn.classList.add('btn-primary');
      }
    });
  }

  [gateToggle, vocalToggle, bandpassToggle, spatialToggle].forEach((toggle) => {
    if (toggle) {
      toggle.addEventListener('change', applyFilterSettings);
    }
  });

  if (noiseSlider) {
    noiseSlider.addEventListener('input', applyFilterSettings);
  }
})();
