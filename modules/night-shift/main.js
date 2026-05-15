// Night Shift — overlay an amber tint to reduce blue light.
// Uses a fixed full-screen overlay with `mix-blend-mode: multiply` so
// the page colours are tinted rather than just covered.

(function () {
  var opacity = Math.max(0, Math.min(100, parseInt(getConfig('opacity', '20'), 10) || 20)) / 100;

  function injectOverlay() {
    if (document.getElementById('wta-night-shift-overlay')) return;
    var overlay = document.createElement('div');
    overlay.id = 'wta-night-shift-overlay';
    overlay.style.cssText = [
      'position:fixed',
      'inset:0',
      'background:#ff9b40',
      'opacity:' + opacity,
      'mix-blend-mode:multiply',
      'pointer-events:none',
      'z-index:2147483647',
    ].join(';');
    (document.documentElement || document.body).appendChild(overlay);
  }

  if (document.body || document.documentElement) {
    injectOverlay();
  } else {
    document.addEventListener('DOMContentLoaded', injectOverlay, { once: true });
  }
})();
