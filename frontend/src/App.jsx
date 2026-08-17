import { Route, Routes, useLocation } from 'react-router-dom';
import AdminRoute from './components/AdminRoute.jsx';
import AdminLoginPage from './pages/AdminLoginPage.jsx';
import AdminGalleryPage from './pages/AdminGalleryPage.jsx';
import AdminPage from './pages/AdminPage.jsx';
import AdminEventFormPage from './pages/AdminEventFormPage.jsx';
import GuestEventPage from './pages/GuestEventPage.jsx';
import LandingPage from './pages/LandingPage.jsx';
import ViewerGalleryPage from './pages/ViewerGalleryPage.jsx';

export default function App() {
  const { pathname } = useLocation();
  const shellVariant = pathname === '/'
    ? ' app-shell-landing'
    : pathname.startsWith('/e/')
      ? ' app-shell-guest'
      : pathname.startsWith('/gallery/')
        ? ' app-shell-viewer'
      : '';

  return (
    <div className={`app-shell${shellVariant}`}>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/e/:token" element={<GuestEventPage />} />
        <Route path="/gallery/:viewerToken" element={<ViewerGalleryPage />} />
        <Route path="/admin/login" element={<AdminLoginPage />} />
        <Route path="/admin" element={<AdminRoute><AdminPage /></AdminRoute>} />
        <Route path="/admin/events/new" element={<AdminRoute><AdminEventFormPage /></AdminRoute>} />
        <Route path="/admin/events/:id" element={<AdminRoute><AdminGalleryPage /></AdminRoute>} />
        <Route path="*" element={<p className="placeholder-page">Sayfa bulunamadı.</p>} />
      </Routes>
    </div>
  );
}
