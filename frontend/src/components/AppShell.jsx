import { NavLink, Outlet } from 'react-router-dom';
import { useTheme } from '../utils/theme';

const navItems = [
  { to: '/overview', label: 'Overview' },
  { to: '/payments', label: 'Payments' },
  { to: '/support', label: 'Support' },
];

const brandLogo = `${import.meta.env.BASE_URL}favicon.svg`;

export function AppShell() {
  const { theme, toggleTheme } = useTheme();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <img className="brand-badge" src={brandLogo} alt="PayPilot logo" />
          <div>
            <h1>PayPilot</h1>
            <p className="brand-tagline">Every transaction, <em>perfectly handled.</em></p>
          </div>
        </div>

        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-link ${isActive ? 'nav-link--active' : ''}`.trim()}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <button
          type="button"
          className="theme-toggle"
          onClick={toggleTheme}
          aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          <span className="theme-toggle__icon" aria-hidden="true">
            {theme === 'dark' ? '☀️' : '🌙'}
          </span>
          <span>{theme === 'dark' ? 'Light mode' : 'Dark mode'}</span>
        </button>

      </aside>

      <main className="main-panel">
        <Outlet />
      </main>
    </div>
  );
}
