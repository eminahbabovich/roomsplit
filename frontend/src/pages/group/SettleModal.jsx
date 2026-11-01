import { useState } from 'react'
import { ApiError } from '../../api/client.js'
import { balancesApi } from '../../api/endpoints.js'
import { Button } from '../../components/Button.jsx'
import { Field } from '../../components/Field.jsx'
import { Modal } from '../../components/Modal.jsx'
import { parseAmount } from '../../lib/money.js'
import { useToast } from '../../toast/toastContext.js'

function SettleForm({ groupId, members, me, preset, onClose, onSaved }) {
  const toast = useToast()
  const others = members.filter((m) => m.userId !== me.id)
  const [toUser, setToUser] = useState(preset?.toUser ?? others[0]?.userId ?? '')
  const [amountText, setAmountText] = useState(preset ? preset.amount.toFixed(2).replace('.', ',') : '')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    const amount = parseAmount(amountText)
    if (amount === null) {
      setError(new ApiError(400, '', { amount: 'Upiši ispravan iznos (npr. 30,00).' }))
      return
    }
    setSubmitting(true)
    try {
      await balancesApi.settle(groupId, { toUser: Number(toUser), amount })
      toast.success('Uplata je zabilježena.')
      onSaved()
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, 'Uplata nije zabilježena.'))
      setSubmitting(false)
    }
  }

  const fe = error?.fieldErrors ?? {}
  const general = error && !Object.keys(fe).length ? error.message : null

  return (
    <form className="form" onSubmit={handleSubmit} noValidate>
      <p className="settle-lead">
        Bilježiš da <strong>si ti isplatio/la</strong> nekoga iz grupe. Uplatu uvijek upisuje onaj ko plaća.
      </p>
      {general && (
        <div className="notice" role="alert">
          {general}
        </div>
      )}
      <Field label="Kome si platio/la?" error={fe.toUser}>
        {(props) => (
          <select {...props} className="select" value={toUser} onChange={(e) => setToUser(e.target.value)}>
            {others.map((m) => (
              <option key={m.userId} value={m.userId}>
                {m.displayName}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field
        label="Iznos"
        inputMode="decimal"
        placeholder="0,00"
        affix="KM"
        value={amountText}
        onChange={(e) => setAmountText(e.target.value)}
        error={fe.amount}
        autoFocus
      />
      <div className="form-actions">
        <Button type="button" variant="ghost" onClick={onClose}>
          Odustani
        </Button>
        <Button type="submit" variant="green" loading={submitting} disabled={others.length === 0}>
          Zabilježi uplatu
        </Button>
      </div>
    </form>
  )
}

export function SettleModal({ open, onClose, preset, ...rest }) {
  return (
    <Modal open={open} onClose={onClose} title="Zabilježi uplatu">
      <SettleForm preset={preset} onClose={onClose} {...rest} />
    </Modal>
  )
}
