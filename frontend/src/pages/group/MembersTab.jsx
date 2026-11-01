import { Avatar } from '../../components/Avatar.jsx'
import { formatInstant } from '../../lib/dates.js'
import { CopyCode } from './CopyCode.jsx'

export function MembersTab({ group, me }) {
  return (
    <div className="tab-panel">
      <section className="invite card pad" aria-labelledby="invite-title">
        <div>
          <p className="eyebrow">Pozovi cimere</p>
          <h2 id="invite-title">Kod za pridruživanje</h2>
          <p className="invite-note">Pošalji ga ljudima koje želiš u grupi. Ko ima kod i račun, može ući.</p>
        </div>
        <CopyCode code={group.joinCode} large />
      </section>

      <h2 className="section-title">Članovi ({group.members.length})</h2>
      <ul className="members">
        {group.members.map((m) => (
          <li key={m.userId} className="member card">
            <Avatar name={m.displayName} id={m.userId} size={44} />
            <div>
              <p className="member-name">
                {m.displayName}
                {m.userId === me.id && <em> (ti)</em>}
              </p>
              <p className="member-meta">u grupi od {formatInstant(m.joinedAt)}</p>
            </div>
            <span className={`pill ${m.role === 'OWNER' ? 'pill-sun' : ''}`}>{m.role === 'OWNER' ? 'vlasnik' : 'član'}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
