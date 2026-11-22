// Tiny synth for SushiTV's sounds: every voice adds into a Float32Array at SR Hz (deterministic).
const TAU = Math.PI * 2, mf = m => 440 * Math.pow(2, (m - 69) / 12);
function Synth(seconds, SR) {
  const N = Math.round(seconds * SR), S = new Float32Array(N);
  const add = (i, v) => { if (i >= 0 && i < N) S[i] += v };
  let seed = 12345; const rnd = () => { seed = (seed * 1103515245 + 12345) % 2147483648; return seed / 1073741824 - 1 };
  const k0 = t => Math.floor(t * SR);
  return {
    S, N, SR,
    // Music-box / kalimba pluck.
    pluck(t0, f, vol, len = 1.3) { const s0 = k0(t0), n = k0(len); for (let k = 0; k < n; k++) { const t = k / SR, a = Math.min(1, t / 0.004); add(s0 + k, vol * a * (Math.sin(TAU * f * t) * Math.exp(-t / 0.6) + 0.35 * Math.sin(2 * TAU * f * t) * Math.exp(-t / 0.25) + 0.12 * Math.sin(3 * TAU * f * t) * Math.exp(-t / 0.12))) } },
    // Short staccato pluck (pizzicato).
    pizz(t0, f, vol) { const s0 = k0(t0), n = k0(0.3); for (let k = 0; k < n; k++) { const t = k / SR, a = Math.min(1, t / 0.003); add(s0 + k, vol * a * (Math.sin(TAU * f * t) + 0.3 * Math.sin(2 * TAU * f * t)) * Math.exp(-t / 0.07)) } },
    bass(t0, f, vol, len = 0.6) { const s0 = k0(t0), n = k0(len); for (let k = 0; k < n; k++) { const t = k / SR, a = Math.min(1, t / 0.01); add(s0 + k, vol * a * (Math.sin(TAU * f * t) * Math.exp(-t / 0.32) + 0.25 * Math.sin(2 * TAU * f * t) * Math.exp(-t / 0.15))) } },
    pad(t0, dur, ms, vol) { const s0 = k0(t0), n = k0(dur); for (const m of ms) { const f = mf(m); for (let k = 0; k < n; k++) { const t = k / SR, e = Math.max(0, Math.min(1, t / 0.4, (dur - t) / 0.5)); add(s0 + k, vol * e * (Math.sin(TAU * f * t) + 0.5 * Math.sin(TAU * f * 1.004 * t))) } } },
    glide(t0, dur, f0, f1, vol, vib = 0, vf = 6) { const s0 = k0(t0), n = k0(dur); let ph = 0; for (let k = 0; k < n; k++) { const u = k / n, t = k / SR, f = f0 * Math.pow(f1 / f0, u) * (1 + vib * Math.sin(TAU * vf * t)); ph += TAU * f / SR; const e = Math.max(0, Math.min(1, u / 0.1, (1 - u) / 0.2)); add(s0 + k, vol * e * Math.sin(ph)) } },
    noise(t0, dur, vol, lp) { const s0 = k0(t0), n = k0(dur); let y = 0; for (let k = 0; k < n; k++) { const u = k / n; y += lp * (rnd() - y); add(s0 + k, vol * Math.min(1, u / 0.05) * (1 - u) * y) } },
    boing(t0, dur, f, vol) { const s0 = k0(t0), n = k0(dur); let ph = 0; for (let k = 0; k < n; k++) { const t = k / SR, d = Math.exp(-t / 0.25); ph += TAU * f * (1 + 0.08 * d * Math.sin(TAU * 14 * t)) / SR; add(s0 + k, vol * Math.min(1, t / 0.01) * d * Math.sin(ph)) } },
    // Soft breathing / snoring: filtered noise that swells with a breath of `period` seconds.
    breath(t0, dur, vol, period) { const s0 = k0(t0), n = k0(dur); let y = 0; for (let k = 0; k < n; k++) { const t = k / SR; y += 0.05 * (rnd() - y); const b = Math.max(0, Math.sin(TAU * t / period)); const e = Math.max(0, Math.min(1, t / 0.4, (dur - t) / 0.5)); add(s0 + k, vol * e * b * b * y * 3) } },
    // Soft clip, fade out from `fadeAt`, normalise to `peak`.
    finish(fadeAt, peak = 0.8) { const end = N / SR; let pk = 0; for (let i = 0; i < N; i++) { const t = i / SR, f = t < fadeAt ? 1 : Math.max(0, 1 - (t - fadeAt) / (end - fadeAt)); S[i] = Math.tanh(1.3 * S[i] * f); pk = Math.max(pk, Math.abs(S[i])) } if (pk > 0) for (let i = 0; i < N; i++) S[i] *= peak / pk; return S },
  };
}
