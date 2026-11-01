# RoomSplit

Aplikacija za dijeljenje zajedničkih troškova među cimerima i prijateljima.
Vodi troškove u grupi, računa ko kome koliko duguje i predlaže najmanji broj
uplata za poravnanje.

> **Status:** u izradi (faza 1 od 5 gotova, sljedeća: Autentikacija). **Live demo:** _uskoro_

## Problem

Cimeri i studenti dijele troškove (hrana, struja, putovanja) i stalno se zbune ko kome
koliko duguje. RoomSplit vodi sve troškove na jednom mjestu, tačno računa balanse
(bez grešaka u centima) i predlaže minimalan broj uplata kojim se svi poravnaju.

## Mogućnosti

| Mogućnost | Status |
|---|---|
| Docker Compose za lokalni PostgreSQL | ✅ gotovo |
| Verzionisane migracije baze (Flyway), tabela `users` | ✅ gotovo |
| Health endpoint `GET /api/health` s integracionim testom | ✅ gotovo |
| Registracija (`POST /api/auth/register`, BCrypt, validacija, 409 za zauzet email) | ✅ gotovo |
| Prijava (`POST /api/auth/login`), JWT (HS256, istek 60 min), zaštićen `GET /api/users/me`, CORS | ✅ gotovo |
| Grupe, članstvo i pridruživanje kodom | ⏳ faza 3: Grupe |
| Troškovi (jednaka podjela i tačni iznosi), paginacija, filtriranje | ⏳ faza 4: Troškovi i balansi |
| Balansi i algoritam poravnanja (minimalan broj uplata) | ⏳ faza 4: Troškovi i balansi |
| React UI i prvi deploy | ⏳ faza 5: Frontend i deploy |

## Tehnologije

- **Backend:** Java 21, Spring Boot 4.1, Spring Security (JWT), Spring Data JPA,
  Bean Validation, Lombok, PostgreSQL 17, Flyway, JUnit 5, MockMvc
- **Frontend:** React, Vite
- **Infrastruktura:** Docker Compose (lokalna baza); deploy _(upisati kad bude postavljeno)_

## Arhitektura

Troslojna arhitektura: `Controller → Service → Repository`. Kontroleri nemaju
poslovnu logiku, a API nikad ne vraća JPA entitete nego DTO-ove (Java `record`).
Šemu baze mijenja isključivo Flyway, a Hibernate je podešen samo da je provjerava
(`ddl-auto=validate`).

Detalji: [docs/ARHITEKTURA.md](docs/ARHITEKTURA.md) (slojevi, tok zahtjeva, model podataka)
i [docs/ODLUKE.md](docs/ODLUKE.md) (ključne tehničke odluke s obrazloženjem).

## Pokretanje lokalno

Potrebno: Java 21, Docker Desktop. (Node.js treba tek kad frontend dobije sadržaj.)

1. **Podesi varijable okruženja.** Kopiraj `.env.example` u `.env` i upiši svoje vrijednosti
   (`.env` je u `.gitignore` i nikad ne ide na GitHub):
   ```powershell
   Copy-Item .env.example .env
   ```
   U `.env` zamijeni i `JWT_SECRET` svojim dugim nasumičnim nizom (najmanje 32 znaka; primjer
   kako ga generisati je u `.env.example`). Aplikacija ne starta ako je ključ prekratak.
2. **Pokreni bazu:**
   ```powershell
   docker compose up -d
   ```
3. **Učitaj varijable u terminal i pokreni backend** (Spring Boot ne čita `.env` sam):
   ```powershell
   Get-Content .env | ForEach-Object { $k,$v = $_ -split '=',2; Set-Item "env:$k" $v }
   cd backend
   .\mvnw.cmd spring-boot:run
   ```
   Pri startu Flyway sam primijeni migracije iz `backend/src/main/resources/db/migration`.
4. **Testovi:**
   ```powershell
   cd backend
   .\mvnw.cmd test
   ```

Na Linuxu/macOS-u koristi `./mvnw` umjesto `.\mvnw.cmd`.

## Struktura projekta

```
backend/    Spring Boot API (Maven)
frontend/   React aplikacija (Vite)
docs/       Arhitektura i tehničke odluke
docker-compose.yml   Lokalni PostgreSQL
.env.example         Primjer varijabli okruženja (bez stvarnih tajni)
LEARNING_LOG.md      Dnevnik učenja tokom izrade
PROJECT_PLAN.md      Plan projekta po fazama
```

## Screenshotovi

_(Dodati kad UI bude gotov.)_

## Dnevnik učenja

Tok izrade i naučeno po sesijama: [LEARNING_LOG.md](LEARNING_LOG.md).
