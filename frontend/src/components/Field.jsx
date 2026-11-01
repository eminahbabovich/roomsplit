import { useId } from 'react'

// Labela + input + greška. Greška se veže na input preko aria-describedby (čitači ekrana).
// Za select i ostalo: children kao funkcija dobija props koje treba proslijediti elementu.
export function Field({ label, error, hint, affix, children, className = '', ...inputProps }) {
  const id = useId()
  const describedBy = error ? `${id}-err` : hint ? `${id}-hint` : undefined
  const input = children ? (
    children({ id, 'aria-invalid': !!error, 'aria-describedby': describedBy })
  ) : (
    <input
      id={id}
      className={`input ${inputProps.inputMode === 'decimal' ? 'input-money' : ''}`}
      aria-invalid={!!error}
      aria-describedby={describedBy}
      {...inputProps}
    />
  )

  return (
    <div className={`field ${className}`} data-invalid={!!error}>
      {label && <label htmlFor={id}>{label}</label>}
      {affix ? (
        <div className="input-affix">
          {input}
          <span>{affix}</span>
        </div>
      ) : (
        input
      )}
      {error ? (
        <p className="field-error" id={`${id}-err`} role="alert">
          {error}
        </p>
      ) : (
        hint && (
          <p className="field-hint" id={`${id}-hint`}>
            {hint}
          </p>
        )
      )}
    </div>
  )
}
