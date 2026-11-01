import { useToast } from '../../toast/toastContext.js'

// Kod za pridruživanje s dugmetom za kopiranje
export function CopyCode({ code, large = false }) {
  const toast = useToast()

  async function copy() {
    try {
      await navigator.clipboard.writeText(code)
      toast.success('Kod je kopiran.')
    } catch {
      toast.error('Kopiranje nije uspjelo, prepiši kod ručno.')
    }
  }

  return (
    <button type="button" className={`code-ticket ${large ? 'code-ticket-lg' : ''}`} onClick={copy} aria-label={`Kopiraj kod ${code}`}>
      <span className="code-ticket-label">kod</span>
      <span className="code-ticket-value mono">{code}</span>
      <span className="code-ticket-copy" aria-hidden="true">
        kopiraj
      </span>
    </button>
  )
}
