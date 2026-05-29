import * as fs from 'fs';
import * as path from 'path';

export interface DocMetadata {
  title: string;
  source_type: string;
  source_url?: string;
  trust_level: 'high' | 'medium' | 'low';
  tags: string[];
}

export interface TrustedDoc {
  filePath: string;
  metadata: DocMetadata;
  content: string;
}

function parseFrontmatter(raw: string): { metadata: Partial<DocMetadata>; body: string } {
  const match = raw.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n?([\s\S]*)$/);
  if (!match) return { metadata: {}, body: raw.trim() };

  const yamlBlock = match[1];
  const body = match[2].trim();
  const meta: Record<string, unknown> = {};

  for (const line of yamlBlock.split('\n')) {
    const colonIdx = line.indexOf(':');
    if (colonIdx < 0) continue;
    const key = line.slice(0, colonIdx).trim();
    const val = line.slice(colonIdx + 1).trim();
    if (val.startsWith('[') && val.endsWith(']')) {
      meta[key] = val
        .slice(1, -1)
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
    } else {
      meta[key] = val;
    }
  }

  return { metadata: meta as Partial<DocMetadata>, body };
}

function walkDir(dir: string, out: string[]): void {
  if (!fs.existsSync(dir)) return;
  for (const entry of fs.readdirSync(dir)) {
    const full = path.join(dir, entry);
    if (fs.statSync(full).isDirectory()) {
      walkDir(full, out);
    } else if (entry.endsWith('.md') && entry !== 'README.md') {
      out.push(full);
    }
  }
}

export function loadTrustedDocs(sourcesDir: string): TrustedDoc[] {
  const paths: string[] = [];
  walkDir(sourcesDir, paths);

  const docs: TrustedDoc[] = [];
  for (const filePath of paths) {
    try {
      const raw = fs.readFileSync(filePath, 'utf-8');
      const { metadata, body } = parseFrontmatter(raw);
      if (!metadata.title || !body) continue;
      docs.push({
        filePath,
        metadata: {
          title: metadata.title ?? path.basename(filePath, '.md'),
          source_type: metadata.source_type ?? 'curated',
          source_url: metadata.source_url,
          trust_level: metadata.trust_level ?? 'high',
          tags: metadata.tags ?? [],
        },
        content: body,
      });
    } catch {
      // skip unreadable files
    }
  }
  return docs;
}
