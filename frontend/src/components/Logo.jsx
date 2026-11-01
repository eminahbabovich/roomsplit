import { Link } from 'react-router-dom'

// Znak: novčić podijeljen na dva dijela (zeleni i terakota)
export function LogoMark({ size = 32 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true" className="logo-mark">
      <g className="logo-half logo-half-l">
        <path d="M30 8a24 24 0 0 0 0 48z" fill="var(--green)" stroke="var(--ink)" strokeWidth="3" strokeLinejoin="round" />
      </g>
      <g className="logo-half logo-half-r">
        <path d="M34 8a24 24 0 0 1 0 48z" fill="var(--terra)" stroke="var(--ink)" strokeWidth="3" strokeLinejoin="round" />
      </g>
    </svg>
  )
}

export function Logo({ to = '/' }) {
  return (
    <Link to={to} className="logo" aria-label="RoomSplit, početna">
      <LogoMark />
      <span className="logo-word">
        room<span>split</span>
      </span>
    </Link>
  )
}
