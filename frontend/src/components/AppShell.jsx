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
          <img className="brand-badge" src="/paypilot-logo.svg" alt="PayPilot logo" />
          <div>
            <h1>PayPilot</h1>
            <p>where every transaction matters</p>
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

      </aside>

      <main className="main-panel">
        <Outlet />
      </main>
    </div>
  );
}

