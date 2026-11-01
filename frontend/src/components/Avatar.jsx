// Krug s inicijalom. Boja zavisi od id-a, pa isti čovjek ima istu boju svuda.
const COLORS = ['var(--sun)', 'var(--terra-l)', 'var(--sage)', 'var(--sky)', '#f1d3e0', '#e8dcb8']

export function Avatar({ name, id = 0, size = 38 }) {
  const initial = (name ?? '?').trim().charAt(0).toUpperCase()
  return (
    <span
      className="avatar"
      style={{ '--size': `${size}px`, background: COLORS[Math.abs(Number(id)) % COLORS.length] }}
      aria-hidden="true"
      title={name}
    >
      {initial}
    </span>
  )
}

export function AvatarStack({ people, max = 4 }) {
  const shown = people.slice(0, max)
  const extra = people.length - shown.length
  return (
    <span className="avatar-stack">
      {shown.map((p) => (
        <Avatar key={p.userId} name={p.displayName} id={p.userId} size={32} />
      ))}
      {extra > 0 && (
        <span className="avatar" style={{ '--size': '32px', background: 'var(--card)' }}>
          +{extra}
        </span>
      )}
    </span>
  )
}
