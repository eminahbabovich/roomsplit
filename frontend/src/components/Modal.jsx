import { useEffect, useRef } from 'react'

// Nativni <dialog>: browser sam daje focus trap, Esc za zatvaranje i sloj iznad stranice.
// Bez toga bismo ručno pisali sve troje, i vjerovatno bi nešto pokvarili za tastaturu.
export function Modal({ open, onClose, title, children }) {
  const ref = useRef(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) {
      dialog.showModal()
      // showModal fokusira prvi element (dugme "zatvori"); mi želimo prvo polje forme
      dialog.querySelector('.modal-body input, .modal-body select')?.focus()
    }
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      className="modal"
      onClose={onClose}
      onClick={(e) => {
        // klik na tamnu pozadinu (sam <dialog>, ne sadržaj) zatvara
        if (e.target === ref.current) onClose()
      }}
      aria-label={title}
    >
      {open && (
        <div className="modal-inner">
          <div className="modal-head">
            <h2>{title}</h2>
            <button type="button" className="icon-btn" onClick={onClose} aria-label="Zatvori">
              <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
                <path d="M3 3l10 10M13 3L3 13" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </button>
          </div>
          <div className="modal-body">{children}</div>
        </div>
      )}
    </dialog>
  )
}
