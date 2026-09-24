import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Logo from './Logo';

export default function Layout() {
  const { email, signOut } = useAuth();
  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand" aria-label="snipp home">
            <Logo />
            <span>snipp</span>
          </NavLink>
          <nav className="nav" aria-label="Main">
            <NavLink to="/" end>Links</NavLink>
            <NavLink to="/stats">Stats</NavLink>
          </nav>
          <div className="account">
            {email && <span className="account-email" title={email}>{email}</span>}
            <button type="button" className="btn btn-ghost" onClick={() => signOut()}>
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
