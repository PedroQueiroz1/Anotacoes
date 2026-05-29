import express, { Request, Response, NextFunction } from 'express';
import { config } from './config';
import { loadTrustedDocs } from './rag/trusted-docs-loader';
import { chunkAllDocs } from './rag/chunker';
import { DocChunk } from './rag/chunker';
import { explainProgrammingTerm, ExplainInput } from './tools/explain-programming-term.tool';
import { searchTrustedDocs } from './tools/search-trusted-docs.tool';
import { searchSavedConcepts } from './tools/search-saved-concepts.tool';
import { webSearchProgrammingTerm } from './tools/web-search-programming-term.tool';
import { sanitizeTerm } from './security/sanitize';

// ── Load RAG documents once at startup ────────────────────────────────────────

let chunks: DocChunk[] = [];

function loadRag(): void {
  if (!config.rag.enabled) {
    console.log('[mcp] RAG disabled');
    return;
  }
  try {
    const docs = loadTrustedDocs(config.rag.sourcesDir);
    chunks = chunkAllDocs(docs);
    console.log(`[mcp] RAG loaded: ${docs.length} docs → ${chunks.length} chunks (sourcesDir=${config.rag.sourcesDir})`);
  } catch (err) {
    console.error('[mcp] Failed to load RAG documents:', err);
  }
}

// ── Express app ───────────────────────────────────────────────────────────────

const app = express();
app.use(express.json({ limit: '32kb' }));

// Only accept requests from localhost (internal service)
app.use((_req: Request, res: Response, next: NextFunction) => {
  const ip = _req.socket.remoteAddress ?? '';
  if (ip === '::1' || ip === '127.0.0.1' || ip === '::ffff:127.0.0.1') {
    return next();
  }
  res.status(403).json({ error: 'forbidden: internal service only' });
});

// ── Health ─────────────────────────────────────────────────────────────────────

app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    rag: { enabled: config.rag.enabled, chunks: chunks.length },
    internet: { enabled: config.internet.enabled },
  });
});

// ── Main endpoint: explain a programming term ─────────────────────────────────

app.post('/internal/concepts/explain', async (req: Request, res: Response) => {
  const startMs = Date.now();
  try {
    const input = req.body as ExplainInput;
    const result = await explainProgrammingTerm(input, chunks);
    const ms = Date.now() - startMs;

    if (result.sourceType === 'not_found' || !result.summary) {
      console.log(`[mcp] explain term="${input.term}" sourceType=not_found ms=${ms}`);
      return res.status(404).json({ found: false, term: result.term });
    }

    console.log(`[mcp] explain term="${input.term}" sourceType=${result.sourceType} confidence=${result.confidence} ms=${ms}`);
    return res.json({ found: true, ...result });
  } catch (err) {
    const message = err instanceof Error ? err.message : 'unknown error';
    console.error(`[mcp] explain error: ${message}`);
    return res.status(400).json({ error: message });
  }
});

// ── Tool: search trusted docs ─────────────────────────────────────────────────

app.post('/internal/tools/search-trusted-docs', (req: Request, res: Response) => {
  try {
    const result = searchTrustedDocs(req.body, chunks);
    res.json(result);
  } catch (err) {
    const message = err instanceof Error ? err.message : 'unknown error';
    res.status(400).json({ error: message });
  }
});

// ── Tool: search saved concepts ───────────────────────────────────────────────

app.post('/internal/tools/search-saved-concepts', async (req: Request, res: Response) => {
  try {
    const result = await searchSavedConcepts(req.body);
    res.json(result);
  } catch (err) {
    const message = err instanceof Error ? err.message : 'unknown error';
    res.status(400).json({ error: message });
  }
});

// ── Tool: web search ──────────────────────────────────────────────────────────

app.post('/internal/tools/web-search', async (req: Request, res: Response) => {
  try {
    const result = await webSearchProgrammingTerm(req.body);
    res.json(result);
  } catch (err) {
    const message = err instanceof Error ? err.message : 'unknown error';
    res.status(400).json({ error: message });
  }
});

// ── Term validation tool (sanitize only, no lookup) ──────────────────────────

app.post('/internal/tools/validate-term', (req: Request, res: Response) => {
  try {
    const term = sanitizeTerm(req.body?.term);
    res.json({ valid: true, term });
  } catch (err) {
    const message = err instanceof Error ? err.message : 'invalid term';
    res.status(400).json({ valid: false, error: message });
  }
});

// ── Start ─────────────────────────────────────────────────────────────────────

loadRag();

app.listen(config.port, '127.0.0.1', () => {
  console.log(`[mcp] TaskNotes Concepts MCP listening on http://127.0.0.1:${config.port}`);
  console.log(`[mcp] Internet fallback: ${config.internet.enabled ? 'ENABLED' : 'disabled'}`);
});
