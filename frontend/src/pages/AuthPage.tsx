import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import * as api from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Logo from '../components/Logo';

const MIN_PASSWORD = 8;
const MAX_PASSWORD = 72;

export default function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { signIn, notice } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const isRegister = mode === 'register';
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (isRegister && password.length < MIN_PASSWORD) {
      setError(`Password must be at least ${MIN_PASSWORD} characters.`);
      return;
    }
    setBusy(true);
    try {
      if (isRegister) await api.register(email.trim(), password);
      await signIn(email.trim(), password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err instanceof api.ApiError ? friendly(err, isRegister) : 'Something went wrong. Please try again.');
      setBusy(false);
    }
  }

  return (
    <div className="auth">
      <div className="auth-card">
        <div className="auth-brand">
          <Logo size={32} />
          <span>snipp</span>
        </div>
        <h1>{isRegister ? 'Create your account' : 'Log in'}</h1>
        <p className="muted">
          {isRegister ? 'Shorten links and see who clicks them.' : 'Welcome back.'}
        </p>

        {notice && !isRegister && (
          <p className="alert alert-info" role="status">{notice}</p>
        )}

        <form onSubmit={onSubmit} noValidate>
          <label className="field">
            <span>Email</span>
            <input
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </label>
          <label className="field">
            <span>Password</span>
            <input
              type="password"
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              required
              maxLength={MAX_PASSWORD}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              aria-describedby={isRegister ? 'password-hint' : undefined}
            />
            {isRegister && (
              <small id="password-hint" className="muted">
                {MIN_PASSWORD}–{MAX_PASSWORD} characters
              </small>
            )}
          </label>

          {error && (
            <p className="alert alert-error" role="alert">{error}</p>
          )}

          <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
            {busy ? 'Please wait…' : isRegister ? 'Create account' : 'Log in'}
          </button>
        </form>

        <p className="auth-switch">
          {isRegister ? (
            <>Already have an account? <Link to="/login">Log in</Link></>
          ) : (
            <>New here? <Link to="/register">Create an account</Link></>
          )}
        </p>
      </div>
    </div>
  );
}

/** Turns backend messages like "email: must be a well-formed email address" into UI copy. */
function friendly(err: api.ApiError, isRegister: boolean): string {
  if (err.status === 401) return 'Incorrect email or password.';
  const message = err.message;
  if (message.startsWith('email:')) return 'Enter a valid email address.';
  if (message.startsWith('password:')) {
    return isRegister ? `Password must be ${MIN_PASSWORD}–${MAX_PASSWORD} characters.` : 'Enter your password.';
  }
  if (message === 'Email already exists') return 'An account with this email already exists. Try logging in.';
  return message;
}
