import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AdminApiError, login } from '../api/adminApi.js';
import { clearAdminSession, saveAdminSession } from '../auth/adminSession.js';

export default function AdminLoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);

    try {
      const session = await login(email, password);
      saveAdminSession(session);
      navigate('/admin', { replace: true });
    } catch (requestError) {
      clearAdminSession();
      setError(requestError instanceof AdminApiError && requestError.status === 401
        ? 'E-posta veya parola hatalı.'
        : 'Giriş şu anda tamamlanamadı. Lütfen tekrar deneyin.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <section className="admin-login-page">
      <h1>Yönetici girişi</h1>
      <form className="admin-login-form" onSubmit={handleSubmit}>
        <label>
          E-posta
          <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required autoComplete="email" />
        </label>
        <label>
          Parola
          <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required autoComplete="current-password" />
        </label>
        {error && <p className="guest-error" role="alert">{error}</p>}
        <button type="submit" className="primary-button" disabled={isSubmitting}>
          {isSubmitting ? 'Giriş yapılıyor...' : 'Giriş yap'}
        </button>
      </form>
    </section>
  );
}
