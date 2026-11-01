# Arhitektura RoomSplita

Dokument opisuje kako je backend organizovan i kako zahtjev prolazi kroz sistem.
Dijelovi označeni kao _planirano_ još nisu implementirani.

## Slojevi

```mermaid
flowchart LR
    Klijent[React / HTTP klijent] --> Sec[Spring Security filteri]
    Sec --> C[Controller]
    C --> S["Service (poslovna logika)"]
    S --> R["Repository (Spring Data JPA)"]
    R --> DB[(PostgreSQL)]
    F[Flyway migracije] -.pri startu.-> DB
```

| Sloj | Odgovornost |
|---|---|
| Security filteri | Autentikacija (JWT, _planirano_) prije nego zahtjev stigne do kontrolera |
| Controller | Prima HTTP zahtjev, validira ulaz, vraća DTO i ispravan HTTP status. Nema poslovne logike |
| Service | Poslovna pravila, autorizacija po grupi, transakcije _(planirano)_ |
| Repository | Pristup bazi preko Spring Data JPA |
| Flyway | Jedini način da se mijenja šema baze |

Paketi su organizovani po funkcionalnosti (npr. `com.emin.roomsplit.health`), a ne po tehničkom sloju.

## Tok zahtjeva (primjer: `GET /api/health`)

```mermaid
sequenceDiagram
    participant K as Klijent
    participant S as Security filter
    participant C as HealthController
    K->>S: GET /api/health
    alt korisnik nije prijavljen
        S-->>K: 401 Unauthorized
    else korisnik je prijavljen
        S->>C: zahtjev prosljeđen
        C-->>K: 200 {"status":"UP"} (HealthResponse record)
    end
```

Napomena: 401 vraća Security filter prije kontrolera, pa 401 samo po sebi ne dokazuje da je
kontroler ispravno povezan. Zato endpoint pokriva test (`HealthControllerTest`) s oba slučaja.

## Autentikacija (JWT)

```mermaid
sequenceDiagram
    participant K as Klijent
    participant A as AuthController / AuthService
    participant T as TokenService
    participant F as Security filter (JwtDecoder)
    participant U as UserController
    K->>A: POST /api/auth/login {email, password}
    A->>A: nađi korisnika, BCrypt provjera lozinke
    A->>T: generateToken(user)
    T-->>A: JWT (subject = id korisnika, potpisan HS256)
    A-->>K: 200 {accessToken, tokenType, expiresInSeconds, user}
    K->>F: GET /api/users/me + Authorization: Bearer token
    F->>F: provjeri potpis i istek
    alt token nevažeći ili istekao
        F-->>K: 401
    else token važeći
        F->>U: zahtjev s Jwt u Security kontekstu
        U-->>K: 200 {id, email, displayName, ...}
    end
```

- Token potpisujemo i provjeravamo istim tajnim ključem (`JWT_SECRET`, najmanje 32 znaka) preko
  Spring Securityjevog `oauth2ResourceServer`-a (Nimbus), pa ne pišemo vlastiti filter.
- U tokenu je samo `subject` (id korisnika) i vremena izdavanja/isteka; lozinka ni hash nikad.
- Serveru ne treba sesija: svaki zahtjev nosi token (`STATELESS`).
- CORS dozvoljava origin iz `CORS_ALLOWED_ORIGINS` (podrazumijevano `http://localhost:5173`, Vite).

## Rukovanje greškama

Sve greške prolaze kroz `GlobalExceptionHandler` (`@RestControllerAdvice`) i vraćaju isti oblik
`ApiError` (`timestamp`, `status`, `message`, opciono `fieldErrors`). Kontroleri ne hvataju izuzetke sami.

| Situacija | Status |
|---|---|
| Neispravan ulaz (`@Valid`) | 400 + `fieldErrors` po polju |
| Email već postoji | 409 |
| Pogrešan email ili lozinka pri prijavi | 401 (ista poruka za oba slučaja) |
| Nema tokena ili je nevažeći/istekao | 401 (`SecurityConfig`) |
| Token važi, ali korisnik više ne postoji | 404 |

## Model podataka

Implementirana je tabela `users` (migracija `V1__create_users.sql`). Ostale tabele su
_planirane_ i dolaze u fazama 3 (Grupe) i 4 (Troškovi i balansi).

```mermaid
erDiagram
    users ||--o{ group_members : "je član"
    groups ||--o{ group_members : "ima članove"
    groups ||--o{ expenses : "sadrži"
    users ||--o{ expenses : "platio"
    expenses ||--o{ expense_shares : "dijeli se na"
    users ||--o{ expense_shares : "duguje dio"
    groups ||--o{ settlements : "sadrži"

    users {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar display_name
        timestamptz created_at
    }
    groups {
        bigint id PK
        varchar name
        varchar join_code UK
        bigint created_by FK
        timestamptz created_at
    }
    group_members {
        bigint group_id PK
        bigint user_id PK
        varchar role
        timestamptz joined_at
    }
    expenses {
        bigint id PK
        bigint group_id FK
        bigint paid_by FK
        varchar description
        numeric amount "NUMERIC(12,2)"
        varchar category
        date expense_date
        timestamptz created_at
    }
    expense_shares {
        bigint id PK
        bigint expense_id FK
        bigint user_id FK
        numeric share_amount "NUMERIC(12,2)"
    }
    settlements {
        bigint id PK
        bigint group_id FK
        bigint from_user FK
        bigint to_user FK
        numeric amount "NUMERIC(12,2)"
        timestamptz settled_at
    }
```

**Invarijanta:** za svaki trošak vrijedi `SUM(share_amount) = amount` (podjela mora zbrojiti
tačno na originalni iznos, bez gubitka centa).

## API (skica)

| Metoda | Putanja | Opis | Status |
|---|---|---|---|
| GET | `/api/health` | provjera da servis radi | ✅ |
| POST | `/api/auth/register` | registracija (201, 400 validacija, 409 zauzet email) | ✅ |
| POST | `/api/auth/login` | prijava, vraća JWT (401 za pogrešan email ili lozinku) | ✅ |
| GET | `/api/users/me` | prijavljeni korisnik (traži `Authorization: Bearer`) | ✅ |
| POST | `/api/groups` | nova grupa | ⏳ |
| POST | `/api/groups/join` | pridruži se kodom | ⏳ |
| GET | `/api/groups` | moje grupe | ⏳ |
| GET | `/api/groups/{id}` | detalji (samo članovi) | ⏳ |
| POST | `/api/groups/{id}/expenses` | novi trošak | ⏳ |
| GET | `/api/groups/{id}/expenses?page=&size=` | lista, paginirano | ⏳ |
| PUT/DELETE | `/api/groups/{id}/expenses/{eid}` | izmjena/brisanje | ⏳ |
| GET | `/api/groups/{id}/balances` | balansi | ⏳ |
| GET | `/api/groups/{id}/settle-suggestions` | predložena poravnanja | ⏳ |
| POST | `/api/groups/{id}/settlements` | zabilježi uplatu | ⏳ |

Svaki endpoint grupe provjerava da je prijavljeni korisnik član te grupe.
