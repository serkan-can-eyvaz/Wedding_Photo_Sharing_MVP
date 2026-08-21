import assert from 'node:assert/strict';
import test from 'node:test';
import { toUserFacingUrl } from './userFacingUrl.js';

test('converts the technical root domain while preserving a guest path', () => {
  assert.equal(
    toUserFacingUrl('https://xn--aramzdan-wkb.com/e/abc'),
    'https://aramızdan.com/e/abc',
  );
});

test('preserves viewer paths, queries, and hashes', () => {
  assert.equal(
    toUserFacingUrl('https://xn--aramzdan-wkb.com/gallery/xyz?source=admin#photos'),
    'https://aramızdan.com/gallery/xyz?source=admin#photos',
  );
});

test('leaves unrelated domains and empty values unchanged', () => {
  assert.equal(toUserFacingUrl('https://example.com/e/abc'), 'https://example.com/e/abc');
  assert.equal(toUserFacingUrl(''), '');
  assert.equal(toUserFacingUrl(null), null);
  assert.equal(toUserFacingUrl(undefined), undefined);
});
