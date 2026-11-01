import { Avatar } from '../../components/Avatar.jsx'
import { Button } from '../../components/Button.jsx'
import { ErrorState, SkeletonList } from '../../components/Feedback.jsx'
import { formatKM, formatSignedKM, toFeninzi } from '../../lib/money.js'

// Pečat "KVIT" kad su svi poravnati
function Stamp() {
  return (
    <div className="kvit" role="status">
      <span className="kvit-stamp mono">KVIT</span>
      <p>Svi su poravnati. Nema ko kome ništa da duguje.</p>
    </div>
  )
}

function BalanceBars({ balances, me }) {
  const max = Math.max(...balances.map((b) => Math.abs(b.balance)), 0.01)
  return (
    <ul className="bars">
      {balances.map((b, i) => {
        const pct = (Math.abs(b.balance) / max) * 100
        const state = toFeninzi(b.balance) === 0 ? 'zero' : b.balance > 0 ? 'pos' : 'neg'
        return (
          <li key={b.userId} className="bar-row" style={{ '--i': i }}>
            <span className="bar-person">
              <Avatar name={b.displayName} id={b.userId} size={32} />
              <span>
                {b.displayName}
                {b.userId === me.id && <em> (ti)</em>}
              </span>
            </span>
            <span className="bar-track" aria-hidden="true">
              <span className="bar-axis" />
              {state !== 'zero' && <span className="bar-fill" data-state={state} style={{ '--w': `${pct / 2}%` }} />}
            </span>
            <span className="bar-value mono" data-state={state}>
              {formatSignedKM(b.balance)}
            </span>
          </li>
        )
      })}
    </ul>
  )
}

export function BalancesTab({ balances, suggestions, me, onSettle }) {
  const loadingFirst = (balances.loading && !balances.data) || (suggestions.loading && !suggestions.data)
  const error = balances.error ?? suggestions.error
  const allSettled = balances.data?.every((b) => toFeninzi(b.balance) === 0)

  return (
    <div className="tab-panel">
      {loadingFirst && <SkeletonList rows={4} height={64} />}
      {error && (
        <ErrorState
          error={error}
          onRetry={() => {
            balances.reload()
            suggestions.reload()
          }}
        />
      )}

      {balances.data && suggestions.data && (
        <>
          <section aria-labelledby="bal-title">
            <div className="section-head">
              <h2 id="bal-title" className="section-title">
                Ko kome duguje
              </h2>
              <p className="section-note">Zeleno: drugi duguju njemu. Crveno: on duguje drugima.</p>
            </div>
            <div className="card pad">
              <BalanceBars balances={balances.data} me={me} />
            </div>
          </section>

          <section aria-labelledby="sug-title">
            <div className="section-head">
              <h2 id="sug-title" className="section-title">
                Najmanje uplata do kvita
              </h2>
              <p className="section-note">
                {suggestions.data.length === 0
                  ? 'Ništa za platiti.'
                  : `${suggestions.data.length} ${suggestions.data.length === 1 ? 'uplata' : suggestions.data.length < 5 ? 'uplate' : 'uplata'} i svi su kvit.`}
              </p>
            </div>

            {allSettled ? (
              <Stamp />
            ) : (
              <ul className="transfers">
                {suggestions.data.map((t, i) => {
                  const mine = t.from.userId === me.id
                  return (
                    <li key={`${t.from.userId}-${t.to.userId}`} className="transfer" data-mine={mine} style={{ '--i': i }}>
                      <span className="transfer-who">
                        <Avatar name={t.from.displayName} id={t.from.userId} size={36} />
                        <span>{mine ? 'Ti' : t.from.displayName}</span>
                      </span>
                      <span className="transfer-arrow" aria-label="plaća">
                        <span className="mono transfer-amount">{formatKM(t.amount)}</span>
                        <svg viewBox="0 0 120 14" aria-hidden="true">
                          <path d="M2 7h110M104 2l8 5-8 5" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                      </span>
                      <span className="transfer-who">
                        <Avatar name={t.to.displayName} id={t.to.userId} size={36} />
                        <span>{t.to.userId === me.id ? 'Tebi' : t.to.displayName}</span>
                      </span>
                      {mine && (
                        <Button size="sm" variant="green" onClick={() => onSettle({ toUser: t.to.userId, amount: t.amount })}>
                          Platio/la sam
                        </Button>
                      )}
                    </li>
                  )
                })}
              </ul>
            )}

            <div className="settle-extra">
              <p>Uplatu možeš zabilježiti samo u svoje ime. Kad ti neko plati, on to upiše kod sebe.</p>
              <Button size="sm" onClick={() => onSettle(null)}>
                Zabilježi drugu uplatu
              </Button>
            </div>
          </section>
        </>
      )}
    </div>
  )
}
