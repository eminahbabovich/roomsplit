// Datume formatiramo ručno (nazivi mjeseci na bosanskom), da izgled ne zavisi od locale podataka
// u browseru. Datumi dolaze s backenda kao "2026-11-01" (bez vremenske zone) i parsiramo ih
// ručno, jer new Date("2026-11-01") tretira string kao UTC i u nekim zonama pokaže prethodni dan.

const SHORT = ['jan', 'feb', 'mar', 'apr', 'maj', 'jun', 'jul', 'aug', 'sep', 'okt', 'nov', 'dec']
const LONG = [
  'januara',
  'februara',
  'marta',
  'aprila',
  'maja',
  'juna',
  'jula',
  'augusta',
  'septembra',
  'oktobra',
  'novembra',
  'decembra',
]

function parseISODate(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function formatDay(iso) {
  const date = parseISODate(iso)
  return `${date.getDate()}. ${SHORT[date.getMonth()]}`
}

export function formatInstant(isoInstant) {
  const date = new Date(isoInstant)
  return `${date.getDate()}. ${LONG[date.getMonth()]} ${date.getFullYear()}.`
}

// Današnji datum u lokalnoj zoni kao YYYY-MM-DD
export function todayISO() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

export function greeting() {
  const hour = new Date().getHours()
  if (hour < 5) return 'Još si budan'
  if (hour < 11) return 'Dobro jutro'
  if (hour < 18) return 'Zdravo'
  return 'Dobro veče'
}
