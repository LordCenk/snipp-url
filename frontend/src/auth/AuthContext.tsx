import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import * as api from '../api/client';

interface AuthState {
  token: string | null;
  email: string | null;
  /** Set when the user was signed out because the session ended, shown on the login page */
  notice: string | null;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: (notice?: string) => void;
}

const AuthContext = createContext<AuthState | null>(null);

function initialToken(): string | null {
  const token = api.getToken();
  if (token && api.isTokenExpired(token)) {
    api.setToken(null);
    return null;
  }
  return token;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(initialToken);
  const [notice, setNotice] = useState<string | null>(null);

  const signOut = useCallback((message?: string) => {
    api.setToken(null);
    setTokenState(null);
    setNotice(message ?? null);
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    const newToken = await api.login(email, password);
    api.setToken(newToken);
    setNotice(null);
    setTokenState(newToken);
  }, []);

  // Any API call that comes back 401 ends the session
  useEffect(() => {
    const onUnauthorized = () => signOut('Your session has expired. Please log in again.');
    window.addEventListener(api.UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(api.UNAUTHORIZED_EVENT, onUnauthorized);
  }, [signOut]);

  // Sign out when the token's expiry time passes while the page is open
  useEffect(() => {
    if (!token) return;
    const exp = api.decodeToken(token)?.exp;
    if (!exp) return;
    const ms = exp * 1000 - Date.now();
    const timer = window.setTimeout(
      () => signOut('Your session has expired. Please log in again.'),
      Math.max(0, Math.min(ms, 2 ** 31 - 1)),
    );
    return () => window.clearTimeout(timer);
  }, [token, signOut]);

  const value = useMemo<AuthState>(
    () => ({
      token,
      email: token ? (api.decodeToken(token)?.sub ?? null) : null,
      notice,
      signIn,
      signOut,
    }),
    [token, notice, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
