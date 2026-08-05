import { NavLink, Outlet } from 'react-router-dom';

const navItems = [
  { to: '/overview', label: 'Overview' },
  { to: '/payments', label: 'Payments' },
  { to: '/transactions', label: 'Transactions' },
  { to: '/customers', label: 'Customers' },
  { to: '/support', label: 'Support' },
];

export function AppShell() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <span className="brand-badge">PPS</span>
          <div>
            <h1>Payment Processing System</h1>
            <p>React frontend connected to your Spring Boot APIs.</p>
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

        <div className="sidebar-footnote">
          <p>Backend base path: <code>/api</code></p>
          <p>Recommended backend URL: <code>http://localhost:8080</code></p>
        </div>
      </aside>

      <main className="main-panel">
        <Outlet />
      </main>
    </div>
  );
}

