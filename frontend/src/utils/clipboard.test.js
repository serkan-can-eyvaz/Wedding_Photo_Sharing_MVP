import assert from 'node:assert/strict';
import test from 'node:test';
import { copyTextToClipboard } from './clipboard.js';

function setGlobal(name, value) {
  const descriptor = Object.getOwnPropertyDescriptor(globalThis, name);
  Object.defineProperty(globalThis, name, { configurable: true, writable: true, value });
  return () => {
    if (descriptor) {
      Object.defineProperty(globalThis, name, descriptor);
    } else {
      delete globalThis[name];
    }
  };
}

function createDocument(copyResult) {
  const textarea = {
    value: '',
    style: {},
    setAttribute() {},
    select() {},
    setSelectionRange() {},
    remove() { this.removed = true; },
  };

  return {
    textarea,
    document: {
      body: { appendChild() {} },
      createElement() { return textarea; },
      execCommand(command) { return command === 'copy' && copyResult; },
    },
  };
}

test('uses the modern Clipboard API when it succeeds', async () => {
  const calls = [];
  const restoreNavigator = setGlobal('navigator', { clipboard: { writeText: async (text) => calls.push(text) } });
  const { document } = createDocument(true);
  const restoreDocument = setGlobal('document', document);

  try {
    assert.equal(await copyTextToClipboard('https://example.test/gallery/token'), true);
    assert.deepEqual(calls, ['https://example.test/gallery/token']);
  } finally {
    restoreDocument();
    restoreNavigator();
  }
});

test('uses the fallback when the Clipboard API rejects on an HTTP origin', async () => {
  const restoreNavigator = setGlobal('navigator', { clipboard: { writeText: async () => { throw new Error('NotAllowedError'); } } });
  const { document, textarea } = createDocument(true);
  const restoreDocument = setGlobal('document', document);

  try {
    assert.equal(await copyTextToClipboard('https://example.test/gallery/token'), true);
    assert.equal(textarea.value, 'https://example.test/gallery/token');
    assert.equal(textarea.removed, true);
  } finally {
    restoreDocument();
    restoreNavigator();
  }
});

test('returns false when both Clipboard API and fallback fail', async () => {
  const restoreNavigator = setGlobal('navigator', { clipboard: { writeText: async () => { throw new Error('NotAllowedError'); } } });
  const { document } = createDocument(false);
  const restoreDocument = setGlobal('document', document);

  try {
    assert.equal(await copyTextToClipboard('https://example.test/gallery/token'), false);
  } finally {
    restoreDocument();
    restoreNavigator();
  }
});
