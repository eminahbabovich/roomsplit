import { Link } from 'react-router-dom'

export function NotFound({ title = 'Ovdje nema ničega', children }) {
  return (
    <section className="container not-found">
      <p className="not-found-code mono" aria-hidden="true">
        404
      </p>
      <h1>{title}</h1>
      <p>{children ?? 'Stranica koju tražiš ne postoji, ili je neko pojeo račun prije tebe.'}</p>
      <Link to="/" className="btn btn-primary">
        Nazad na početnu
      </Link>
    </section>
  )
}
