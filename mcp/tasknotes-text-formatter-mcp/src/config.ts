import * as fs from 'fs';
import * as path from 'path';

const projectRoot = path.join(__dirname, '..');

function loadDotEnv(): void {
  const envPath = path.join(projectRoot, '.env');
  if (!fs.existsSync(envPath)) return;
  for (const line of fs.readFileSync(envPath, 'utf-8').split('\n')) {
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
  port: parseInt(process.env.MCP_PORT ?? '8790', 10),
  llm: {
    provider: process.env.LLM_PROVIDER ?? 'none',
    apiKey: process.env.LLM_API_KEY ?? '',
  },
  formatter: {
    mode: process.env.FORMATTER_MODE ?? 'local-rules-first',
    maxNoteChars: parseInt(process.env.MAX_NOTE_CHARS ?? '12000', 10),
  },
};
