import * as fs from 'fs';
import * as path from 'path';

const projectRoot = path.join(__dirname, '..');

function loadDotEnv(): void {
  const envPath = path.join(projectRoot, '.env');
  if (!fs.existsSync(envPath)) return;
  const lines = fs.readFileSync(envPath, 'utf-8').split('\n');
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq < 0) continue;
    const key = trimmed.slice(0, eq).trim();
    const val = trimmed.slice(eq + 1).trim();
    if (!process.env[key]) process.env[key] = val;
  }
}

loadDotEnv();

export const config = {
  port: parseInt(process.env.MCP_PORT ?? '8787', 10),
  tasknotes: {
    apiUrl: process.env.TASKNOTES_API_URL ?? 'http://localhost:8080',
  },
  rag: {
    enabled: process.env.RAG_ENABLED !== 'false',
    sourcesDir: process.env.RAG_SOURCES_DIR
      ? process.env.RAG_SOURCES_DIR
      : path.join(projectRoot, 'src', 'rag', 'sources'),
  },
  internet: {
    enabled: process.env.INTERNET_FALLBACK_ENABLED === 'true',
    provider: process.env.INTERNET_SEARCH_PROVIDER ?? 'none',
    apiKey: process.env.INTERNET_SEARCH_API_KEY ?? '',
  },
  concept: {
    confidenceThreshold: parseFloat(process.env.CONCEPT_CONFIDENCE_THRESHOLD ?? '0.72'),
    maxTermLength: 120,
    minTermLength: 2,
  },
};
