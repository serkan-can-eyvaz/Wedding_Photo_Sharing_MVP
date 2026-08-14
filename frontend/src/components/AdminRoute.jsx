import { Navigate, useLocation } from 'react-router-dom';
import { getAdminSession } from '../auth/adminSession.js';

export default function AdminRoute({ children }) {
  const location = useLocation();

  if (!getAdminSession()) {
    return <Navigate to="/admin/login" replace state={{ from: location.pathname }} />;
  }

  return children;
}
