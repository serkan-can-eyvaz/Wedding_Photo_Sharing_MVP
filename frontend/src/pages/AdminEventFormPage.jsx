import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AdminApiError, createAdminEvent } from '../api/adminApi.js';
import { clearAdminSession } from '../auth/adminSession.js';

export default function AdminEventFormPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', eventDate: '', coverImageKey: '', active: true });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setSaving(true);
    try {
      const created = await createAdminEvent({
        name: form.name.trim(),
        eventDate: form.eventDate,
        coverImageKey: form.coverImageKey.trim() || null,
        active: form.active,
      });
      navigate(`/admin/events/${created.id}`, { replace: true });
    } catch (requestError) {
      if (requestError instanceof AdminApiError && requestError.status === 401) {
        clearAdminSession();
        navigate('/admin/login', { replace: true });
        return;
      }
      setError('Etkinlik oluşturulamadı. Alanları kontrol edip tekrar deneyin.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="admin-page admin-form-page">
      <header className="admin-page-header"><Link to="/admin">Etkinliklere dön</Link></header>
      <div className="admin-section-heading"><p>YENİ ETKİNLİK</p><h1>Etkinliği oluşturun.</h1><span>Misafir yükleme ve müşteri galeri bağlantıları otomatik hazırlanır.</span></div>
      <form className="admin-event-form" onSubmit={submit}>
        <label>Etkinlik adı<input required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
        <label>Tarih<input required type="date" value={form.eventDate} onChange={(event) => setForm({ ...form, eventDate: event.target.value })} /></label>
        <label>Kapak görsel anahtarı <small>Opsiyonel</small><input value={form.coverImageKey} onChange={(event) => setForm({ ...form, coverImageKey: event.target.value })} /></label>
        <label className="admin-switch"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /> Etkinlik aktif</label>
        {error && <p className="guest-error" role="alert">{error}</p>}
        <button className="primary-button" disabled={saving}>{saving ? 'Oluşturuluyor...' : 'Etkinliği oluştur'}</button>
      </form>
    </section>
  );
}
