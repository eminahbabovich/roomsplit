# Dnevnik učenja

Popunjava se na kraju svake sesije (Claude ga dopisuje, ja provjeravam i ispravljam
svojim riječima).

Format unosa:

## YYYY-MM-DD - tema sesije
- **Naučio sam:** ...
- **Greške koje sam napravio:** ...
- **Pitanja za ponavljanje (kao na intervjuu):**
  1. ...
  2. ...
  3. ...
- **Šta mi još nije jasno:** ...

---

## 2026-11-01 (nedjelja, 12:22) - Sedmica 1: Postgres u Dockeru, Flyway, prvi endpoint

- **Naučio sam:**
  - Postgres pokrećem kroz `docker-compose.yml`; podaci žive u named volumeu, pa `docker compose down` briše kontejner, a podaci ostaju. `down -v` briše i volume, tj. sve podatke.
  - Tajne (lozinka baze) idu u `.env` koji je u `.gitignore`; u repo ide samo `.env.example` s lažnim vrijednostima.
  - Spring Boot ne čita `.env` sam, čita varijable okruženja procesa. U `application.properties` koristim `${POSTGRES_DB}` placeholdere.
  - `ddl-auto=validate` + Flyway: šemu mijenjaju samo verzionisane SQL migracije (`V1__create_users.sql`), a Hibernate samo provjerava da se entiteti slažu s bazom.
  - Primijenjena migracija se ne mijenja (checksum u `flyway_schema_history`); nova promjena ide u `V2__...`.
  - `GENERATED ALWAYS AS IDENTITY`, `UNIQUE` na emailu (baza brani duplikate), `TIMESTAMPTZ` za vrijeme, u bazi čuvam `password_hash`, nikad lozinku.
  - DTO kao Java `record` (`HealthResponse`) i `@RestController`/`@GetMapping`; kontroler nema poslovne logike.
  - 401 na `/api/health` ne dokazuje da kontroler radi: Spring Security je filter ispred kontrolera i vraća 401 i za putanje koje ne postoje.
  - `@WebMvcTest` podiže samo web sloj (bez baze), `MockMvc` šalje lažne HTTP zahtjeve, `@WithMockUser` glumi prijavljenog korisnika. Dva testa: prijavljen korisnik dobija 200 i `{"status":"UP"}`, neprijavljen dobija 401.
  - Test s constructor injectionom treba `@Autowired` na konstruktoru, inače JUnit ne zna odakle da uzme `MockMvc`.
- **Greške koje sam napravio:** mislio sam da 401 znači da endpoint radi. Zapravo samo znači da je aplikacija pokrenuta i da je Security aktivan; da kontroler radi dokazuje tek test. Također: prvi test je pao s `No ParameterResolver registered for parameter [MockMvc]` jer konstruktor nije imao `@Autowired`.
- **Pitanja za ponavljanje (kao na intervjuu):**
  1. Zašto u projektu koristiš Flyway i `ddl-auto=validate`, a ne `ddl-auto=update`?
  2. Šta je razlika između `docker compose down` i `docker compose down -v` i zašto je to bitno za bazu?
  3. Zašto API ne smije vraćati JPA entitet `User` direktno, nego DTO?
  4. Koja je razlika između `@WebMvcTest` i `@SpringBootTest`, i zašto si za kontroler birao `@WebMvcTest`?
  5. Endpoint vraća 401. Šta to dokazuje, a šta ne dokazuje o kontroleru?
- **Šta mi još nije jasno:** (dopuni svojim riječima)
