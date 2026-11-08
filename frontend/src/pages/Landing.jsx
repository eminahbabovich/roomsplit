import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth.js'
import { Avatar } from '../components/Avatar.jsx'
import { Reveal } from '../components/Reveal.jsx'
import { formatKM, fromFeninzi, parseAmount, splitEqual, toFeninzi } from '../lib/money.js'

/* ---------- hero ---------- */

function HeroReceipt() {
  const rows = [
    ['Emin', '33,34'],
    ['Emir', '33,33'],
    ['Tarik', '33,33'],
  ]
  return (
    <div className="hero-art" aria-hidden="true">
      <div className="hero-slot" />

      <div className="receipt receipt-main">
        <p className="receipt-tag mono">primjer</p>
        <h3>Namirnice</h3>
        <p className="receipt-sub mono">01.11. · plaćeno: Emin</p>
        <div className="receipt-rule" />
        <div className="receipt-line receipt-line-big" style={{ '--d': '0.9s' }}>
          <span>Ukupno</span>
          <span className="leader" />
          <span className="mono">100,00 KM</span>
        </div>
        <div className="receipt-rule receipt-rule-dash" />
        {rows.map(([name, amount], i) => (
          <div key={name} className="receipt-line" style={{ '--d': `${1.2 + i * 0.28}s` }}>
            <Avatar name={name} id={i} size={22} />
            <span>{name}</span>
            <span className="leader" />
            <span className="mono">{amount}</span>
          </div>
        ))}
        <div className="receipt-rule receipt-rule-dash" />
        <div className="receipt-line receipt-line-big" style={{ '--d': '2.1s' }}>
          <span>Zbir dijelova</span>
          <span className="leader" />
          <span className="mono">100,00 KM</span>
        </div>
        <span className="stamp mono">do feninga</span>
      </div>

      <div className="receipt receipt-back">
        <p className="receipt-tag mono">balansi</p>
        <div className="mini-bar"><i style={{ '--w': '78%' }} data-state="pos" /><b className="mono">+66,66</b></div>
        <div className="mini-bar"><i style={{ '--w': '39%' }} data-state="neg" /><b className="mono">−33,33</b></div>
        <div className="mini-bar"><i style={{ '--w': '39%' }} data-state="neg" /><b className="mono">−33,33</b></div>
      </div>

      <span className="sticker sticker-a mono">1 KM = 100 feninga</span>
      <span className="sticker sticker-b">2 uplate i svi su kvit</span>
    </div>
  )
}

function Hero() {
  const { status } = useAuth()
  const authed = status === 'authed'
  return (
    <section className="hero">
      <div className="container hero-grid">
        <div className="hero-copy">
          <p className="eyebrow">Zajednički troškovi, bez svađe</p>
          <h1>
            Podijeli račun,
            <br />
            ne <span className="scribble">prijateljstvo.</span>
          </h1>
          <p className="hero-lead">
            RoomSplit vodi troškove stana, ekskurzije ili cijele ekipe. Računa ko kome koliko duguje, do posljednjeg
            feninga, i kaže ti koliko je najmanje uplata potrebno da svi budu kvit.
          </p>
          <div className="hero-cta">
            {authed ? (
              <Link to="/grupe" className="btn btn-primary btn-lg">
                Idi na moje grupe →
              </Link>
            ) : (
              <>
                <Link to="/registracija" className="btn btn-primary btn-lg">
                  Napravi grupu
                </Link>
                <Link to="/prijava" className="btn btn-lg">
                  Već imam račun
                </Link>
              </>
            )}
          </div>
          <p className="hero-fine mono">Java · Spring Boot · PostgreSQL · React</p>
        </div>
        <HeroReceipt />
      </div>
    </section>
  )
}

/* ---------- traka ---------- */

const WORDS = ['kirija', 'struja', 'voda', 'internet', 'pizza', 'benzin', 'roštilj', 'kafa', 'namirnice', 'Netflix', 'putovanje', 'toalet papir']

function Marquee() {
  const items = [...WORDS, ...WORDS]
  return (
    <div className="marquee" aria-hidden="true">
      <div className="marquee-track">
        {items.map((w, i) => (
          <span key={i}>
            {w}
            <i>✺</i>
          </span>
        ))}
      </div>
    </div>
  )
}

/* ---------- kako radi ---------- */

const STEPS = [
  ['01', 'Napravi grupu', 'Stan, putovanje, ekipa za petak. Dobiješ kod od osam znakova i pošalješ ga cimerima.'],
  ['02', 'Upiši šta je plaćeno', 'Ko je platio, koliko, i ko je učestvovao. Podijeli jednako ili svakome tačan iznos.'],
  ['03', 'Vidi ko kome duguje', 'Balansi se računaju sami. RoomSplit predloži najmanji broj uplata da svi budu kvit.'],
]

