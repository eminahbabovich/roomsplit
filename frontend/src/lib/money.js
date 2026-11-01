// Novac na klijentu radimo u feninzima (cijeli brojevi), isto kao backend.
// 1 KM = 100 feninga. Tako 0.1 + 0.2 nikad ne pravi probleme.

// Formatiramo ručno (1.234,50), a ne preko Intl: izgled iznosa ne smije zavisiti od toga
// da li browser ima podatke za bosanski locale.
function formatNumber(value) {
  const feninzi = Math.abs(Math.round(Number(value) * 100))
  const whole = String(Math.floor(feninzi / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  const fraction = String(feninzi % 100).padStart(2, '0')
  return `${whole},${fraction}`
}

export function formatKM(value) {
  const sign = toFeninzi(value) < 0 ? '−' : ''
  return `${sign}${formatNumber(value)} KM`
}

// "+60,00 KM" / "−30,00 KM" / "0,00 KM"
export function formatSignedKM(value) {
  if (toFeninzi(value) === 0) return '0,00 KM'
  return `${value > 0 ? '+' : '−'}${formatNumber(value)} KM`
}

export function toFeninzi(value) {
  return Math.round(Number(value) * 100)
}

export function fromFeninzi(feninzi) {
  return feninzi / 100
}

// Prima tekst iz inputa ("12,5", "12.50"). Vraća broj ili null ako nije ispravan iznos
// (najviše 2 decimale, veći od nule).
export function parseAmount(text) {
  const clean = String(text ?? '').trim().replace(',', '.')
  if (!/^\d{1,10}(\.\d{1,2})?$/.test(clean)) return null
  const value = Number(clean)
  return value > 0 ? value : null
}

// Ista logika kao ExpenseSplitter na backendu: osnovni dio + ostatak po 1 fening prvima.
// 100.00 na troje -> [3334, 3333, 3333]. Služi samo za pregled u formi, server računa konačno.
export function splitEqual(totalFeninzi, count) {
  if (count <= 0) return []
  const base = Math.floor(totalFeninzi / count)
  const remainder = totalFeninzi % count
  return Array.from({ length: count }, (_, i) => base + (i < remainder ? 1 : 0))
}
