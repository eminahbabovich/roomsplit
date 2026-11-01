import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { balancesApi, groupsApi } from '../api/endpoints.js'
import { useAuth } from '../auth/useAuth.js'
import { AvatarStack } from '../components/Avatar.jsx'
import { CountUp } from '../components/CountUp.jsx'
import { ErrorState, PageLoader } from '../components/Feedback.jsx'
import { useAsync } from '../hooks/useAsync.js'
import { toFeninzi } from '../lib/money.js'
import { BalancesTab } from './group/BalancesTab.jsx'
import { CopyCode } from './group/CopyCode.jsx'
import { ExpensesTab } from './group/ExpensesTab.jsx'
import { MembersTab } from './group/MembersTab.jsx'
import { SettleModal } from './group/SettleModal.jsx'
import { NotFound } from './NotFound.jsx'

const TABS = [
  { key: 'troskovi', label: 'Troškovi' },
  { key: 'balansi', label: 'Balansi' },
  { key: 'clanovi', label: 'Članovi' },
]

// Veliki sažetak: šta je tvoj ukupni balans u ovoj grupi
function MyBalance({ balances, me }) {
  const mine = balances.find((b) => b.userId === me.id)
  if (!mine) return null
  const state = toFeninzi(mine.balance) === 0 ? 'zero' : mine.balance > 0 ? 'pos' : 'neg'
  const copy = {
    pos: 'Drugi tebi duguju ukupno',
    neg: 'Ti duguješ ukupno',
    zero: 'Ti si kvit',
  }[state]

  return (
    <div className="my-balance" data-state={state}>
      <p className="eyebrow">Tvoj balans</p>
      <p className="my-balance-copy">{copy}</p>
      {state !== 'zero' && (
        <p className="my-balance-value">
          <CountUp key={mine.balance} value={Math.abs(mine.balance)} />
        </p>
      )}
    </div>
  )
}

function GroupView({ id }) {
  const { user: me } = useAuth()
  const [params, setParams] = useSearchParams()
  const tab = TABS.some((t) => t.key === params.get('tab')) ? params.get('tab') : 'troskovi'
  const [settle, setSettle] = useState({ open: false, preset: null })

  const group = useAsync(() => groupsApi.get(id), `group-${id}`)
  const balances = useAsync(() => balancesApi.balances(id), `balances-${id}`)
  const suggestions = useAsync(() => balancesApi.suggestions(id), `suggestions-${id}`)

  const refreshMoney = () => {
    balances.reload()
    suggestions.reload()
  }

  if (group.error?.status === 404) {
    return (
      <NotFound title="Ova grupa ne postoji">
        Ili nisi njen član. Provjeri kod koji ti je poslao cimer.
      </NotFound>
    )
  }
  if (group.error) {
    return (
      <div className="container page">
        <ErrorState error={group.error} onRetry={group.reload} />
      </div>
    )
  }
  if (!group.data) return <PageLoader label="Otvaram grupu…" />

  const g = group.data
  const people = g.members

  return (
    <div className="container page">
      <Link to="/grupe" className="back-link">
        ← Sve grupe
      </Link>

      <header className="group-head">
        <div className="group-head-main">
          <h1>{g.name}</h1>
          <div className="group-head-meta">
            <AvatarStack people={people} />
            <span>
              {people.length} {people.length === 1 ? 'član' : 'članova'}
            </span>
            <CopyCode code={g.joinCode} />
          </div>
        </div>
        {balances.data && <MyBalance balances={balances.data} me={me} />}
      </header>

      <div className="tabs" role="tablist" aria-label="Sekcije grupe">
        {TABS.map((t) => (
          <button
            key={t.key}
            role="tab"
            id={`tab-${t.key}`}
            aria-selected={tab === t.key}
            aria-controls={`panel-${t.key}`}
            className="tab"
            onClick={() => setParams(t.key === 'troskovi' ? {} : { tab: t.key }, { replace: true })}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div role="tabpanel" id={`panel-${tab}`} aria-labelledby={`tab-${tab}`}>
        {tab === 'troskovi' && <ExpensesTab groupId={id} members={people} me={me} onChanged={refreshMoney} />}
        {tab === 'balansi' && (
          <BalancesTab balances={balances} suggestions={suggestions} me={me} onSettle={(preset) => setSettle({ open: true, preset })} />
        )}
        {tab === 'clanovi' && <MembersTab group={g} me={me} />}
      </div>

      <SettleModal
        open={settle.open}
        onClose={() => setSettle((s) => ({ ...s, open: false }))}
        groupId={id}
        members={people}
        me={me}
        preset={settle.preset}
        onSaved={() => {
          setSettle((s) => ({ ...s, open: false }))
          refreshMoney()
        }}
      />
    </div>
  )
}

export function GroupPage() {
  const { id } = useParams()
  // key = id: kad se promijeni grupa, cijela stranica kreće ispočetka (nema starih podataka)
  return <GroupView key={id} id={id} />
}