function HowItWorks() {
  return (
    <section className="section" id="kako-radi">
      <div className="container">
        <Reveal>
          <p className="eyebrow">Kako radi</p>
          <h2 className="section-h">Tri koraka, nijedna tabela.</h2>
        </Reveal>
        <ol className="steps">
          {STEPS.map(([n, title, text], i) => (
            <Reveal as="li" key={n} delay={i * 120} className="step">
              <span className="step-n">{n}</span>
              <h3>{title}</h3>
              <p>{text}</p>
            </Reveal>
          ))}
        </ol>
      </div>
    </section>
  )
}

/* ---------- kalkulator ---------- */

const NAMES = ['Emin', 'Emir', 'Tarik', 'Lejla', 'Mak', 'Amina', 'Haris', 'Sead']

function SplitPlayground() {
  const [amountText, setAmountText] = useState('100,00')
  const [people, setPeople] = useState(3)

  const amount = parseAmount(amountText)
  const shares = amount ? splitEqual(toFeninzi(amount), people) : []
  const sum = shares.reduce((a, b) => a + b, 0)

  return (
    <section className="section section-tint" id="isprobaj">
      <div className="container playground">
        <Reveal className="playground-copy">
          <p className="eyebrow">Isprobaj odmah</p>
          <h2 className="section-h">Šta se desi sa feningom koji ostane?</h2>
          <p>
            100,00 KM na troje ne ide na tri jednaka dijela. Većina aplikacija tiho zaokruži i nekome &ldquo;nestane&rdquo; fening.
            RoomSplit računa u feninzima kao cijelim brojevima, pa ostatak razdijeli po jedan fening, i zbir je uvijek tačan.
          </p>
          <p className="playground-note">Ovo dole je isti algoritam kao na serveru. Mijenjaj iznos i broj ljudi.</p>
        </Reveal>

        <Reveal delay={120} className="card calc">
          <div className="calc-controls">
            <label className="field">
              <span className="field-label">Iznos računa</span>
              <div className="input-affix">
                <input
                  className="input input-money"
                  inputMode="decimal"
                  value={amountText}
                  onChange={(e) => setAmountText(e.target.value)}
                  aria-invalid={amount === null}
                />
                <span>KM</span>
              </div>
            </label>
            <div className="field">
              <span className="field-label">Ljudi</span>
              <div className="stepper">
                <button type="button" onClick={() => setPeople((p) => Math.max(2, p - 1))} aria-label="Manje ljudi">
                  −
                </button>
                <span className="mono" aria-live="polite">
                  {people}
                </span>
                <button type="button" onClick={() => setPeople((p) => Math.min(8, p + 1))} aria-label="Više ljudi">
                  +
                </button>
              </div>
            </div>
          </div>

          {amount ? (
            <>
              <ul className="calc-shares">
                {shares.map((s, i) => {
                  const bumped = s > shares[shares.length - 1]
                  return (
                    <li key={i} data-bumped={bumped}>
                      <Avatar name={NAMES[i]} id={i} size={28} />
                      <span>{NAMES[i]}</span>
                      <span className="leader" />
                      <span className="mono">{formatKM(fromFeninzi(s))}</span>
                      {bumped && <em title="Ovdje ide fening viška iz ostatka">+1 fening</em>}
                    </li>
                  )
                })}
              </ul>
              <p className="calc-sum mono" data-ok={sum === toFeninzi(amount)}>
                Zbir: {formatKM(fromFeninzi(sum))} ✓ tačno
              </p>
            </>
          ) : (
            <p className="calc-sum mono">Upiši iznos poput 12,50.</p>
          )}
        </Reveal>
      </div>
    </section>
  )
}

/* ---------- najmanje uplata ---------- */

const PEOPLE = [
  ['Emin', 0, '+40,00'],
  ['Emir', 1, '+20,00'],
  ['Tarik', 2, '−30,00'],
  ['Mak', 3, '−30,00'],
]

const NAIVE = [
  ['Tarik', 'Emin', '20,00'],
  ['Tarik', 'Emir', '10,00'],
  ['Mak', 'Emin', '20,00'],
  ['Mak', 'Emir', '10,00'],
]

const MINIMAL = [
  ['Tarik', 'Emin', '30,00'],
  ['Mak', 'Emin', '10,00'],
  ['Mak', 'Emir', '20,00'],
]

