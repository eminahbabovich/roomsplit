// Kategorije troška. Vrijednosti su isti nazivi kao enum ExpenseCategory na backendu.

export const CATEGORIES = [
  { value: 'FOOD', label: 'Hrana', color: 'var(--sun-l)' },
  { value: 'UTILITIES', label: 'Režije', color: 'var(--sky)' },
  { value: 'RENT', label: 'Stanarina', color: 'var(--terra-l)' },
  { value: 'TRANSPORT', label: 'Prijevoz', color: 'var(--sage)' },
  { value: 'ENTERTAINMENT', label: 'Zabava', color: '#f1d3e0' },
  { value: 'OTHER', label: 'Ostalo', color: 'var(--paper-2)' },
]

export function categoryMeta(value) {
  return CATEGORIES.find((c) => c.value === value) ?? CATEGORIES[CATEGORIES.length - 1]
}
