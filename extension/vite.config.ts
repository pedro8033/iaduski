import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// Build do popup (React). Content script e service worker são construídos
// como IIFE separados por scripts/build.mjs (MV3 não aceita módulos ES no content script).
export default defineConfig({
  base: './',
  plugins: [react()],
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    rollupOptions: { input: { popup: 'popup.html' } },
  },
});
