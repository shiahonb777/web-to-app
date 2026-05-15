// Hello World — WebToApp example module.
// Runs at DOCUMENT_END and shows a floating banner with a greeting.
// All values come through `getConfig(key, default)` so the user can tweak
// them in the module settings.

(function () {
  var greeting = getConfig('greeting', 'Hello from WebToApp!');
  var durationMs = parseInt(getConfig('durationMs', '3000'), 10) || 3000;

  var banner = document.createElement('div');
  banner.textContent = greeting;
  banner.style.cssText = [
    'position:fixed',
    'left:50%',
    'top:24px',
    'transform:translateX(-50%) translateY(-20px)',
    'padding:10px 18px',
    'background:rgba(20,20,28,0.92)',
    'color:#fff',
    'border-radius:14px',
    'font:500 14px/1.4 -apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif',
    'box-shadow:0 8px 24px rgba(0,0,0,0.25)',
    'z-index:2147483646',
    'opacity:0',
    'transition:opacity .25s ease, transform .25s ease',
    'pointer-events:none',
  ].join(';');

  document.body.appendChild(banner);

  requestAnimationFrame(function () {
    banner.style.opacity = '1';
    banner.style.transform = 'translateX(-50%) translateY(0)';
  });

  setTimeout(function () {
    banner.style.opacity = '0';
    banner.style.transform = 'translateX(-50%) translateY(-20px)';
    setTimeout(function () {
      if (banner.parentNode) banner.parentNode.removeChild(banner);
    }, 300);
  }, durationMs);
})();
