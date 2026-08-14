import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AdminApiError, downloadEventQr, getAdminEvents } from '../api/adminApi.js';
import { clearAdminSession } from '../auth/adminSession.js';

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
      <header className="admin-page-header">
        <h1>Etkinlikler</h1>
        <button type="button" className="secondary-button" onClick={handleLogout}>Çıkış yap</button>
      </header>
      {state.status === 'loading' && <p className="guest-page-state">Etkinlikler yükleniyor...</p>}
      {state.status === 'error' && <p className="guest-error" role="alert">Etkinlikler alınamadı. Lütfen tekrar deneyin.</p>}
      {state.status === 'ready' && state.events.length === 0 && <p>Henüz etkinlik yok.</p>}
      {downloadError && <p className="guest-error" role="alert">{downloadError}</p>}
      {state.status === 'ready' && state.events.length > 0 && (
        <ul className="admin-event-list">
          {state.events.map((event) => (
            <li key={event.id}>
              <Link to={`/admin/events/${event.id}`}>
                <strong>{event.name}</strong>
                <span>{event.eventDate}</span>
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
