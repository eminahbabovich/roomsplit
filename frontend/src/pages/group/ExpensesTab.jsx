import { useState } from 'react'
import { ApiError } from '../../api/client.js'
import { expensesApi } from '../../api/endpoints.js'
import { Avatar } from '../../components/Avatar.jsx'
import { Button } from '../../components/Button.jsx'
import { CategoryIcon } from '../../components/CategoryIcon.jsx'
import { CATEGORIES, categoryMeta } from '../../lib/categories.js'
import { EmptyState, ErrorState, SkeletonList } from '../../components/Feedback.jsx'
import { Modal } from '../../components/Modal.jsx'
import { useAsync } from '../../hooks/useAsync.js'
import { formatDay } from '../../lib/dates.js'
import { formatKM } from '../../lib/money.js'
import { useToast } from '../../toast/toastContext.js'
import { ExpenseFormModal } from './ExpenseFormModal.jsx'

const PAGE_SIZE = 8
const NO_FILTERS = { page: 0, category: '', from: '', to: '' }

function ExpenseRow({ expense, me, expanded, onToggle, onEdit, onDelete }) {
  const canEdit = expense.createdBy.userId === me.id
  const people = expense.shares.length

  return (
    <li className="expense" data-expanded={expanded}>
      <button type="button" className="expense-main" onClick={onToggle} aria-expanded={expanded}>
        <CategoryIcon category={expense.category} />
        <span className="expense-text">
          <span className="expense-title">{expense.description}</span>
          <span className="expense-meta">
            {formatDay(expense.expenseDate)} · platio/la {expense.paidBy.userId === me.id ? 'ti' : expense.paidBy.displayName} ·{' '}
            {people} {people % 10 >= 2 && people % 10 <= 4 && (people < 10 || people > 20) ? 'osobe' : 'osoba'}
          </span>
        </span>
        <span className="expense-amount mono">{formatKM(expense.amount)}</span>
        <span className="expense-chevron" aria-hidden="true">
          ▾
        </span>
      </button>

      {expanded && (
        <div className="expense-detail">
          <ul className="receipt-lines">
            {expense.shares.map((s) => (
              <li key={s.userId}>
                <Avatar name={s.displayName} id={s.userId} size={24} />
                <span>{s.userId === me.id ? 'Ti' : s.displayName}</span>
                <span className="leader" />
                <span className="mono">{formatKM(s.amount)}</span>
              </li>
            ))}
            <li className="receipt-total">
              <span>Ukupno</span>
              <span className="leader" />
              <span className="mono">{formatKM(expense.amount)}</span>
            </li>
          </ul>
          <div className="expense-foot">
            <span className="pill pill-green">{categoryMeta(expense.category).label}</span>
            <span className="expense-author">unio/la {expense.createdBy.userId === me.id ? 'ti' : expense.createdBy.displayName}</span>
            {canEdit && (
              <span className="expense-actions">
                <Button size="sm" onClick={onEdit}>
                  Izmijeni
                </Button>
                <Button size="sm" variant="danger" onClick={onDelete}>
                  Obriši
                </Button>
              </span>
            )}
          </div>
        </div>
      )}
    </li>
  )
}

