import { describe, it, expect } from 'vitest';
import { KnowledgeBase } from '../../src/rag/knowledgeBase.js';

describe('KnowledgeBase', () => {
  it('should be instantiable', () => {
    const kb = new KnowledgeBase({ query: async () => ({ rows: [], rowCount: 0 }) } as any);
    expect(kb).toBeDefined();
  });
});
