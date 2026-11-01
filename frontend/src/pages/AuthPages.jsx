import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client.js'
import { useAuth } from '../auth/useAuth.js'
import { Button } from '../components/Button.jsx'
import { Field } from '../components/Field.jsx'
import { PageLoader } from '../components/Feedback.jsx'

// Zajednički okvir: lijevo "poster" sa sloganom, desno forma
function AuthShell({ title, lead, quote, children, footer }) {
  return (
    <section className="auth">
      <aside className="auth-poster" aria-hidden="true">
        <div className="auth-poster-inner">
          <p className="eyebrow">RoomSplit</p>
          <p className="auth-quote">{quote}</p>
          <div className="coin-row">
            <span className="coin coin-a mono">33,34</span>
            <span className="coin coin-b mono">33,33</span>
            <span className="coin coin-c mono">33,33</span>
          </div>
          <p className="auth-poster-note mono">100,00 KM ÷ 3. Nijedan fening ne nestaje.</p>
        </div>
      </aside>

      <div className="auth-panel">
        <div className="auth-card">
          <h1>{title}</h1>
          <p className="auth-lead">{lead}</p>
          {children}
          <p className="auth-footer">{footer}</p>
        </div>
      </div>
    </section>
  )
}

function useRedirectIfAuthed() {
  const { status } = useAuth()
  const location = useLocation()
  return {
    status,
    target: location.state?.from ?? '/grupe',
  }
}

function PasswordInput({ id, ...props }) {
  const [shown, setShown] = useState(false)
  return (
    <div className="password-wrap">
      <input id={id} className="input" type={shown ? 'text' : 'password'} {...props} />
      <button
        type="button"
        className="password-toggle"
        onClick={() => setShown((s) => !s)}
        aria-label={shown ? 'Sakrij lozinku' : 'Prikaži lozinku'}
        aria-pressed={shown}
      >
        {shown ? 'sakrij' : 'prikaži'}
      </button>
    </div>
  )
}

export function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const { status, target } = useRedirectIfAuthed()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  if (status === 'loading') return <PageLoader />
  if (status === 'authed') return <Navigate to={target} replace />

  const update = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }))

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(form.email.trim(), form.password)
      navigate(target, { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'Prijava nije uspjela.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthShell
      title="Dobro došao nazad"
      lead="Prijavi se i vidi ko je šta platio dok te nije bilo."
      quote="Računi su kao sudovi: bolje ih je riješiti odmah nego čekati da se nakupe."
      footer={
        <>
          Nemaš račun? <Link to="/registracija">Registruj se</Link>
        </>
      }
    >
      <form className="form" onSubmit={handleSubmit} noValidate>
        {error && !Object.keys(error.fieldErrors).length && (
          <div className="notice" role="alert">
            {error.message}
          </div>
        )}
        <Field
          label="Email"
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={update('email')}
          error={error?.fieldErrors?.email}
          required
        />
        <Field label="Lozinka" error={error?.fieldErrors?.password}>
          {(props) => (
            <PasswordInput {...props} autoComplete="current-password" value={form.password} onChange={update('password')} required />
          )}
        </Field>
        <Button type="submit" variant="primary" size="lg" block loading={submitting}>
          Prijavi se
        </Button>
      </form>
    </AuthShell>
  )
}

export function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const { status, target } = useRedirectIfAuthed()
  const [form, setForm] = useState({ displayName: '', email: '', password: '' })
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  if (status === 'loading') return <PageLoader />
  if (status === 'authed') return <Navigate to={target} replace />

  const update = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }))

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await register({ ...form, email: form.email.trim(), displayName: form.displayName.trim() })
      navigate('/grupe', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'Registracija nije uspjela.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthShell
      title="Napravi račun"
      lead="Treba ti minut. Poslije toga cimeri samo trebaju kod za grupu."
      quote="Ko je kupio toalet papir? Niko ne zna. Zato postoji ovo."
      footer={
        <>
          Već imaš račun? <Link to="/prijava">Prijavi se</Link>
        </>
      }
    >
      <form className="form" onSubmit={handleSubmit} noValidate>
        {error && !Object.keys(error.fieldErrors).length && (
          <div className="notice" role="alert">
            {error.message}
          </div>
        )}
        <Field
          label="Ime za prikaz"
          hint="Tako će te vidjeti ostali u grupi."
          autoComplete="nickname"
          value={form.displayName}
          onChange={update('displayName')}
          error={error?.fieldErrors?.displayName}
          maxLength={100}
          required
        />
        <Field
          label="Email"
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={update('email')}
          error={error?.fieldErrors?.email}
          required
        />
        <Field label="Lozinka" hint="Najmanje 8 znakova." error={error?.fieldErrors?.password}>
          {(props) => (
            <PasswordInput {...props} autoComplete="new-password" value={form.password} onChange={update('password')} minLength={8} required />
          )}
        </Field>
        <Button type="submit" variant="primary" size="lg" block loading={submitting}>
          Napravi račun
        </Button>
      </form>
    </AuthShell>
  )
}
