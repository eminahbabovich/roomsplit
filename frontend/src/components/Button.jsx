// Dugme koje zna da je "u toku": pokaže spinner i blokira ponovni klik
export function Button({ variant = 'default', size, loading = false, block = false, children, className = '', ...rest }) {
  const classes = [
    'btn',
    variant !== 'default' && `btn-${variant}`,
    size && `btn-${size}`,
    block && 'btn-block',
    className,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <button className={classes} aria-busy={loading || undefined} disabled={loading || rest.disabled} {...rest}>
      {loading && <span className="spinner" aria-hidden="true" />}
      {children}
    </button>
  )
}
