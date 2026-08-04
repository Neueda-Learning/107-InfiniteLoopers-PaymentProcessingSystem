import { getStatusTone, prettifyStatus } from '../utils/formatters';

export function PageHeader({ eyebrow, title, description, actions }) {
  return (
    <div className="page-header">
      <div>
        {eyebrow ? <p className="page-eyebrow">{eyebrow}</p> : null}
        <h1>{title}</h1>
        {description ? <p className="page-description">{description}</p> : null}
      </div>
      {actions ? <div className="page-actions">{actions}</div> : null}
    </div>
  );
}

export function SectionCard({ title, subtitle, actions, children, className = '' }) {
  return (
    <section className={`card ${className}`.trim()}>
      {(title || subtitle || actions) ? (
        <div className="card-header">
          <div>
            {title ? <h2>{title}</h2> : null}
            {subtitle ? <p>{subtitle}</p> : null}
          </div>
          {actions ? <div className="card-actions">{actions}</div> : null}
        </div>
      ) : null}
      {children}
    </section>
  );
}

export function StatCard({ label, value, helper, tone = 'neutral' }) {
  return (
    <div className={`stat-card stat-card--${tone}`}>
      <p>{label}</p>
      <strong>{value}</strong>
      {helper ? <span>{helper}</span> : null}
    </div>
  );
}

export function StatusBadge({ status }) {
  const tone = getStatusTone(status);
  return <span className={`badge badge--${tone}`}>{prettifyStatus(status)}</span>;
}

export function ErrorAlert({ message, onDismiss }) {
  if (!message) {
    return null;
  }

  return (
    <div className="alert alert--error" role="alert">
      <span>{message}</span>
      {onDismiss ? (
        <button type="button" className="ghost-button" onClick={onDismiss}>
          Dismiss
        </button>
      ) : null}
    </div>
  );
}

export function SuccessAlert({ message, onDismiss }) {
  if (!message) {
    return null;
  }

  return (
    <div className="alert alert--success" role="status">
      <span>{message}</span>
      {onDismiss ? (
        <button type="button" className="ghost-button" onClick={onDismiss}>
          Dismiss
        </button>
      ) : null}
    </div>
  );
}

export function LoadingState({ label = 'Loading...' }) {
  return (
    <div className="empty-state muted">
      <div className="spinner" aria-hidden="true" />
      <p>{label}</p>
    </div>
  );
}

export function EmptyState({ title, description }) {
  return (
    <div className="empty-state">
      <h3>{title}</h3>
      {description ? <p>{description}</p> : null}
    </div>
  );
}

export function DataTable({ columns, rows, rowKey, onRowClick, empty }) {
  if (!rows.length) {
    return empty || <EmptyState title="No data available" />;
  }

  return (
    <div className="table-shell">
      <table>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key}>{column.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={rowKey(row)}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              className={onRowClick ? 'table-row-clickable' : ''}
            >
              {columns.map((column) => (
                <td key={column.key}>{column.render ? column.render(row) : row[column.key]}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

