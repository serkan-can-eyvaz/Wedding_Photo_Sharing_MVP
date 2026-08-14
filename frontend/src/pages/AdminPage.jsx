import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AdminApiError, getAdminEvents } from '../api/adminApi.js';
import { clearAdminSession } from '../auth/adminSession.js';

export default function AdminPage() {
  const navigate = useNavigate();
  const [state, setState] = useState({ status: 'loading', events: [] });

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

  return (
    <section className="admin-page">
      <header className="admin-page-header">
        <h1>Etkinlikler</h1>
        <button type="button" className="secondary-button" onClick={handleLogout}>Çıkış yap</button>
      </header>
      {state.status === 'loading' && <p className="guest-page-state">Etkinlikler yükleniyor...</p>}
      {state.status === 'error' && <p className="guest-error" role="alert">Etkinlikler alınamadı. Lütfen tekrar deneyin.</p>}
      {state.status === 'ready' && state.events.length === 0 && <p>Henüz etkinlik yok.</p>}
      {state.status === 'ready' && state.events.length > 0 && (
        <ul className="admin-event-list">
          {state.events.map((event) => (
            <li key={event.id}>
              <Link to={`/admin/events/${event.id}`}>
                <strong>{event.name}</strong>
                <span>{event.eventDate}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
