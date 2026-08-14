import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AdminApiError, getAdminEvent, getEventMedia } from '../api/adminApi.js';
import { clearAdminSession } from '../auth/adminSession.js';
import MediaGalleryCard from '../components/MediaGalleryCard.jsx';

export default function AdminGalleryPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [state, setState] = useState({ status: 'loading', event: null, media: [] });
  const [selectedIds, setSelectedIds] = useState(new Set());

  useEffect(() => {
    let cancelled = false;
    setSelectedIds(new Set());
    setState({ status: 'loading', event: null, media: [] });

    Promise.all([getAdminEvent(id), getEventMedia(id)])
      .then(([event, media]) => {
        if (!cancelled) {
          setState({ status: 'ready', event, media });
        }
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }
        if (error instanceof AdminApiError && error.status === 401) {
          clearAdminSession();
          navigate('/admin/login', { replace: true });
          return;
        }
        setState({ status: error instanceof AdminApiError && error.status === 404 ? 'not-found' : 'error', event: null, media: [] });
      });

    return () => {
      cancelled = true;
    };
  }, [id, navigate]);

  const handleLogout = () => {
    clearAdminSession();
    navigate('/admin/login', { replace: true });
  };

  const toggleSelection = (mediaId) => {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (next.has(mediaId)) {
        next.delete(mediaId);
      } else {
        next.add(mediaId);
      }
      return next;
    });
  };

  const selectAll = () => setSelectedIds(new Set(state.media.map((media) => media.mediaId)));
  const clearSelection = () => setSelectedIds(new Set());

  return (
    <section className="admin-page">
      <header className="admin-page-header">
        <Link to="/admin">Etkinliklere dön</Link>
        <button type="button" className="secondary-button" onClick={handleLogout}>Çıkış yap</button>
      </header>
      {state.status === 'loading' && <p className="guest-page-state">Galeri yükleniyor...</p>}
      {state.status === 'not-found' && <p className="guest-page-state">Etkinlik bulunamadı.</p>}
      {state.status === 'error' && <p className="guest-error" role="alert">Galeri alınamadı. Lütfen tekrar deneyin.</p>}
      {state.status === 'ready' && (
        <>
          <header className="gallery-header">
            <h1>{state.event.name}</h1>
            <p>{state.media.length} medya</p>
          </header>
          {state.media.length > 0 && (
            <div className="gallery-selection-toolbar">
              <button type="button" className="secondary-button" onClick={selectAll}>Tümünü seç</button>
              <button type="button" className="secondary-button" onClick={clearSelection} disabled={selectedIds.size === 0}>Seçimi temizle</button>
              <span>{selectedIds.size} seçili</span>
            </div>
          )}
          {state.media.length === 0 ? (
            <p>Bu etkinlikte henüz medya yok.</p>
          ) : (
            <div className="media-gallery-grid">
              {state.media.map((media) => (
                <MediaGalleryCard
                  key={media.mediaId}
                  media={media}
                  selected={selectedIds.has(media.mediaId)}
                  onToggle={toggleSelection}
                />
              ))}
            </div>
          )}
        </>
      )}
    </section>
  );
}
