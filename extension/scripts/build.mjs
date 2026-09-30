// Constrói popup (React), content script (IIFE) e service worker (IIFE) para extension/dist.
import { copyFileSync, cpSync, mkdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import react from '@vitejs/plugin-react';
import { build } from 'vite';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const watch = process.argv.includes('--watch');
const apiUrl = process.env.VITE_API_URL ?? 'http://localhost:3001';

const common = { root, configFile: false, logLevel: 'info', define: { __API_URL__: JSON.stringify(apiUrl) } };

const scriptBuild = (name, entry) =>
  build({
    ...common,
    build: {
      outDir: 'dist',
      emptyOutDir: false,
      watch: watch ? {} : null,
      minify: false,
      lib: { entry: path.join(root, entry), name, formats: ['iife'], fileName: () => `${name}.js` },
    },
  });

function copyStatic() {
  mkdirSync(path.join(root, 'dist'), { recursive: true });
  copyFileSync(path.join(root, 'manifest.json'), path.join(root, 'dist/manifest.json'));
  cpSync(path.join(root, 'public/icons'), path.join(root, 'dist/icons'), { recursive: true });
}

// 1) popup (limpa dist), 2) content, 3) background, 4) manifest + ícones
await build({
  ...common,
  base: './',
  plugins: [react()],
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    watch: watch ? {} : null,
    rollupOptions: { input: { popup: path.join(root, 'popup.html') } },
  },
});
await scriptBuild('content', 'src/content/content.ts');
await scriptBuild('background', 'src/background/serviceWorker.ts');
copyStatic();
console.log(`\n✔ Extensão construída em extension/dist (API: ${apiUrl})${watch ? ' — a observar alterações…' : ''}`);
