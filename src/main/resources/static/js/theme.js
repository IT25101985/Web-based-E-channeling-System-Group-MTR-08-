function applyTheme(theme) {
 document.documentElement.classList.toggle('dark-mode', theme === 'dark');
 document.documentElement.dataset.theme = theme;
}
try { applyTheme(localStorage.getItem('hc-theme') || 'light'); } catch (_) { applyTheme('light'); }
function toggleGlobalTheme() {
 const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
 applyTheme(next); try { localStorage.setItem('hc-theme', next); } catch (_) {}
}
window.addEventListener('storage', event => { if (event.key === 'hc-theme') applyTheme(event.newValue || 'light'); });
