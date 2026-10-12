import fs from 'node:fs';
import path from 'node:path';
import dotenv from 'dotenv';

// Garante carregamento de .env tanto quando iniciado da raiz do monorepo quanto dentro de server/
const possibleEnvPaths = [
  path.resolve('.env'),
  path.resolve('server/.env'),
  path.resolve('../server/.env'),
];
const serverEnvPath = possibleEnvPaths.find((p) => fs.existsSync(p)) ?? path.resolve('.env');
dotenv.config({ path: serverEnvPath });

import { createApp } from './app';
import { createProviderFromEnv } from './services/ai';

const port = Number(process.env.PORT ?? 3001);
const host = process.env.HOST ?? '0.0.0.0';

let runtimeProviderOverride: string | null = null;

const getProvider = () => {
  if (fs.existsSync(serverEnvPath)) {
    dotenv.config({ path: serverEnvPath, override: true });
  }
  const env = { ...process.env };
  if (runtimeProviderOverride) {
    env.AI_PROVIDER = runtimeProviderOverride;
  }
  return createProviderFromEnv(env);
};

const setProvider = (newProvider: string) => {
  runtimeProviderOverride = newProvider;
  process.env.AI_PROVIDER = newProvider;
  try {
    if (fs.existsSync(serverEnvPath)) {
      let content = fs.readFileSync(serverEnvPath, 'utf8');
      if (/^AI_PROVIDER=.*$/m.test(content)) {
        content = content.replace(/^AI_PROVIDER=.*$/m, `AI_PROVIDER=${newProvider}`);
      } else {
        content = `AI_PROVIDER=${newProvider}\n` + content;
      }
      fs.writeFileSync(serverEnvPath, content, 'utf8');
      console.log(`[server] Provedor de IA alterado para: ${newProvider} (salvo em ${serverEnvPath})`);
    }
  } catch (err) {
    console.warn('[server] Falha ao persistir AI_PROVIDER:', (err as Error).message);
  }
};

const setSettings = (settings: { geminiApiKey?: string; provider?: string; model?: string }) => {
  if (settings.provider) {
    setProvider(settings.provider);
  }
  try {
    if (fs.existsSync(serverEnvPath)) {
      let content = fs.readFileSync(serverEnvPath, 'utf8');
      if (settings.geminiApiKey !== undefined) {
        process.env.GEMINI_API_KEY = settings.geminiApiKey;
        if (/^GEMINI_API_KEY=.*$/m.test(content)) {
          content = content.replace(/^GEMINI_API_KEY=.*$/m, `GEMINI_API_KEY=${settings.geminiApiKey}`);
        } else {
          content += `\nGEMINI_API_KEY=${settings.geminiApiKey}`;
        }
      }
      if (settings.model) {
        process.env.AI_MODEL = settings.model;
        if (/^AI_MODEL=.*$/m.test(content)) {
          content = content.replace(/^AI_MODEL=.*$/m, `AI_MODEL=${settings.model}`);
        } else {
          content += `\nAI_MODEL=${settings.model}`;
        }
      }
      fs.writeFileSync(serverEnvPath, content, 'utf8');
      console.log('[server] Configurações atualizadas e persistidas em .env');
    }
  } catch (err) {
    console.warn('[server] Falha ao persistir configurações:', (err as Error).message);
  }
};

const app = createApp({
  provider: getProvider,
  setProvider,
  setSettings,
  corsOrigins: process.env.CORS_ORIGINS,
  serveTestPage: process.env.NODE_ENV !== 'production',
});

app.listen(port, host, () => {
  const currentProvider = getProvider();
  console.log(`[server] http://${host}:${port}  provider=${currentProvider.name}`);
  console.log(`[server] 🚀 Assistente IA Web:  http://${host}:${port}/`);
  console.log(`[server] 📝 Simulado de testes: http://${host}:${port}/test-page/`);
});