function Settlement() {
  const [smart, setSmart] = useState(true)
  const list = smart ? MINIMAL : NAIVE

  return (
    <section className="section" id="poravnanje">
      <div className="container settle">
        <Reveal>
          <p className="eyebrow">Poravnanje</p>
          <h2 className="section-h">Manje prebacivanja, isti rezultat.</h2>
          <p className="settle-copy">
            Kad se nakupi nekoliko troškova, svako svakome duguje po malo. RoomSplit pronađe najmanji mogući broj uplata
            kojim svi završe na nuli. Ne približno, nego tačno.
          </p>
          <div className="people-row" aria-label="Primjer balansa">
            {PEOPLE.map(([name, id, bal]) => (
              <div key={name} className="person-chip" data-state={bal.startsWith('+') ? 'pos' : 'neg'}>
                <Avatar name={name} id={id} size={34} />
                <span>{name}</span>
                <b className="mono">{bal}</b>
              </div>
            ))}
          </div>
          <p className="playground-note">Primjer s izmišljenim ljudima i iznosima.</p>
        </Reveal>

        <Reveal delay={120} className="card settle-card">
          <div className="segmented" role="radiogroup" aria-label="Način poravnanja">
            <label data-checked={!smart}>
              <input type="radio" name="mode" checked={!smart} onChange={() => setSmart(false)} />
              Svako svakome
            </label>
            <label data-checked={smart}>
              <input type="radio" name="mode" checked={smart} onChange={() => setSmart(true)} />
              RoomSplit
            </label>
          </div>

          <p className="settle-count">
            <span className="settle-big" key={list.length}>
              {list.length}
            </span>{' '}
            uplate
          </p>

          <ul className="settle-list" key={smart ? 'smart' : 'naive'}>
            {list.map(([from, to, amount], i) => (
              <li key={`${from}${to}`} style={{ '--i': i }}>
                <b>{from}</b>
                <span className="settle-arrow" aria-hidden="true">
                  →
                </span>
                <b>{to}</b>
                <span className="leader" />
                <span className="mono">{amount} KM</span>
              </li>
            ))}
          </ul>
        </Reveal>
      </div>
    </section>
  )
}

/* ---------- mogućnosti ---------- */

const FEATURES = [
  ['Tačno do feninga', 'Novac je BigDecimal na serveru i cijeli brojevi feninga u proračunu. Nema double-a, nema zaokruživanja koje krade.', 'var(--sun-l)'],
  ['Najmanje uplata', 'Algoritam traži najmanji broj prebacivanja novca, pa nema lanaca od pet ljudi koji sebi vraćaju.', 'var(--sage)'],
  ['Jednako ili tačno', 'Podijeli računa na sve jednako ili upiši tačan iznos za svakoga. Zbir se provjerava prije snimanja.', 'var(--terra-l)'],
  ['Samo tvoja ekipa', 'Grupu vide samo članovi. Ko nije unutra, dobija isti odgovor kao da grupa ne postoji.', 'var(--sky)'],
  ['Kategorije i filteri', 'Hrana, režije, stanarina, prijevoz, zabava. Filtriraj po kategoriji i datumu, stranicu po stranicu.', '#f1d3e0'],
  ['Ispravke bez drame', 'Pogrešan iznos? Trošak mijenja ili briše samo onaj ko ga je unio, a balansi se odmah preračunaju.', 'var(--paper-2)'],
]

function Features() {
  return (
    <section className="section section-tint">
      <div className="container">
        <Reveal>
          <p className="eyebrow">Šta dobijaš</p>
          <h2 className="section-h">Malo stvari, ali urađenih kako treba.</h2>
        </Reveal>
        <ul className="features">
          {FEATURES.map(([title, text, color], i) => (
            <Reveal as="li" key={title} delay={(i % 3) * 100} className="feature" style={{ '--tint': color }}>
              <h3>{title}</h3>
              <p>{text}</p>
            </Reveal>
          ))}
        </ul>
      </div>
    </section>
  )
}

/* ---------- završni poziv ---------- */

function FinalCta() {
  const { status } = useAuth()
  return (
    <section className="section final">
      <div className="container">
        <Reveal className="final-card">
          <h2>Napravi grupu prije nego neko opet &ldquo;zaboravi novčanik&rdquo;.</h2>
          <p>Traje manje nego pisanje poruke u grupni chat.</p>
          <Link to={status === 'authed' ? '/grupe' : '/registracija'} className="btn btn-sun btn-lg">
            {status === 'authed' ? 'Moje grupe →' : 'Počni →'}
          </Link>
        </Reveal>
      </div>
    </section>
  )
}

export function Landing() {
  return (
    <>
      <Hero />
      <Marquee />
      <HowItWorks />
      <SplitPlayground />
      <Settlement />
      <Features />
      <FinalCta />
    </>
  )
}