export function ExpensesTab({ groupId, members, me, onChanged }) {
  const toast = useToast()
  const [filters, setFilters] = useState(NO_FILTERS)
  const [expandedId, setExpandedId] = useState(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [deleting, setDeleting] = useState(null)
  const [deleteBusy, setDeleteBusy] = useState(false)

  const { data, error, loading, reload } = useAsync(
    () =>
      expensesApi.list(groupId, {
        page: filters.page,
        size: PAGE_SIZE,
        category: filters.category,
        from: filters.from,
        to: filters.to,
      }),
    JSON.stringify(filters),
  )

  const setFilter = (patch) => setFilters((f) => ({ ...f, ...patch, page: 0 }))
  const hasFilters = filters.category || filters.from || filters.to

  function openNew() {
    setEditing(null)
    setFormOpen(true)
  }

  function openEdit(expense) {
    setEditing(expense)
    setFormOpen(true)
  }

  function handleSaved() {
    setFormOpen(false)
    reload()
    onChanged()
  }

  async function confirmDelete() {
    setDeleteBusy(true)
    try {
      await expensesApi.remove(groupId, deleting.id)
      toast.success('Trošak je obrisan.')
      // ako smo obrisali jedini trošak na stranici, vratimo se jednu stranicu nazad
      if (data.items.length === 1 && filters.page > 0) setFilters((f) => ({ ...f, page: f.page - 1 }))
      else reload()
      setDeleting(null)
      onChanged()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Brisanje nije uspjelo.')
    } finally {
      setDeleteBusy(false)
    }
  }

  return (
    <div className="tab-panel">
      <div className="toolbar">
        <div className="chip-row" role="group" aria-label="Filter po kategoriji">
          <button type="button" className="chip" data-checked={!filters.category} onClick={() => setFilter({ category: '' })}>
            Sve
          </button>
          {CATEGORIES.map((c) => (
            <button
              key={c.value}
              type="button"
              className="chip"
              style={{ '--chip': c.color }}
              data-checked={filters.category === c.value}
              onClick={() => setFilter({ category: filters.category === c.value ? '' : c.value })}
            >
              {c.label}
            </button>
          ))}
        </div>
        <Button variant="primary" onClick={openNew}>
          + Novi trošak
        </Button>
      </div>

      <div className="date-filter">
        <label>
          <span>Od</span>
          <input type="date" className="input" value={filters.from} max={filters.to || undefined} onChange={(e) => setFilter({ from: e.target.value })} />
        </label>
        <label>
          <span>Do</span>
          <input type="date" className="input" value={filters.to} min={filters.from || undefined} onChange={(e) => setFilter({ to: e.target.value })} />
        </label>
        {hasFilters && (
          <Button variant="ghost" size="sm" onClick={() => setFilters(NO_FILTERS)}>
            Poništi filtere
          </Button>
        )}
      </div>

      {loading && !data && <SkeletonList rows={4} height={78} />}
      {error && <ErrorState error={error} onRetry={reload} />}

      {data && data.items.length === 0 && (
        <EmptyState
          title={hasFilters ? 'Nema troškova za ove filtere' : 'Još nema troškova'}
          action={!hasFilters && <Button variant="primary" onClick={openNew}>Dodaj prvi trošak</Button>}
        >
          {hasFilters ? 'Pokušaj s drugom kategorijom ili širim datumima.' : 'Dodaj prvi i RoomSplit odmah računa ko kome duguje.'}
        </EmptyState>
      )}

      {data && data.items.length > 0 && (
        <>
          <ul className="expense-list" data-loading={loading}>
            {data.items.map((expense) => (
              <ExpenseRow
                key={expense.id}
                expense={expense}
                me={me}
                expanded={expandedId === expense.id}
                onToggle={() => setExpandedId(expandedId === expense.id ? null : expense.id)}
                onEdit={() => openEdit(expense)}
                onDelete={() => setDeleting(expense)}
              />
            ))}
          </ul>

          {data.totalPages > 1 && (
            <nav className="pager" aria-label="Stranice troškova">
              <Button size="sm" disabled={data.page === 0 || loading} onClick={() => setFilters((f) => ({ ...f, page: f.page - 1 }))}>
                ← Novije
              </Button>
              <span className="mono">
                {data.page + 1} / {data.totalPages}
              </span>
              <Button size="sm" disabled={data.page + 1 >= data.totalPages || loading} onClick={() => setFilters((f) => ({ ...f, page: f.page + 1 }))}>
                Starije →
              </Button>
            </nav>
          )}
        </>
      )}

      <ExpenseFormModal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        groupId={groupId}
        members={members}
        me={me}
        expense={editing}
        onSaved={handleSaved}
      />

      <Modal open={Boolean(deleting)} onClose={() => setDeleting(null)} title="Obrisati trošak?">
        {deleting && (
          <div className="form">
            <p>
              „{deleting.description}“ ({formatKM(deleting.amount)}) nestaje, a balansi se računaju ponovo bez njega.
            </p>
            <div className="form-actions">
              <Button variant="ghost" onClick={() => setDeleting(null)}>
                Ipak ne
              </Button>
              <Button variant="danger" loading={deleteBusy} onClick={confirmDelete}>
                Da, obriši
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
