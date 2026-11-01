import { useEffect, useState } from 'react'
import { formatKM } from '../lib/money.js'

// Broj se "nabroji" od nule do vrijednosti. Poštuje prefers-reduced-motion.
export function CountUp({ value, duration = 800, format = formatKM }) {
  const [shown, setShown] = useState(0)

  useEffect(() => {
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (reduce) {
      const id = requestAnimationFrame(() => setShown(value))
      return () => cancelAnimationFrame(id)
    }
    let frame
    const start = performance.now()
    const tick = (now) => {
      const t = Math.min(1, (now - start) / duration)
      const eased = 1 - Math.pow(1 - t, 3)
      setShown(value * eased)
      if (t < 1) frame = requestAnimationFrame(tick)
    }
    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [value, duration])

  return <span className="mono">{format(Math.round(shown * 100) / 100)}</span>
}
