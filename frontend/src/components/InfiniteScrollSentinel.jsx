import { useEffect, useRef } from 'react';

export default function InfiniteScrollSentinel({ hasMore, isLoading, error, onLoadMore }) {
  const sentinelRef = useRef(null);

  useEffect(() => {
    if (!hasMore || isLoading || !sentinelRef.current || typeof IntersectionObserver === 'undefined') {
      return undefined;
    }

    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        onLoadMore();
      }
    }, { rootMargin: '400px 0px' });
    observer.observe(sentinelRef.current);
    return () => observer.disconnect();
  }, [hasMore, isLoading, onLoadMore]);

  if (!hasMore) {
    return null;
  }

  return (
    <div className="gallery-load-more" ref={sentinelRef}>
      {error ? <p className="guest-error" role="alert">Daha fazla medya yüklenemedi. <button type="button" className="secondary-button" onClick={onLoadMore}>Tekrar dene</button></p> : null}
      {isLoading ? <p role="status">Daha fazla medya yükleniyor...</p> : <button type="button" className="secondary-button" onClick={onLoadMore}>Daha fazla yükle</button>}
    </div>
  );
}
