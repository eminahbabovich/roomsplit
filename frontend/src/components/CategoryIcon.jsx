import { categoryMeta } from '../lib/categories.js'

const PATHS = {
  FOOD: <path d="M7 3v8a2 2 0 0 0 4 0V3M9 3v18M17 3c-2 2-3 5-3 8h3v10" />,
  UTILITIES: <path d="M13 2L4 14h7l-1 8 9-12h-7z" />,
  RENT: <path d="M3 11l9-8 9 8M5 10v10h14V10M10 20v-6h4v6" />,
  TRANSPORT: <path d="M5 16V8a3 3 0 0 1 3-3h8a3 3 0 0 1 3 3v8M3 16h18M7 19v-3M17 19v-3M5 11h14" />,
  ENTERTAINMENT: (
    <path d="M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4zM14 6v12" strokeDasharray="2 3" />
  ),
  OTHER: (
    <>
      <circle cx="6" cy="12" r="1.2" />
      <circle cx="12" cy="12" r="1.2" />
      <circle cx="18" cy="12" r="1.2" />
    </>
  ),
}

export function CategoryIcon({ category, size = 44 }) {
  const meta = categoryMeta(category)
  return (
    <span className="cat-icon" style={{ '--size': `${size}px`, background: meta.color }} title={meta.label}>
      <svg
        width={size * 0.5}
        height={size * 0.5}
        viewBox="0 0 24 24"
        fill="none"
        stroke="var(--ink)"
        strokeWidth="1.9"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        {PATHS[meta.value]}
      </svg>
    </span>
  )
}
