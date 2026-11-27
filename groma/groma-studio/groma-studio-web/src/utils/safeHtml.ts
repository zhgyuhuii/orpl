import DOMPurify from 'dompurify';

export function sanitizeNoteHtml(rawMarkup: string): string {
  return DOMPurify.sanitize(rawMarkup, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'p', 'br', 'ul', 'ol', 'li', 'code', 'span'],
    ALLOWED_ATTR: ['class']
  });
}
