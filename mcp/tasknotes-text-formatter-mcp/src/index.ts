import express, { Request, Response, NextFunction } from 'express';
import { config } from './config';
import { formatNoteTextTool } from './tools/format-note-text.tool';
import { analyzeNoteStructure } from './tools/analyze-note-structure.tool';

const app = express();
app.use(express.json({ limit: '128kb' }));

// Only accept requests from localhost
app.use((_req: Request, res: Response, next: NextFunction) => {
  const ip = _req.socket.remoteAddress ?? '';
  if (ip === '::1' || ip === '127.0.0.1' || ip === '::ffff:127.0.0.1') return next();
  res.status(403).json({ error: 'forbidden: internal service only' });
});

// ── Health ─────────────────────────────────────────────────────────────────────

app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    formatter: { mode: config.formatter.mode, maxNoteChars: config.formatter.maxNoteChars },
    llm: { provider: config.llm.provider, hasKey: !!config.llm.apiKey },
  });
});

// ── Main endpoint: format a note ──────────────────────────────────────────────

app.post('/internal/format-note', async (req: Request, res: Response) => {
  const startMs = Date.now();
  try {
    const result = await formatNoteTextTool(req.body);
    const ms = Date.now() - startMs;
    // Log only metadata, never full content
    console.log(
      `[formatter] format-note ops=${result.operationsSummary.length} ` +
      `preserved=${result.plainTextPreserved} warnings=${result.warnings.length} ms=${ms}`
    );
    res.json(result);
  } catch (err) {
    const message = err instanceof Error ? err.message : 'unknown error';
    console.error(`[formatter] format-note error: ${message}`);
    res.status(400).json({ error: message });
  }
});

// ── Tool: analyze structure ───────────────────────────────────────────────────

app.post('/internal/tools/analyze-structure', (req: Request, res: Response) => {
  try {
    res.json(analyzeNoteStructure(req.body));
  } catch (err) {
    const message = err instanceof Error ? err.message : 'unknown error';
    res.status(400).json({ error: message });
  }
});

// ── Start ─────────────────────────────────────────────────────────────────────

app.listen(config.port, '127.0.0.1', () => {
  console.log(`[formatter] TaskNotes Text Formatter MCP on http://127.0.0.1:${config.port}`);
  console.log(`[formatter] mode=${config.formatter.mode} llm=${config.llm.provider}`);
});
