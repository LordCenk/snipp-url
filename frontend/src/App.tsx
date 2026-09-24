import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import type { ReactElement } from 'react';
import { useAuth } from './auth/AuthContext';
import Layout from './components/Layout';
import AuthPage from './pages/AuthPage';
import LinksPage from './pages/LinksPage';
import StatsPage from './pages/StatsPage';

function RequireAuth({ children }: { children: ReactElement }) {
  const { token } = useAuth();
  const location = useLocation();
  return token ? children : <Navigate to="/login" replace state={{ from: location.pathname }} />;
}

function GuestOnly({ children }: { children: ReactElement }) {
  const { token } = useAuth();
  return token ? <Navigate to="/" replace /> : children;
}

// Note: /auth, /urls, /analytics and /s are API paths proxied to the backend,
// so the app's own routes must not start with them.
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<GuestOnly><AuthPage mode="login" /></GuestOnly>} />
      <Route path="/register" element={<GuestOnly><AuthPage mode="register" /></GuestOnly>} />
      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route index element={<LinksPage />} />
        <Route path="stats" element={<StatsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
