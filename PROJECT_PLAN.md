# Plan portfolija

Cilj: 2 live projekta + portfolio sajt.
Backend: Spring Boot (učim). Frontend: React (znam).

Rad je podijeljen na **faze**, ne na sedmice. Faze se rade redom, ali tempo je
neujednačen: nekad se uradi veliki komad, nekad ništa. Faza je gotova kad je zadovoljen
njen kriterij "gotovo", a ne kad prođe neko vrijeme. Status svake faze vodi se u tabeli
ispod i u `README.md`.

---

# PROJEKAT 1: RoomSplit (faze 1-5)

**Problem:** cimeri i studenti dijele troškove (hrana, račun za struju, putovanje) i
stalno se zbunjuju ko kome koliko duguje. Aplikacija vodi zajedničke troškove u
grupi i računa tko kome duguje, uz minimalan broj uplata za poravnanje.

**Zašto je dobar za portfolio:** stvaran problem, novac i tačnost (BigDecimal),
relacije u bazi, autorizacija po grupi, netrivijalan algoritam, deploy.

## Funkcionalnosti (MVP)

1. Registracija i prijava (JWT), BCrypt lozinke.
2. Grupe: kreiranje, pozivanje članova (kod za pridruživanje), pregled članova.
3. Troškovi: dodavanje troška (opis, iznos, ko je platio, između koga se dijeli),
   izmjena i brisanje (samo autor), paginacija i filtriranje po datumu/kategoriji.
4. Podjela: jednako, ili po tačnim iznosima. (Procenti = bonus.)
5. Balansi: ko je koliko u plusu/minusu u grupi.
6. Poravnanje ("settle up"): predloženi minimalni skup uplata + bilježenje uplate.
7. React UI: login, lista grupa, detalji grupe (troškovi + balansi), forma za trošak.

## Model podataka (početna verzija, doradit ćemo uz Claude-a)

- `users` (id, email UNIQUE, password_hash, display_name, created_at)
- `groups` (id, name, join_code UNIQUE, created_by, created_at)
- `group_members` (group_id, user_id, role, joined_at) - PK (group_id, user_id)
- `expenses` (id, group_id, paid_by, description, amount NUMERIC(12,2), category, expense_date, created_at)
- `expense_shares` (id, expense_id, user_id, share_amount NUMERIC(12,2))
- `settlements` (id, group_id, from_user, to_user, amount NUMERIC(12,2), settled_at)

Invarijanta: za svaki trošak `SUM(share_amount) = amount`.

## API (skica)

| Metoda | Putanja | Opis |
|---|---|---|
| POST | /api/auth/register | registracija |
| POST | /api/auth/login | prijava, vraća JWT |
| POST | /api/groups | nova grupa |
| POST | /api/groups/join | pridruži se kodom |
| GET | /api/groups | moje grupe |
| GET | /api/groups/{id} | detalji (samo članovi) |
| POST | /api/groups/{id}/expenses | novi trošak |
| GET | /api/groups/{id}/expenses?page=&size= | lista, paginirano |
| PUT/DELETE | /api/groups/{id}/expenses/{eid} | izmjena/brisanje |
| GET | /api/groups/{id}/balances | balansi |
| GET | /api/groups/{id}/settle-suggestions | predložena poravnanja |
| POST | /api/groups/{id}/settlements | zabilježi uplatu |

## Faze

| Faza | Naziv | Fokus | Šta ja učim | Status |
|---|---|---|---|---|
| 1 | Temelji | Setup (Initializr, Postgres u Dockeru, Flyway), prvi endpoint, prvi test | Spring Boot anatomija, slojevi, DI, `@RestController`, DTO | ✅ gotovo |
| 2 | Autentikacija | Korisnici + Spring Security + JWT | Filter chain, `PasswordEncoder`, stateless auth, CORS | ✅ gotovo |
| 3 | Grupe | Grupe i članstvo, autorizacija po grupi | JPA relacije, `@Transactional`, validacija, `@RestControllerAdvice` | ✅ gotovo |
| 4 | Troškovi i balansi | Troškovi, podjela, balansi, algoritam poravnanja | `BigDecimal`, zaokruživanje, unit testovi algoritma, paginacija | ✅ gotovo |
| 5 | Frontend (lokalno) | React UI koji radi lokalno sa cijelim backendom: prijava, grupe, troškovi, balansi, uplate | Pravila hookova, API sloj, JWT u klijentu, zaštićene rute, loading/error stanja | |
| 6 | Izmjene i deploy | Ispravke i dorade UI-ja poslije pregleda + **prvi deploy** (Render/Railway + Vercel/Netlify, Neon/Supabase) | CORS u produkciji, env varijable, Dockerfile, DNS | |

Pravilo: prvi deploy se radi u fazi 6, ne na kraju cijelog portfolija. Prvi deploy uvijek
donese neočekivane greške.

## Definicija "gotovo" za Projekat 1

- [ ] Sve MVP funkcionalnosti rade u produkciji (live URL)
- [ ] Barem 15 testova (unit + integracioni), posebno za algoritam podjele
- [ ] README: opis, screenshotovi, arhitektura, kako pokrenuti lokalno, live link
- [ ] Nema tajni u repozitoriju (provjereno)
- [ ] Mogu bez gledanja objasniti: tok zahtjeva, JWT prijavu, relacije, zašto BigDecimal

---

# PROJEKAT 2: SlotBook (faze 1-5)

**Problem:** zajednički resursi (učionice za učenje, prostor za sastanke, termini
konsultacija, teretana) rezerviraju se preko poruka i tabela, uz preklapanja i zabune.
Aplikacija omogućava rezervaciju termina bez preklapanja, s ulogama i potvrdom.

**Zašto je različit od P1:** vrijeme i konflikti (preklapanje termina), konkurentnost
(dva korisnika rezerviraju isti termin), uloge, zakazani poslovi, upload slika,
admin panel. Pokriva ono što RoomSplit ne pokriva.

## Funkcionalnosti (MVP)

1. Uloge: `ADMIN`, `PROVIDER` (vlasnik resursa), `CLIENT`.
2. Resursi (prostor/osoba) s radnim vremenom i trajanjem termina.
3. Kalendar slobodnih termina (React kalendar).
4. Rezervacija s provjerom preklapanja i zaštitom od istovremene rezervacije
   (pesimistično zaključavanje ili unique constraint + obrada greške).
5. Otkazivanje, pravila (npr. ne manje od 2h prije termina).
6. Zakazani posao (`@Scheduled`): podsjetnik/email i automatsko isteklih rezervacija.
7. Upload slike resursa.
8. Admin pregled: statistika rezervacija.

## Faze (okvirno, detalji kad završimo P1)

| Faza | Fokus |
|---|---|
| 1 | Model, uloge, resursi (CRUD), method security (`@PreAuthorize`) |
| 2 | Logika termina i preklapanja, testovi vremena |
| 3 | Konkurentnost, `@Scheduled`, email |
| 4 | React kalendar + admin |
| 5 | Deploy, README, screenshotovi |

---

# Završna faza: Portfolio sajt

React stranica: o meni, 2 projekta (opis problema, stack, live demo, GitHub),
kontakt, CV. Vlastiti domen (Cloudflare Registrar ili Namecheap), hosting na
Vercel/Netlify/Cloudflare Pages. Detaljne korake radimo zajedno kad dođemo do tog dijela.

---

# Bonus ideje ako ostane vremena

- Docker Compose za lokalno pokretanje cijelog sistema jednom komandom
- GitHub Actions: automatski testovi na svaki push
- Swagger/OpenAPI dokumentacija (springdoc)
