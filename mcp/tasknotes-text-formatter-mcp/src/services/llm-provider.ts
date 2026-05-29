import { config } from '../config';

export interface LlmFormatResult {
  formattedHtml: string;
  operationsSummary: string[];
}

/**
 * Stub for optional LLM-based formatting.
 * When LLM_PROVIDER=none (default), always returns null so the
 * local rule-based formatter is used instead.
 */
export async function llmFormat(
  _titlePlain: string,
  _contentPlain: string,
): Promise<LlmFormatResult | null> {
  if (config.llm.provider === 'none' || !config.llm.apiKey) return null;
  // Future: call OpenAI / Anthropic / local LLM
  return null;
}
