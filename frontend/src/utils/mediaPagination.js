export const MEDIA_PAGE_SIZE = 40;

export function mediaPagePath(path, cursor, limit = MEDIA_PAGE_SIZE) {
  const params = new URLSearchParams({ limit: String(limit) });
  if (cursor) {
    params.set('cursor', cursor);
  }
  return `${path}?${params.toString()}`;
}

export function appendUniqueMedia(currentItems, nextItems) {
  const knownIds = new Set(currentItems.map((media) => media.mediaId));
  return [...currentItems, ...nextItems.filter((media) => !knownIds.has(media.mediaId))];
}

export function addLoadedMediaToSelection(selectedIds, mediaItems) {
  const next = new Set(selectedIds);
  mediaItems.forEach((media) => next.add(media.mediaId));
  return next;
}

export function canLoadNextMediaPage({ hasMore, nextCursor, isLoading }) {
  return Boolean(hasMore && nextCursor && !isLoading);
}
