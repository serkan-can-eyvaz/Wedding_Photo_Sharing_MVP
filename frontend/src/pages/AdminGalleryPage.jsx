import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  AdminApiError,
  downloadAllMedia,
  downloadSelectedMedia,
  downloadSingleMedia,
  downloadEventQr,
  getAdminEvent,
  getEventMedia,
  updateAdminEvent,
} from '../api/adminApi.js';
import { clearAdminSession } from '../auth/adminSession.js';
import MediaGalleryCard from '../components/MediaGalleryCard.jsx';
import { copyTextToClipboard } from '../utils/clipboard.js';

export default function AdminGalleryPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [state, setState] = useState({ status: 'loading', event: null, media: [] });
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [downloadError, setDownloadError] = useState('');
  const [isDownloading, setIsDownloading] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState(null);
  const [isSaving, setIsSaving] = useState(false);
  const [preview, setPreview] = useState(null);
  const [copyFeedback, setCopyFeedback] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setSelectedIds(new Set());
    setDownloadError('');
    setCopyFeedback(null);
    setState({ status: 'loading', event: null, media: [] });

    Promise.all([getAdminEvent(id), getEventMedia(id)])
      .then(([event, media]) => {
        if (!cancelled) {
          setState({ status: 'ready', event, media });
          setEditForm({ name: event.name, eventDate: event.eventDate, coverImageKey: event.coverImageKey ?? '', active: event.active });
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

  const copyLink = async (link, label) => {
    const copied = await copyTextToClipboard(link);
    setCopyFeedback({ type: copied ? 'success' : 'error', message: `${label} ${copied ? 'kopyalandı' : 'kopyalanamadı'}.` });
  };

  const saveEdit = async (event) => {
    event.preventDefault();
    setIsSaving(true); setDownloadError('');
    try {
      const updated = await updateAdminEvent(id, { ...editForm, name: editForm.name.trim(), coverImageKey: editForm.coverImageKey.trim() || null });
      setState((current) => ({ ...current, event: updated }));
      setEditForm({ name: updated.name, eventDate: updated.eventDate, coverImageKey: updated.coverImageKey ?? '', active: updated.active });
      setIsEditing(false);
    } catch (error) {
      if (error instanceof AdminApiError && error.status === 401) { clearAdminSession(); navigate('/admin/login', { replace: true }); return; }
      setDownloadError('Etkinlik güncellenemedi. Lütfen tekrar deneyin.');
    } finally { setIsSaving(false); }
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
    } catch (error) {
      if (error instanceof AdminApiError && error.status === 401) {
        clearAdminSession();
        navigate('/admin/login', { replace: true });
        return;
      }
      setDownloadError('Medya indirilemedi. Lütfen tekrar deneyin.');
    } finally {
      setIsDownloading(false);
    }
  };

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
          <header className="gallery-header admin-event-detail-header">
            <div><p className="admin-eyebrow">ETKİNLİK OPERASYONU</p><h1>{state.event.name}</h1><p>{new Intl.DateTimeFormat('tr-TR', { dateStyle: 'long' }).format(new Date(`${state.event.eventDate}T00:00:00`))} · <b className={state.event.active ? 'status-active' : 'status-inactive'}>{state.event.active ? 'AKTİF' : 'PASİF'}</b> · {state.media.length} medya</p></div>
            <div className="admin-detail-actions">
              <button type="button" className="secondary-button" onClick={() => setIsEditing((value) => !value)}>Düzenle</button>
              <button type="button" className="secondary-button" disabled={isDownloading} onClick={() => handleDownload(() => downloadEventQr(id), 'event-qr.png')}>QR indir</button>
              <button type="button" className="primary-button" disabled={isDownloading || state.media.length === 0} onClick={() => handleDownload(() => downloadAllMedia(id), 'tum-medya.zip')}>Tümünü ZIP indir</button>
            </div>
          </header>
          <section className="admin-operation-links">
            <div><span>Misafir yükleme linki</span><a href={state.event.publicUrl} target="_blank" rel="noreferrer">Yükleme sayfasını aç</a><button type="button" className="secondary-button" onClick={() => copyLink(state.event.publicUrl, 'Misafir yükleme linki')}>Kopyala</button></div>
            <div><span>Müşteri galeri linki</span><a href={state.event.viewerUrl} target="_blank" rel="noreferrer">Galeriyi aç</a><button type="button" className="secondary-button" onClick={() => copyLink(state.event.viewerUrl, 'Müşteri galeri linki')}>Kopyala</button></div>
          </section>
          {copyFeedback && <p className={`admin-operation-feedback admin-operation-feedback-${copyFeedback.type}`} role={copyFeedback.type === 'success' ? 'status' : 'alert'}>{copyFeedback.message}</p>}
          {isEditing && editForm && <form className="admin-event-form admin-inline-form" onSubmit={saveEdit}>
            <label>Etkinlik adı<input required value={editForm.name} onChange={(event) => setEditForm({ ...editForm, name: event.target.value })} /></label>
            <label>Tarih<input required type="date" value={editForm.eventDate} onChange={(event) => setEditForm({ ...editForm, eventDate: event.target.value })} /></label>
            <label>Kapak görsel anahtarı <small>Opsiyonel</small><input value={editForm.coverImageKey} onChange={(event) => setEditForm({ ...editForm, coverImageKey: event.target.value })} /></label>
            <label className="admin-switch"><input type="checkbox" checked={editForm.active} onChange={(event) => setEditForm({ ...editForm, active: event.target.checked })} /> Etkinlik aktif</label>
            <div><button type="submit" className="primary-button" disabled={isSaving}>{isSaving ? 'Kaydediliyor...' : 'Değişiklikleri kaydet'}</button><button type="button" className="secondary-button" onClick={() => setIsEditing(false)}>İptal</button></div>
          </form>}
          {state.media.length > 0 && (
            <div className="gallery-selection-toolbar">
              <button type="button" className="secondary-button" onClick={selectAll}>Tümünü seç</button>
              <button type="button" className="secondary-button" onClick={clearSelection} disabled={selectedIds.size === 0}>Seçimi temizle</button>
              <span>{selectedIds.size} seçili</span>
              <button
                type="button"
                className="primary-button"
                disabled={isDownloading || selectedIds.size === 0}
                onClick={() => handleDownload(
                  () => downloadSelectedMedia(id, [...selectedIds]),
                  'secilen-medya.zip',
                )}
              >
                Seçilenleri indir
              </button>
              <button
                type="button"
                className="primary-button"
                disabled={isDownloading}
                onClick={() => handleDownload(() => downloadAllMedia(id), 'tum-medya.zip')}
              >
                Tümünü indir
              </button>
            </div>
          )}
          {downloadError && <p className="guest-error" role="alert">{downloadError}</p>}
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
                  onDownload={(item) => handleDownload(
                    () => downloadSingleMedia(id, item.mediaId),
                    item.originalFilename,
                  )}
                  onPreview={setPreview}
                  downloadDisabled={isDownloading}
                />
              ))}
            </div>
          )}
          {preview && <div className="media-preview-modal" role="dialog" aria-modal="true" aria-label={preview.originalFilename} onClick={() => setPreview(null)}><div onClick={(event) => event.stopPropagation()}><button type="button" className="secondary-button" onClick={() => setPreview(null)}>Kapat</button><img src={preview.previewUrl} alt={preview.originalFilename} referrerPolicy="no-referrer" /></div></div>}
        </>
      )}
    </section>
  );
}
