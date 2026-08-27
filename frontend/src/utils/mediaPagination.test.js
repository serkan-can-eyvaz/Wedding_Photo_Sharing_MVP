import assert from 'node:assert/strict';
import test from 'node:test';
import {
  addLoadedMediaToSelection,
  appendUniqueMedia,
  canLoadNextMediaPage,
  mediaPagePath,
} from './mediaPagination.js';

test('builds the first and next media page request paths', () => {
  assert.equal(mediaPagePath('/api/events/event-1/media'), '/api/events/event-1/media?limit=40');
  assert.equal(
    mediaPagePath('/api/events/event-1/media', 'cursor-value', 30),
    '/api/events/event-1/media?limit=30&cursor=cursor-value',
  );
});

test('appends a next page once without duplicating media cards', () => {
  const firstPage = [{ mediaId: 'one' }, { mediaId: 'two' }];
  const nextPage = [{ mediaId: 'two' }, { mediaId: 'three' }];

  assert.deepEqual(appendUniqueMedia(firstPage, nextPage), [
    { mediaId: 'one' },
    { mediaId: 'two' },
    { mediaId: 'three' },
  ]);
});

test('keeps selections from earlier pages while selecting loaded media', () => {
  assert.deepEqual(
    [...addLoadedMediaToSelection(new Set(['one']), [{ mediaId: 'two' }, { mediaId: 'three' }])],
    ['one', 'two', 'three'],
  );
});

test('only permits one next-page request while more media remains', () => {
  assert.equal(canLoadNextMediaPage({ hasMore: true, nextCursor: 'cursor', isLoading: false }), true);
  assert.equal(canLoadNextMediaPage({ hasMore: true, nextCursor: 'cursor', isLoading: true }), false);
  assert.equal(canLoadNextMediaPage({ hasMore: false, nextCursor: null, isLoading: false }), false);
});
