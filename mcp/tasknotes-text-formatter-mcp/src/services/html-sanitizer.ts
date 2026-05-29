const ALLOWED_TAGS = new Set([
  'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'mark', 'span',
  'h1', 'h2', 'h3', 'ul', 'ol', 'li', 'a', 'blockquote',
]);

const ALLOWED_ATTR: Record<string, Set<string>> = {
  a: new Set(['href', 'target', 'rel']),
  span: new Set(['style']),
  p: new Set(['style']),
  li: new Set(['style']),
  mark: new Set(['style']),
};

const SAFE_STYLE_PROPS = /^(color|background-color|text-align|font-weight|text-decoration|margin-bottom|font-size):/i;
const DANGEROUS_PATTERNS = [
  /javascript:/gi,
  /expression\s*\(/gi,
  /on\w+\s*=/gi,
];

function stripDangerousStyle(style: string): string {
  const parts = style.split(';').map(s => s.trim()).filter(Boolean);
  return parts.filter(p => SAFE_STYLE_PROPS.test(p)).join('; ');
}

export function sanitizeHtml(html: string): string {
  if (!html) return '';

  let result = html;

  // Remove script/object/embed/iframe blocks entirely
  result = result.replace(/<(script|iframe|object|embed|form|input|style)[^>]*>[\s\S]*?<\/\1>/gi, '');
  result = result.replace(/<(script|iframe|object|embed|form|input|style)[^>]*/gi, '');

  // Remove event handlers and javascript: URLs
  for (const pattern of DANGEROUS_PATTERNS) {
    result = result.replace(pattern, '');
  }

  // Strip disallowed tags but keep their text content
  result = result.replace(/<\/?([a-zA-Z][a-zA-Z0-9]*)[^>]*>/g, (match, tag) => {
    const lowerTag = tag.toLowerCase();
    if (!ALLOWED_TAGS.has(lowerTag)) {
      return ''; // remove tag entirely (including its text wrapper)
    }
    return match;
  });

  // Strip disallowed attributes from allowed tags
  result = result.replace(/<([a-zA-Z][a-zA-Z0-9]*)((?:\s+[^>]*)?)>/g, (_match, tag, attrs) => {
    const lowerTag = tag.toLowerCase();
    if (!ALLOWED_TAGS.has(lowerTag)) return '';

    const allowedForTag = ALLOWED_ATTR[lowerTag];
    if (!allowedForTag || !attrs.trim()) return `<${lowerTag}>`;

    const safeAttrs = attrs.replace(
      /\s+([\w-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]*)))?/g,
      (_: string, attrName: string, dq: string, sq: string, unq: string) => {
        const name = attrName.toLowerCase();
        if (!allowedForTag.has(name)) return '';
        const val = dq ?? sq ?? unq ?? '';

        if (name === 'href') {
          if (/^javascript:/i.test(val.trim())) return '';
          return ` ${name}="${val}"`;
        }
        if (name === 'style') {
          const safe = stripDangerousStyle(val);
          return safe ? ` style="${safe}"` : '';
        }
        if (name === 'target') return ` target="_blank"`;
        if (name === 'rel') return ` rel="noopener noreferrer"`;
        return ` ${name}="${val}"`;
      }
    );

    return `<${lowerTag}${safeAttrs}>`;
  });

  return result;
}

export function htmlToPlainText(html: string): string {
  return html
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<\/li>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&nbsp;/g, ' ')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}
