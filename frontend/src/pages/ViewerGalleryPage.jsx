import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import MediaGalleryCard from '../components/MediaGalleryCard.jsx';
import {
  ViewerApiError,
  downloadViewerAllMedia,
  downloadViewerSelectedMedia,
  downloadViewerSingleMedia,
  getViewerEvent,
  getViewerMedia,
} from '../api/viewerApi.js';

function ViewerState({ title, message }) {
  return (
    <section className="viewer-gallery-page viewer-gallery-state">
      <p className="viewer-gallery-eyebrow">ÖZEL GALERİ</p>
      <h1>{title}</h1>
      <p>{message}</p>
    </section>
  );
}

export default function ViewerGalleryPage() {
  const { viewerToken } = useParams();
  const [state, setState] = useState({ status: 'loading', event: null, media: [] });
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [downloadError, setDownloadError] = useState('');
  const [isDownloading, setIsDownloading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setState({ status: 'loading', event: null, media: [] });
    setSelectedIds(new Set());

    Promise.all([getViewerEvent(viewerToken), getViewerMedia(viewerToken)])
      .then(([event, media]) => {
        if (!cancelled) setState({ status: 'ready', event, media });
      })
      .catch((error) => {
        if (!cancelled) setState({ status: error instanceof ViewerApiError && error.status === 404 ? 'not-found' : 'error', event: null, media: [] });
      });

    return () => { cancelled = true; };
  }, [viewerToken]);

  const toggleSelection = (mediaId) => {
    setSelectedIds((current) => {
      const next = new Set(current);
      next.has(mediaId) ? next.delete(mediaId) : next.add(mediaId);
      return next;
    });
  };

  const saveBlob = (blob, filename) => {
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = filename;
    document.body.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
  };

  const handleDownload = async (downloadRequest, filename) => {
    setDownloadError('');
    setIsDownloading(true);
    try {
      saveBlob(await downloadRequest(), filename);
    } catch {
      setDownloadError('Medya indirilemedi. Lütfen tekrar deneyin.');
    } finally {
      setIsDownloading(false);
    }
  };

  if (state.status === 'loading') return <ViewerState title="Galeri yükleniyor." message="Anılar hazırlanıyor." />;
  if (state.status === 'not-found') return <ViewerState title="Bu galeri kullanılamıyor." message="Bağlantıyı kontrol edip tekrar deneyebilirsiniz." />;
  if (state.status === 'error') return <ViewerState title="Galeriye ulaşılamıyor." message="Lütfen bağlantınızı kontrol edip daha sonra tekrar deneyin." />;

  const selected = [...selectedIds];
  const eventDate = new Intl.DateTimeFormat('tr-TR', { dateStyle: 'long' }).format(new Date(`${state.event.eventDate}T00:00:00`));

  return (
    <section className="viewer-gallery-page">
      <header className="viewer-gallery-header">
        <p className="viewer-gallery-eyebrow">ÖZEL GALERİ</p>
        <h1>{state.event.name}</h1>
        <p>{eventDate} · {state.event.mediaCount} medya</p>
      </header>
      {state.media.length === 0 ? (
        <p className="viewer-gallery-empty">Bu etkinlikte henüz medya yok.</p>
      ) : (
        <>
          <div className="gallery-selection-toolbar viewer-gallery-toolbar">
            <span>{selected.length} seçili</span>
            <div>
              <button type="button" className="secondary-button" onClick={() => setSelectedIds(new Set(state.media.map((media) => media.mediaId)))}>Tümünü seç</button>
              {selected.length > 0 && <button type="button" className="secondary-button" disabled={isDownloading} onClick={() => handleDownload(() => downloadViewerSelectedMedia(viewerToken, selected), 'secili-medya.zip')}>Seçilenleri indir</button>}
              <button type="button" className="primary-button" disabled={isDownloading} onClick={() => handleDownload(() => downloadViewerAllMedia(viewerToken), 'tum-medya.zip')}>Tümünü indir</button>
            </div>
          </div>
          {downloadError && <p className="guest-error" role="alert">{downloadError}</p>}
          <div className="media-gallery-grid">
            {state.media.map((media) => (
              <MediaGalleryCard
                key={media.mediaId}
                media={media}
                selected={selectedIds.has(media.mediaId)}
                onToggle={toggleSelection}
                onDownload={(item) => handleDownload(() => downloadViewerSingleMedia(viewerToken, item.mediaId), item.originalFilename)}
                downloadDisabled={isDownloading}
              />
            ))}
          </div>
        </>
      )}
    </section>
  );
}
