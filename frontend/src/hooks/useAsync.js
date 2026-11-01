import { useCallback, useEffect, useRef, useState } from 'react'

// Učitava podatke i vodi računa o loading/error stanju.
//  - fetcher: funkcija koja vraća Promise
//  - key: string; kad se promijeni, podaci se učitaju ponovo (npr. broj stranice ili filter)
// Dok se novi podaci učitavaju, stari ostaju vidljivi (data), a loading je true. Tako lista
// ne "trepće" pri promjeni stranice. reload() ponovo učita isti key.
export function useAsync(fetcher, key) {
  const [reloadCount, setReloadCount] = useState(0)
  const fullKey = `${key}#${reloadCount}`
  const [result, setResult] = useState({ key: null, data: null, error: null })

  // Uvijek zovemo najnoviju verziju fetchera, a da effect ne ovisi o njoj
  const fetcherRef = useRef(fetcher)
  useEffect(() => {
    fetcherRef.current = fetcher
  })

  useEffect(() => {
    let cancelled = false
    fetcherRef
      .current()
      .then((data) => {
        if (!cancelled) setResult({ key: fullKey, data, error: null })
      })
      .catch((error) => {
        if (!cancelled) setResult((prev) => ({ key: fullKey, data: prev.data, error }))
      })
    return () => {
      cancelled = true
    }
  }, [fullKey])

  const reload = useCallback(() => setReloadCount((n) => n + 1), [])
  const loading = result.key !== fullKey

  return {
    data: result.data,
    error: loading ? null : result.error,
    loading,
    reload,
  }
}
