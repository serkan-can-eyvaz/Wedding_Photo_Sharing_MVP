import { Route, Routes } from 'react-router-dom';
import AdminLoginPage from './pages/AdminLoginPage.jsx';
import AdminPage from './pages/AdminPage.jsx';
import GuestEventPage from './pages/GuestEventPage.jsx';

export default function App() {
  return (
    <main className="app-shell">
      <Routes>
        <Route path="/e/:token" element={<GuestEventPage />} />
        <Route path="/admin/login" element={<AdminLoginPage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="*" element={<p className="placeholder-page">Sayfa bulunamadı.</p>} />
      </Routes>
    </main>
  );
}
