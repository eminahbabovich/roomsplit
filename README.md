# RoomSplit

Aplikacija za dijeljenje zajedničkih troškova među cimerima i prijateljima.
Vodi troškove u grupi, računa ko kome koliko duguje i predlaže najmanji broj
uplata za poravnanje.

> **Status:** u izradi (sedmica 1 od 5 završena). **Live demo:** _uskoro_

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
| Registracija i prijava (JWT, BCrypt) | ⏳ sedmica 2 |
| Grupe, članstvo i pridruživanje kodom | ⏳ sedmica 3 |
| Troškovi (jednaka podjela i tačni iznosi), paginacija, filtriranje | ⏳ sedmica 4 |
| Balansi i algoritam poravnanja (minimalan broj uplata) | ⏳ sedmica 4 |
| React UI i prvi deploy | ⏳ sedmica 5 |

## Tehnologije

- **Backend:** Java 21, Spring Boot 4.1, Spring Security (JWT), Spring Data JPA,
  Bean Validation, PostgreSQL 17, Flyway, JUnit 5, MockMvc
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
PROJECT_PLAN.md      Plan projekta po sedmicama
```

## Screenshotovi

_(Dodati kad UI bude gotov.)_

## Dnevnik učenja

Tok izrade i naučeno po sesijama: [LEARNING_LOG.md](LEARNING_LOG.md).
