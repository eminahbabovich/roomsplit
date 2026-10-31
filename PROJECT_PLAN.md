# Plan portfolija

Cilj: 2 live projekta + portfolio sajt za 10-12 sedmica (3 dana x 3 sata sedmično).
Backend: Spring Boot (učim). Frontend: React (znam).

---

# PROJEKAT 1: RoomSplit (sedmice 1-5)

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

## Plan po sedmicama

| Sedmica | Fokus | Šta ja učim |
|---|---|---|
| 1 | Setup (Initializr, Postgres u Dockeru, Flyway), prvi endpoint, prvi test | Spring Boot anatomija, slojevi, DI, `@RestController`, DTO |
| 2 | Korisnici + Spring Security + JWT | Filter chain, `PasswordEncoder`, stateless auth, CORS |
| 3 | Grupe i članstvo, autorizacija po grupi | JPA relacije, `@Transactional`, validacija, `@RestControllerAdvice` |
| 4 | Troškovi, podjela, balansi, algoritam poravnanja | `BigDecimal`, zaokruživanje, unit testovi algoritma, paginacija |
| 5 | React UI + **prvi deploy** (Render/Railway + Vercel/Netlify, Neon/Supabase) | CORS u produkciji, env varijable, Dockerfile, DNS |

Pravilo: deploy u sedmici 5, ne na kraju. Prvi deploy uvijek donese neočekivane greške.

## Definicija "gotovo" za Projekat 1

- [ ] Sve MVP funkcionalnosti rade u produkciji (live URL)
- [ ] Barem 15 testova (unit + integracioni), posebno za algoritam podjele
- [ ] README: opis, screenshotovi, arhitektura, kako pokrenuti lokalno, live link
- [ ] Nema tajni u repozitoriju (provjereno)
- [ ] Mogu bez gledanja objasniti: tok zahtjeva, JWT prijavu, relacije, zašto BigDecimal

---

# PROJEKAT 2: SlotBook (sedmice 6-10)

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

## Plan po sedmicama (okvirno, detalji kad završimo P1)

| Sedmica | Fokus |
|---|---|
| 6 | Model, uloge, resursi (CRUD), method security (`@PreAuthorize`) |
| 7 | Logika termina i preklapanja, testovi vremena |
| 8 | Konkurentnost, `@Scheduled`, email |
| 9 | React kalendar + admin |
| 10 | Deploy, README, screenshotovi |

---

# Sedmice 11-12: Portfolio sajt

React stranica: o meni, 2 projekta (opis problema, stack, live demo, GitHub),
kontakt, CV. Vlastiti domen (Cloudflare Registrar ili Namecheap), hosting na
Vercel/Netlify/Cloudflare Pages. Detaljne korake radimo zajedno kad dođemo do tog dijela.

---

# Bonus ideje ako ostane vremena

- Docker Compose za lokalno pokretanje cijelog sistema jednom komandom
- GitHub Actions: automatski testovi na svaki push
- Swagger/OpenAPI dokumentacija (springdoc)
