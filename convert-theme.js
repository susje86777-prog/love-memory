const fs=require('fs');
const path=require('path');
const src=path.resolve(__dirname,'index.html');
// backup first
fs.copyFileSync(src, path.resolve(__dirname,'index-backup.html'));
let html=fs.readFileSync(src,'utf8');

// ── CSS 变量替换: 暗色 → 奶油风 ──
const vars={
  '--primary: #ff6b81;':'--primary: #e8a0b0;',
  '--primary-dark: #ee5a6f;':'--primary-dark: #d4869a;',
  '--secondary: #ffb8c6;':'--secondary: #f5c6d0;',
  '--accent: #ff4757;':'--accent: #c97a8f;',
  '--bg: #0a0a12;':'--bg: #fdf6f0;',
  '--bg-card: rgba(255,255,255,0.08);':'--bg-card: rgba(255,255,255,0.75);',
  '--bg-card-hover: rgba(255,255,255,0.14);':'--bg-card-hover: rgba(255,255,255,0.92);',
  '--glass: rgba(255,255,255,0.06);':'--glass: rgba(255,255,255,0.55);',
  '--glass-border: rgba(255,255,255,0.12);':'--glass-border: rgba(232,160,176,0.25);',
  '--text: #f0e6e8;':'--text: #5a3e46;',
  '--text-secondary: #b8a0a8;':'--text-secondary: #8c6472;',
  '--text-muted: #7a6670;':'--text-muted: #b89aa4;',
  '--shadow: 0 8px 32px rgba(0,0,0,0.4);':'--shadow: 0 8px 32px rgba(200,140,160,0.15);',
  '--shadow-lg: 0 16px 48px rgba(0,0,0,0.5);':'--shadow-lg: 0 16px 48px rgba(200,140,160,0.22);',
  '--radius: 16px;':'--radius: 20px;',
  '--radius-sm: 10px;':'--radius-sm: 14px;',
  '--radius-xs: 8px;':'--radius-xs: 10px;',
};
for(const [old,nu] of Object.entries(vars)) html=html.split(old).join(nu);

// font
html=html.replace(
  "--font: 'Segoe UI', system-ui, -apple-system, sans-serif;",
  "--font: 'PingFang SC', 'Segoe UI', system-ui, -apple-system, sans-serif;"
);

// theme-color meta
html=html.replace(
  '<meta name="theme-color" content="#ff6b81">',
  '<meta name="theme-color" content="#f5c6d0">'
);

// lock-screen background
html=html.replace(
  'background: var(--bg);',
  'background: linear-gradient(145deg, #fdf0f3 0%, #fef6ee 50%, #f8f0fc 100%);'
);

// lock-card rgba
html=html.replace(
  'background: rgba(20, 15, 30, 0.92);',
  'background: rgba(255,255,255,0.82);'
);
// lock-card glass
html=html.replace(
  'background: var(--glass);',
  'background: rgba(255,255,255,0.82);'
);
// lock-card border+shadow+animation combo
html=html.replace(
  'border: 1px solid var(--glass-border);\n  box-shadow: var(--shadow-lg);\n  max-width: 380px; width: 90%;\n  animation: lockIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);',
  'border: 1px solid rgba(232,160,176,0.3);\n  box-shadow: 0 20px 60px rgba(200,140,160,0.18), 0 4px 16px rgba(200,140,160,0.1);\n  max-width: 380px; width: 90%;\n  animation: lockIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);\n  backdrop-filter: blur(20px);\n  -webkit-backdrop-filter: blur(20px);'
);
// lock-card border-radius 24→28
html=html.replace(
  'padding: 40px 30px;\n  border-radius: 24px;',
  'padding: 40px 30px;\n  border-radius: 28px;'
);

// tab-bar background
html=html.replace(
  'background: rgba(10,10,18,0.85);',
  'background: rgba(255,252,250,0.88);'
);

// tab-bar border-top + box-shadow
html=html.replace(
  'border-top: 1px solid var(--glass-border);\n  z-index: 100;\n  padding-bottom: env(safe-area-inset-bottom, 0px);',
  'border-top: 1px solid rgba(232,160,176,0.2);\n  z-index: 100;\n  padding-bottom: env(safe-area-inset-bottom, 0px);\n  box-shadow: 0 -4px 20px rgba(200,140,160,0.08);'
);

// modal overlay
html=html.replace(
  'background: rgba(0,0,0,0.75);',
  'background: rgba(180,140,155,0.35);'
);
// modal content bg
html=html.replace(
  'background: rgba(20,20,35,0.95);',
  'background: rgba(255,252,250,0.96);'
);
// modal content border
html=html.replace(
  'border: 1px solid var(--glass-border);\n  border-radius: var(--radius);\n  max-width: 600px; width: 100%; max-height: 85vh;\n  overflow-y: auto; padding: 24px;\n  box-shadow: var(--shadow-lg);',
  'border: 1px solid rgba(232,160,176,0.2);\n  border-radius: var(--radius);\n  max-width: 600px; width: 100%; max-height: 85vh;\n  overflow-y: auto; padding: 24px;\n  box-shadow: 0 20px 60px rgba(180,120,140,0.2);'
);

// bg-overlay
html=html.replace(
  'background: radial-gradient(ellipse at 50% 0%, rgba(255,107,129,0.08) 0%, transparent 60%),',
  'background: radial-gradient(ellipse at 20% 10%, rgba(248,210,220,0.45) 0%, transparent 55%),'
);
html=html.replace(
  'radial-gradient(ellipse at 80% 80%, rgba(180,130,200,0.06) 0%, transparent 50%);',
  'radial-gradient(ellipse at 85% 85%, rgba(220,200,240,0.3) 0%, transparent 50%),\n              radial-gradient(ellipse at 50% 50%, rgba(255,245,235,0.2) 0%, transparent 70%);'
);

