import { useState } from 'react'
import { ApiError } from '../../api/client.js'
import { expensesApi } from '../../api/endpoints.js'
import { Avatar } from '../../components/Avatar.jsx'
import { Button } from '../../components/Button.jsx'
import { CATEGORIES } from '../../lib/categories.js'
import { Field } from '../../components/Field.jsx'
import { Modal } from '../../components/Modal.jsx'
import { todayISO } from '../../lib/dates.js'
import { formatKM, fromFeninzi, parseAmount, splitEqual, toFeninzi } from '../../lib/money.js'
import { useToast } from '../../toast/toastContext.js'

// Polja čije greške prikazujemo ispod inputa. Sve ostalo ide u opšti okvir.
const KNOWN_FIELDS = ['description', 'amount', 'paidBy', 'expenseDate', 'participants']

const asInput = (n) => n.toFixed(2).replace('.', ',')

// Odgovor servera ne kaže kako je trošak podijeljen, pa zaključujemo iz iznosa:
// ako dijelovi odgovaraju jednakoj podjeli, otvaramo ga kao "jednako", inače kao "tačni iznosi".
function guessSplitType(expense) {
  const equal = splitEqual(toFeninzi(expense.amount), expense.shares.length)
  const actual = expense.shares.map((s) => toFeninzi(s.amount)).sort((a, b) => b - a)
  return equal.length === actual.length && equal.every((v, i) => v === actual[i]) ? 'EQUAL' : 'EXACT'
}

