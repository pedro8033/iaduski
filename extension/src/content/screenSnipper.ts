/**
 * Ferramenta visual de captura de área de ecrã (Snip Tool).
 * Permite ao utilizador arrastar uma caixa sobre símbolos, sequências lógicas,
 * figuras geométricas ou matrizes psicotécnicas e enviar a imagem recortada à IA.
 */

export function startScreenCapture(onCapture: (base64Image: string) => void): void {
  // Evita duplicar o overlay se o usuário já estiver capturando
  if (document.getElementById('ai-qa-snipper-host')) return;

  const host = document.createElement('div');
  host.id = 'ai-qa-snipper-host';
  host.style.cssText = 'all:initial;position:fixed;inset:0;z-index:2147483647;cursor:crosshair;user-select:none;';

  const shadow = host.attachShadow({ mode: 'closed' });
  shadow.innerHTML = `
    <style>
      :host {
        all: initial;
      }
      .backdrop {
        position: fixed;
        inset: 0;
        background: rgba(15, 23, 42, 0.45);
        backdrop-filter: blur(1.5px);
        cursor: crosshair;
        user-select: none;
      }
      .instruction-pill {
        position: fixed;
        top: 24px;
        left: 50%;
        transform: translateX(-50%);
        background: rgba(15, 23, 42, 0.92);
        color: #ffffff;
        font: 600 13px/1.4 system-ui, -apple-system, sans-serif;
        padding: 8px 18px;
        border-radius: 9999px;
        box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.15);
        pointer-events: none;
        display: flex;
        align-items: center;
        gap: 8px;
        z-index: 10;
        letter-spacing: 0.2px;
      }
      .instruction-pill .badge {
        background: #0284c7;
        color: #fff;
        padding: 2px 7px;
        border-radius: 4px;
        font-size: 11px;
        font-weight: 700;
        text-transform: uppercase;
      }
      .selection-box {
        position: fixed;
        border: 2px dashed #38bdf8;
        background: rgba(56, 189, 248, 0.12);
        box-shadow: 0 0 0 99999px rgba(15, 23, 42, 0.6);
        pointer-events: none;
        display: none;
        border-radius: 4px;
      }
      .size-indicator {
        position: absolute;
        bottom: -28px;
        right: 0;
        background: rgba(15, 23, 42, 0.95);
        color: #93c5fd;
        font: 600 11px system-ui, sans-serif;
        padding: 3px 7px;
        border-radius: 4px;
        white-space: nowrap;
        border: 1px solid rgba(56, 189, 248, 0.3);
      }
    </style>
    <div class="backdrop" id="backdrop">
      <div class="instruction-pill">
        <span class="badge">Capturar Ecrã</span>
        <span>Arraste o rato para selecionar a área do ecrã • Pressione <strong>[ESC]</strong> para cancelar</span>
      </div>
      <div class="selection-box" id="box">
        <div class="size-indicator" id="size">0 × 0 px</div>
      </div>
    </div>
  `;

  const backdrop = shadow.getElementById('backdrop')!;
  const box = shadow.getElementById('box')!;
  const size = shadow.getElementById('size')!;

  document.documentElement.appendChild(host);

  let isDragging = false;
  let startX = 0;
  let startY = 0;
  let currentX = 0;
  let currentY = 0;

  const cleanup = () => {
    window.removeEventListener('keydown', onKeyDown);
    if (host.parentElement) {
      host.parentElement.removeChild(host);
    }
  };

  const onKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      cleanup();
    }
  };
  window.addEventListener('keydown', onKeyDown);

  const onStart = (clientX: number, clientY: number) => {
    isDragging = true;
    startX = clientX;
    startY = clientY;
    currentX = clientX;
    currentY = clientY;

    box.style.display = 'block';
    box.style.left = `${startX}px`;
    box.style.top = `${startY}px`;
    box.style.width = '0px';
    box.style.height = '0px';
  };

  const onMove = (clientX: number, clientY: number) => {
    if (!isDragging) return;
    currentX = clientX;
    currentY = clientY;

    const left = Math.min(startX, currentX);
    const top = Math.min(startY, currentY);
    const width = Math.abs(currentX - startX);
    const height = Math.abs(currentY - startY);

    box.style.left = `${left}px`;
    box.style.top = `${top}px`;
    box.style.width = `${width}px`;
    box.style.height = `${height}px`;
    size.textContent = `${Math.round(width)} × ${Math.round(height)} px`;
  };

  const onEnd = () => {
    if (!isDragging) return;
    isDragging = false;

    const left = Math.min(startX, currentX);
    const top = Math.min(startY, currentY);
    const width = Math.abs(currentX - startX);
    const height = Math.abs(currentY - startY);

    // Ignora seleções muito pequenas (ex: cliques acidentais)
    if (width < 25 || height < 25) {
      cleanup();
      return;
    }

    // Remove imediatamente o overlay antes de capturar a tela para não aparecer o fundo escuro
    cleanup();

    // Pequeno atraso para o navegador repintar sem o overlay
    requestAnimationFrame(() => {
      setTimeout(() => {
        chrome.runtime.sendMessage({ type: 'CAPTURE_TAB' }, (response) => {
          if (!response || !response.ok || !response.dataUrl) {
            console.error('[AI Question Assistant] Falha ao capturar ecrã:', response?.error);
            return;
          }

          const img = new Image();
          img.onload = () => {
            const dpr = window.devicePixelRatio || 1;
            const canvas = document.createElement('canvas');
            canvas.width = Math.max(1, Math.round(width * dpr));
            canvas.height = Math.max(1, Math.round(height * dpr));
            const ctx = canvas.getContext('2d');
            if (!ctx) return;

            // Recorta a área selecionada pelo usuário
            ctx.drawImage(
              img,
              Math.round(left * dpr),
              Math.round(top * dpr),
              Math.round(width * dpr),
              Math.round(height * dpr),
              0,
              0,
              canvas.width,
              canvas.height,
            );

            const croppedDataUrl = canvas.toDataURL('image/png');
            onCapture(croppedDataUrl);
          };
          img.src = response.dataUrl;
        });
      }, 50);
    });
  };

  // Suporte a Rato (PC)
  backdrop.addEventListener('mousedown', (e) => {
    if (e.button !== 0) return;
    onStart(e.clientX, e.clientY);
  });
  window.addEventListener('mousemove', (e) => onMove(e.clientX, e.clientY));
  window.addEventListener('mouseup', onEnd);

  // Suporte a Toque (Telemóvel / Tablet)
  backdrop.addEventListener(
    'touchstart',
    (e) => {
      if (e.touches.length === 1) {
        onStart(e.touches[0].clientX, e.touches[0].clientY);
      }
    },
    { passive: true },
  );
  window.addEventListener(
    'touchmove',
    (e) => {
      if (e.touches.length === 1) {
        onMove(e.touches[0].clientX, e.touches[0].clientY);
      }
    },
    { passive: true },
  );
  window.addEventListener('touchend', onEnd);
}
