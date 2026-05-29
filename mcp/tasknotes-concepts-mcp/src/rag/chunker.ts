import { TrustedDoc } from './trusted-docs-loader';

export interface DocChunk {
  docTitle: string;
  tags: string[];
  trustLevel: string;
  sourceUrl?: string;
  text: string;
}

const MAX_CHUNK_CHARS = 900;

function splitByHeadings(content: string): string[] {
  // Split on markdown headings (##, ###, ####)
  const sections = content.split(/(?=^#{2,4}\s)/m).filter((s) => s.trim());
  return sections;
}

function splitByParagraphs(text: string): string[] {
  return text.split(/\n{2,}/).filter((p) => p.trim());
}

export function chunkDoc(doc: TrustedDoc): DocChunk[] {
  const sections = splitByHeadings(doc.content);
  const chunks: DocChunk[] = [];

  for (const section of sections) {
    if (section.length <= MAX_CHUNK_CHARS) {
      chunks.push({
        docTitle: doc.metadata.title,
        tags: doc.metadata.tags,
        trustLevel: doc.metadata.trust_level,
        sourceUrl: doc.metadata.source_url,
        text: section.trim(),
      });
    } else {
      // Too long: split further by paragraph
      for (const para of splitByParagraphs(section)) {
        if (para.trim().length < 20) continue;
        chunks.push({
          docTitle: doc.metadata.title,
          tags: doc.metadata.tags,
          trustLevel: doc.metadata.trust_level,
          sourceUrl: doc.metadata.source_url,
          text: para.trim(),
        });
      }
    }
  }

  // If the whole doc has no headings, treat it as one chunk
  if (chunks.length === 0 && doc.content.trim()) {
    chunks.push({
      docTitle: doc.metadata.title,
      tags: doc.metadata.tags,
      trustLevel: doc.metadata.trust_level,
      sourceUrl: doc.metadata.source_url,
      text: doc.content.trim().slice(0, MAX_CHUNK_CHARS),
    });
  }

  return chunks;
}

export function chunkAllDocs(docs: TrustedDoc[]): DocChunk[] {
  return docs.flatMap(chunkDoc);
}
