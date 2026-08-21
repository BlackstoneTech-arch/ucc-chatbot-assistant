import { describe, it, expect } from 'vitest';
import { env } from '../src/config/env.js';

describe('Configuration', () => {
  it('should have required environment variables', () => {
    expect(env.PORT).toBeDefined();
    expect(env.NODE_ENV).toBeDefined();
    expect(env.JWT_SECRET).toBeDefined();
  });

  it('should have valid default port', () => {
    expect(env.PORT).toBeGreaterThan(0);
    expect(env.PORT).toBeLessThan(65536);
  });
});

describe('Text Chunking', () => {
  it('should split text into chunks', () => {
    const text = 'Line 1\nLine 2\nLine 3\nLine 4\nLine 5';
    const chunks = chunkText(text, 15, 0);

    expect(chunks.length).toBeGreaterThan(0);
    chunks.forEach(chunk => {
      expect(chunk.length).toBeGreaterThan(0);
    });
  });
});

function chunkText(text: string, chunkSize: number, overlap: number): string[] {
  const chunks: string[] = [];
  let start = 0;

  while (start < text.length) {
    let end = start + chunkSize;
    if (end < text.length) {
      const lastNewline = text.lastIndexOf('\n', end);
      if (lastNewline > start) {
        end = lastNewline + 1;
      }
    }
    const chunk = text.slice(start, end).trim();
    if (chunk.length > 0) {
      chunks.push(chunk);
    }
    start = end - overlap;
    if (start >= text.length) break;
  }

  return chunks;
}
