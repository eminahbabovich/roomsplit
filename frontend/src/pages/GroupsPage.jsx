import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client.js'
import { groupsApi } from '../api/endpoints.js'
import { useAuth } from '../auth/useAuth.js'
import { Button } from '../components/Button.jsx'
import { EmptyState, ErrorState, SkeletonList } from '../components/Feedback.jsx'
import { Field } from '../components/Field.jsx'
import { useAsync } from '../hooks/useAsync.js'
import { greeting } from '../lib/dates.js'
import { useToast } from '../toast/toastContext.js'

const TILE_COLORS = ['var(--sun-l)', 'var(--sage)', 'var(--terra-l)', 'var(--sky)', '#f1d3e0', 'var(--paper-2)']

function CreateGroupCard() {
  const navigate = useNavigate()
  const toast = useToast()
  const [name, setName] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const group = await groupsApi.create(name.trim())
      toast.success(`Grupa „${group.name}“ je napravljena.`)
      navigate(`/grupe/${group.id}`)
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'Grupa nije napravljena.'))
      setSubmitting(false)
    }
  }

  return (
    <form className="card action-card" onSubmit={handleSubmit} noValidate>
      <p className="eyebrow">Nova grupa</p>
      <h2>Počni od nule</h2>
      <Field
        label="Naziv grupe"
        placeholder="npr. Stan na Ilidži"
        value={name}
        onChange={(e) => setName(e.target.value)}
        error={error?.fieldErrors?.name ?? (error && !Object.keys(error.fieldErrors).length ? error.message : undefined)}
        maxLength={100}
      />
      <Button type="submit" variant="green" loading={submitting}>
        Napravi grupu
      </Button>
    </form>
  )
}

function JoinGroupCard() {
  const navigate = useNavigate()
  const toast = useToast()
  const [code, setCode] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const group = await groupsApi.join(code.trim().toUpperCase())
      toast.success(`Dobro došao/la u „${group.name}“.`)
      navigate(`/grupe/${group.id}`)
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'Pridruživanje nije uspjelo.'))
      setSubmitting(false)
    }
  }

  return (
    <form className="card action-card action-card-sun" onSubmit={handleSubmit} noValidate>
      <p className="eyebrow">Imam kod</p>
      <h2>Uđi u postojeću grupu</h2>
      <Field
        label="Kod za pridruživanje"
        placeholder="8 znakova"
        className="code-field"
        value={code}
        onChange={(e) => setCode(e.target.value)}
        error={error?.fieldErrors?.joinCode ?? (error && !Object.keys(error.fieldErrors).length ? error.message : undefined)}
        maxLength={16}
        autoCapitalize="characters"
        spellCheck={false}
      />
      <Button type="submit" loading={submitting}>
        Pridruži se
      </Button>
    </form>
  )
}

export function GroupsPage() {
  const { user } = useAuth()
  const { data: groups, error, loading, reload } = useAsync(() => groupsApi.list(), 'groups')

  return (
    <div className="container page">
      <header className="page-head">
        <p className="eyebrow">Moje grupe</p>
        <h1>
          {greeting()}, <em>{user.displayName}</em>.
        </h1>
      </header>

      <div className="action-grid">
        <CreateGroupCard />
        <JoinGroupCard />
      </div>

      <section aria-labelledby="my-groups">
        <h2 id="my-groups" className="section-title">
          Tvoje grupe
        </h2>

        {loading && !groups && <SkeletonList rows={3} height={92} />}
        {error && <ErrorState error={error} onRetry={reload} />}
        {groups && groups.length === 0 && (
          <EmptyState title="Još nisi ni u jednoj grupi">
            Napravi prvu iznad ili upiši kod koji ti je poslao cimer.
          </EmptyState>
        )}
        {groups && groups.length > 0 && (
          <ul className="group-list">
            {groups.map((group, i) => (
              <li key={group.id} style={{ '--i': i }}>
                <Link to={`/grupe/${group.id}`} className="group-tile" style={{ '--tile': TILE_COLORS[group.id % TILE_COLORS.length] }}>
                  <span className="group-tile-name">{group.name}</span>
                  <span className="pill">{group.role === 'OWNER' ? 'vlasnik' : 'član'}</span>
                  <span className="group-tile-arrow" aria-hidden="true">
                    →
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
