import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AdminApiError, downloadEventQr, getAdminEvents } from '../api/adminApi.js';
import { clearAdminSession } from '../auth/adminSession.js';
import BrandLogo from '../components/BrandLogo.jsx';

export default function AdminPage() {
  const navigate = useNavigate();
  const [state, setState] = useState({ status: 'loading', events: [] });
  const [downloadError, setDownloadError] = useState('');
  const [downloadingEventId, setDownloadingEventId] = useState(null);

  useEffect(() => {
    let cancelled = false;

    getAdminEvents()
      .then((events) => {
        if (!cancelled) {
          setState({ status: 'ready', events });
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
        setState({ status: 'error', events: [] });
      });

    return () => {
      cancelled = true;
    };
  }, [navigate]);

  const handleLogout = () => {
    clearAdminSession();
    navigate('/admin/login', { replace: true });
  };

  const downloadQr = async (eventId) => {
    setDownloadError('');
    setDownloadingEventId(eventId);
    try {
      const blob = await downloadEventQr(eventId);
      const objectUrl = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = 'event-qr.png';
      document.body.append(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
    } catch (error) {
      if (error instanceof AdminApiError && error.status === 401) {
        clearAdminSession();
        navigate('/admin/login', { replace: true });
        return;
      }
      setDownloadError('QR kodu indirilemedi. Lütfen tekrar deneyin.');
    } finally {
      setDownloadingEventId(null);
    }
  };

  return (
    <section className="admin-page">
      <header className="admin-page-header admin-dashboard-header">
        <div><BrandLogo variant="mark" className="admin-header-mark" decorative /><p className="admin-eyebrow">YÖNETİM PANELİ</p><h1>Etkinlikler</h1><span>Tüm etkinlikleri, galerileri ve paylaşım bağlantılarını yönetin.</span></div>
        <div><Link className="primary-button" to="/admin/events/new">+ Yeni etkinlik oluştur</Link><button type="button" className="secondary-button" onClick={handleLogout}>Çıkış yap</button></div>
      </header>
      {state.status === 'loading' && <p className="guest-page-state">Etkinlikler yükleniyor...</p>}
      {state.status === 'error' && <p className="guest-error" role="alert">Etkinlikler alınamadı. Lütfen tekrar deneyin.</p>}
      {state.status === 'ready' && state.events.length === 0 && <p className="admin-empty-state">Henüz etkinlik yok. İlk etkinliği oluşturarak başlayın.</p>}
      {downloadError && <p className="guest-error" role="alert">{downloadError}</p>}
      {state.status === 'ready' && state.events.length > 0 && (
        <ul className="admin-event-list">
          {state.events.map((event) => (
            <li key={event.id}>
              <Link to={`/admin/events/${event.id}`}>
                <div><strong>{event.name}</strong><span>{new Intl.DateTimeFormat('tr-TR', { dateStyle: 'long' }).format(new Date(`${event.eventDate}T00:00:00`))}</span></div>
                <div className="admin-event-meta"><b className={event.active ? 'status-active' : 'status-inactive'}>{event.active ? 'AKTİF' : 'PASİF'}</b><span>{event.mediaCount} medya</span><span>{new Intl.DateTimeFormat('tr-TR', { dateStyle: 'medium' }).format(new Date(event.createdAt))}</span></div>
                <em>Detayı aç →</em>
              </Link>
              <button
                type="button"
                className="secondary-button"
                disabled={downloadingEventId !== null}
                onClick={() => downloadQr(event.id)}
              >
                {downloadingEventId === event.id ? 'QR indiriliyor...' : 'QR İndir'}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
