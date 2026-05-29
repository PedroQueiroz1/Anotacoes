import { config } from '../config';

export function sanitizeTerm(raw: unknown): string {
  if (typeof raw !== 'string') throw new Error('term must be a string');
  const term = raw.trim().replace(/\s+/g, ' ');
  if (term.length < config.concept.minTermLength) {
    throw new Error(`term too short (min ${config.concept.minTermLength} chars)`);
  }
  if (term.length > config.concept.maxTermLength) {
    throw new Error(`term too long (max ${config.concept.maxTermLength} chars)`);
  }
  // Reject if it looks like full note content (many sentences or lines)
  if ((term.match(/[.!?]/g) ?? []).length > 4) {
    throw new Error('term looks like full content, not a single concept');
  }
  if (term.split('\n').length > 3) {
    throw new Error('term must be a single concept, not multiline content');
  }
  return term;
}