function ExpenseForm({ groupId, members, me, expense, onClose, onSaved }) {
  const toast = useToast()
  const editing = Boolean(expense)

  const [description, setDescription] = useState(expense?.description ?? '')
  const [amountText, setAmountText] = useState(expense ? asInput(expense.amount) : '')
  const [paidBy, setPaidBy] = useState(expense?.paidBy.userId ?? me.id)
  const [category, setCategory] = useState(expense?.category ?? 'FOOD')
  const [date, setDate] = useState(expense?.expenseDate ?? todayISO())
  const [splitType, setSplitType] = useState(expense ? guessSplitType(expense) : 'EQUAL')
  const [selected, setSelected] = useState(
    () => new Set(expense ? expense.shares.map((s) => s.userId) : members.map((m) => m.userId)),
  )
  const [exact, setExact] = useState(() =>
    Object.fromEntries((expense?.shares ?? []).map((s) => [s.userId, asInput(s.amount)])),
  )
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const amount = parseAmount(amountText)
  const totalF = amount ? toFeninzi(amount) : null
  const participants = members.filter((m) => selected.has(m.userId))

  // Pregled podjele uživo (isti algoritam kao na serveru)
  const equalShares = totalF ? splitEqual(totalF, participants.length) : []
  const exactSumF = participants.reduce((sum, p) => sum + toFeninzi(parseAmount(exact[p.userId]) ?? 0), 0)
  const remainingF = totalF ? totalF - exactSumF : null

  function toggle(userId) {
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(userId)) next.delete(userId)
      else next.add(userId)
      return next
    })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)

    if (amount === null) {
      setError(new ApiError(400, 'Upiši ispravan iznos, najviše 2 decimale.', { amount: 'Upiši ispravan iznos (npr. 12,50).' }))
      return
    }

    const body = {
      description: description.trim(),
      amount,
      paidBy: Number(paidBy),
      category,
      expenseDate: date,
      splitType,
      participants: participants.map((p) =>
        splitType === 'EXACT' ? { userId: p.userId, amount: parseAmount(exact[p.userId]) } : { userId: p.userId },
      ),
    }

    setSubmitting(true)
    try {
      const saved = editing
        ? await expensesApi.update(groupId, expense.id, body)
        : await expensesApi.create(groupId, body)
      toast.success(editing ? 'Trošak je izmijenjen.' : 'Trošak je dodan.')
      onSaved(saved)
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'Trošak nije sačuvan.'))
      setSubmitting(false)
    }
  }

  const fe = error?.fieldErrors ?? {}
  const otherErrors = error
    ? [...Object.entries(fe).filter(([k]) => !KNOWN_FIELDS.includes(k)).map(([, v]) => v), ...(Object.keys(fe).length ? [] : [error.message])]
    : []

  return (
    <form className="form" onSubmit={handleSubmit} noValidate>
      {otherErrors.length > 0 && (
        <div className="notice" role="alert">
          <div>
            {otherErrors.map((m) => (
              <p key={m}>{m}</p>
            ))}
          </div>
        </div>
      )}

      <Field
        label="Šta je plaćeno?"
        placeholder="npr. Namirnice, struja za oktobar"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        error={fe.description}
        maxLength={255}
        autoFocus
      />

      <div className="form-row">
        <Field
          label="Iznos"
          inputMode="decimal"
          placeholder="0,00"
          affix="KM"
          value={amountText}
          onChange={(e) => setAmountText(e.target.value)}
          error={fe.amount}
        />
        <Field label="Datum" type="date" value={date} onChange={(e) => setDate(e.target.value)} error={fe.expenseDate} />
      </div>

      <Field label="Ko je platio?" error={fe.paidBy}>
        {(props) => (
          <select {...props} className="select" value={paidBy} onChange={(e) => setPaidBy(e.target.value)}>
            {members.map((m) => (
              <option key={m.userId} value={m.userId}>
                {m.displayName}
                {m.userId === me.id ? ' (ti)' : ''}
              </option>
            ))}
          </select>
        )}
      </Field>

      <fieldset className="fieldset">
        <legend className="field-label">Kategorija</legend>
        <div className="chip-row">
          {CATEGORIES.map((c) => (
            <label key={c.value} className="chip" data-checked={category === c.value} style={{ '--chip': c.color }}>
              <input type="radio" name="category" value={c.value} checked={category === c.value} onChange={() => setCategory(c.value)} />
              {c.label}
            </label>
          ))}
        </div>
      </fieldset>

      <fieldset className="fieldset">
        <legend className="field-label">Kako se dijeli?</legend>
        <div className="segmented" role="radiogroup">
          {[
            ['EQUAL', 'Jednako'],
            ['EXACT', 'Tačni iznosi'],
          ].map(([value, label]) => (
            <label key={value} data-checked={splitType === value}>
              <input type="radio" name="split" value={value} checked={splitType === value} onChange={() => setSplitType(value)} />
              {label}
            </label>
          ))}
        </div>

        {fe.participants && (
          <p className="field-error" role="alert">
            {fe.participants}
          </p>
        )}

        <ul className="split-list">
          {members.map((m) => {
            const isIn = selected.has(m.userId)
            const index = participants.findIndex((p) => p.userId === m.userId)
            return (
              <li key={m.userId} className="split-row" data-off={!isIn}>
                <label className="split-person">
                  <input type="checkbox" checked={isIn} onChange={() => toggle(m.userId)} />
                  <Avatar name={m.displayName} id={m.userId} size={30} />
                  <span>{m.displayName}</span>
                </label>

                {isIn && splitType === 'EQUAL' && totalF !== null && (
                  <span className="mono split-share">{formatKM(fromFeninzi(equalShares[index]))}</span>
                )}
                {isIn && splitType === 'EXACT' && (
                  <div className="input-affix split-input">
                    <input
                      className="input input-money"
                      inputMode="decimal"
                      placeholder="0,00"
                      aria-label={`Iznos za ${m.displayName}`}
                      value={exact[m.userId] ?? ''}
                      onChange={(e) => setExact((cur) => ({ ...cur, [m.userId]: e.target.value }))}
                    />
                    <span>KM</span>
                  </div>
                )}
              </li>
            )
          })}
        </ul>

        {splitType === 'EQUAL' && totalF !== null && participants.length > 0 && totalF % participants.length !== 0 && (
          <p className="field-hint">Ostatak od feninga ide prvima po redu, pa zbir ostaje tačno {formatKM(amount)}.</p>
        )}
        {splitType === 'EXACT' && remainingF !== null && (
          <p className="remaining mono" data-ok={remainingF === 0} role="status">
            {remainingF === 0
              ? '✓ Zbir se poklapa s iznosom'
              : remainingF > 0
                ? `Još treba rasporediti ${formatKM(fromFeninzi(remainingF))}`
                : `Previše za ${formatKM(fromFeninzi(-remainingF))}`}
          </p>
        )}
      </fieldset>

      <div className="form-actions">
        <Button type="button" variant="ghost" onClick={onClose}>
          Odustani
        </Button>
        <Button type="submit" variant="primary" loading={submitting}>
          {editing ? 'Sačuvaj izmjene' : 'Dodaj trošak'}
        </Button>
      </div>
    </form>
  )
}

export function ExpenseFormModal({ open, onClose, expense, ...rest }) {
  return (
    <Modal open={open} onClose={onClose} title={expense ? 'Izmijeni trošak' : 'Novi trošak'}>
      <ExpenseForm expense={expense} onClose={onClose} {...rest} />
    </Modal>
  )
}
