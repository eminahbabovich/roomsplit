import { Button } from './Button.jsx'

export function PageLoader({ label = 'Učitavam…' }) {
  return (
    <div className="page-loader" role="status">
      <span className="spinner spinner-lg" aria-hidden="true" />
      <p>{label}</p>
    </div>
  )
}

export function SkeletonList({ rows = 3, height = 76 }) {
  return (
    <div className="skeleton-list" aria-hidden="true">
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="skeleton" style={{ height }} />
      ))}
    </div>
  )
}

function ReceiptArt({ children }) {
  return (
    <div className="state-art" aria-hidden="true">
      <svg viewBox="0 0 120 90" width="120" height="90">
        <path
          d="M20 10h80v70l-10-6-10 6-10-6-10 6-10-6-10 6-10-6-10 6z"
          fill="var(--card)"
          stroke="var(--ink)"
          strokeWidth="3"
          strokeLinejoin="round"
        />
        {children}
      </svg>
    </div>
  )
}

// Greška pri učitavanju, s dugmetom za ponovni pokušaj
export function ErrorState({ error, onRetry }) {
  return (
    <div className="state-box" role="alert">
      <ReceiptArt>
        <path d="M40 34l16 16M56 34L40 50" stroke="var(--terra)" strokeWidth="5" strokeLinecap="round" />
        <path d="M66 38h20M66 48h14" stroke="var(--ink-3)" strokeWidth="3" strokeLinecap="round" />
      </ReceiptArt>
      <h3>Ovo nije uspjelo učitati</h3>
      <p>{error?.message ?? 'Nepoznata greška.'}</p>
      {onRetry && (
        <Button size="sm" onClick={onRetry}>
          Pokušaj ponovo
        </Button>
      )}
    </div>
  )
}

export function EmptyState({ title, children, action }) {
  return (
    <div className="state-box">
      <ReceiptArt>
        <path d="M36 32h48M36 44h48M36 56h28" stroke="var(--ink-3)" strokeWidth="3" strokeLinecap="round" strokeDasharray="1 7" />
        <circle cx="88" cy="62" r="13" fill="var(--sun)" stroke="var(--ink)" strokeWidth="3" />
        <path d="M83 62h10" stroke="var(--ink)" strokeWidth="3" strokeLinecap="round" />
      </ReceiptArt>
      <h3>{title}</h3>
      <p>{children}</p>
      {action}
    </div>
  )
}