// anniversary-banner
html=html.replace(
  '#FFF5EE',
  'linear-gradient(135deg, rgba(232,160,176,0.18), rgba(245,198,208,0.12), rgba(220,200,240,0.1))'
);
html=html.replace(
  'border: 1px solid rgba(255,182,193,0.35);\n  box-shadow: 0 4px 24px rgba(89,73,71,0.10), 0 2px 8px rgba(89,73,71,0.05);',
  'border: 1px solid rgba(232,160,176,0.25);\n  backdrop-filter: blur(10px);\n  box-shadow: 0 4px 20px rgba(200,140,160,0.1);'
);
html=html.replace(
  'margin-bottom: 20px; border-radius: 20px;',
  'margin-bottom: 20px; border-radius: 24px;'
);

// days-count gradient
html=html.replace(
  'linear-gradient(135deg, #FFB6C1, #FF9AAE, #E8A0B0, #FFB6C1)',
  'linear-gradient(135deg, #e8a0b0, #c97a8f, #b8a8d0)'
);
html=html.replace(
  'background-size: 300% auto;',
  'background-size: 200% auto;'
);

// days-label & milestone
html=html.replace(
  'color: #594947; letter-spacing: 2px; margin-top: 4px; }',
  'color: var(--text-secondary); letter-spacing: 2px; margin-top: 4px; }'
);
html=html.replace(
  'color: #8B6E6B; margin-top: 8px; }',
  'color: var(--text-muted); margin-top: 8px; }'
);

// Toast
html=html.replace('background: #2ed573;','background: #7dbfa0;');
html=html.replace('background: #ff4757;','background: #d4849a;');

// Presets
html=html.replace('#3a1020','#fde8ee').replace('#1a0510','#f8d8e4').replace('#0a0a12','#fdf0f5');
html=html.replace('#2c003e','#ede8f8').replace('#150020','#e0d8f4');
html=html.replace('#2d1520','#feeef4').replace('#1a0a14','#fde0ec');
html=html.replace('#0f0c29','#e8eef8').replace('#1a1040','#d8e4f4');
html=html.replace('#2d1a08','#fef4e8').replace('#1a0f05','#fdecd4');
html=html.replace('#301520','#feeef4').replace('#1a0d15','#fddde8');
html=html.replace('#1a1028','#f0eef8').replace('#110d1f','#e8e4f4');
html=html.replace('#2a1008','#fef4ec').replace('#180a06','#fde8d4');

// fallback preset colors in picker
html=html.replace("'#ff6b81','#0a0a12'","'#e8a0b0','#f5c6d0'");

// Theme data-theme colors
html=html.replace('--primary: #f8a4c8;','--primary: #f0a0b8;');
html=html.replace('--primary-dark: #e893b6;','--primary-dark: #db8ea6;');
html=html.replace('--secondary: #fcd5e4;','--secondary: #f8d0dc;');
html=html.replace('--accent: #e8879e;','--accent: #d4788e;');
html=html.replace('--bg: #1a0a14;','--bg: #fff5f8;');
html=html.replace('--primary: #b8a9c9;','--primary: #b8a8d0;');
html=html.replace('--primary-dark: #a695ba;','--primary-dark: #a494be;');
html=html.replace('--secondary: #d4c5e2;','--secondary: #d8cce8;');
html=html.replace('--accent: #9b7db5;','--accent: #9880b8;');
html=html.replace('--bg: #0f0c1a;','--bg: #f8f5fc;');
html=html.replace('--primary: #ff9a76;','--primary: #f0a888;');
html=html.replace('--primary-dark: #f08a65;','--primary-dark: #dc9474;');
html=html.replace('--secondary: #ffc3a0;','--secondary: #f8cdb8;');
html=html.replace('--accent: #ff6f3c;','--accent: #d4826a;');
html=html.replace('--bg: #1a0e08;','--bg: #fff8f4;');

// rgba primary references
html=html.replace(/rgba\(255,107,129,/g,'rgba(232,160,176,');
html=html.replace(/#ff6b81/g,'#c97a8f');
html=html.replace(/color:#ff6b81;/g,'color:#c97a8f;');

// Heart canvas colors
html=html.replace("'': ['#ff6b81', '#ffb8c6', '#ff8fa3']","'': ['#e8a0b0', '#f5c6d0', '#d4869a']");
html=html.replace("'blush': ['#f8a4c8', '#fcd5e4', '#ffb8d0']","'blush': ['#f0a0b8', '#f8d0dc', '#e8879e']");
html=html.replace("'lavender': ['#c3cfe2', '#d4c5e2', '#b8a9c9']","'lavender': ['#c3b8d8', '#d8cce8', '#b8a8d0']");
html=html.replace("'sunset': ['#ff9a76', '#ffc3a0', '#ffb088']","'sunset': ['#f0a888', '#f8cdb8', '#e89a78']");

// theme dot default
html=html.replace('data-theme-val="" style="background:#ff6b81;color:#ff6b81"','data-theme-val="" style="background:#e8a0b0;color:#e8a0b0"');

// form focus box-shadow
html=html.replace('box-shadow: 0 0 0 3px rgba(255,107,129,0.1);','box-shadow: 0 0 0 3px rgba(232,160,176,0.1);');

// Write both copies
fs.writeFileSync(path.resolve(__dirname,'www','index.html'), html);
fs.writeFileSync(path.resolve(__dirname,'index.html'), html);
console.log('OK: cream theme written,', html.length, 'bytes');
