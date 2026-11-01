# Dnevnik učenja

Hronološki zapis šta sam naučio tokom izrade portfolija (RoomSplit, pa SlotBook).
Svaki unos ima datum, vrijeme i temu sesije. Dopisuje se na kraju svake sesije.

Format unosa:

## YYYY-MM-DD (dan, HH:MM) - Sedmica N: tema
- **Naučio sam:** konkretni koncepti i odluke, uz kratko "zašto"
- **Greške koje sam napravio:** šta je pošlo po zlu i kako sam to riješio

---

## 2026-11-01 (nedjelja, 12:22) - Sedmica 1: Postgres u Dockeru, Flyway, prvi endpoint

- **Naučio sam:**
  - Postgres pokrećem kroz `docker-compose.yml`; podaci žive u named volumeu, pa `docker compose down` briše kontejner, a podaci ostaju. `down -v` briše i volume, tj. sve podatke.
  - Tajne (lozinka baze) idu u `.env` koji je u `.gitignore`; u repo ide samo `.env.example` s lažnim vrijednostima.
  - Spring Boot ne čita `.env` sam, čita varijable okruženja procesa. U `application.properties` koristim `${POSTGRES_DB}` placeholdere.
  - `ddl-auto=validate` + Flyway: šemu mijenjaju samo verzionisane SQL migracije (`V1__create_users.sql`), a Hibernate samo provjerava da se entiteti slažu s bazom. `update` bi tiho mijenjao bazu mimo historije promjena.
  - Primijenjena migracija se ne mijenja (checksum u `flyway_schema_history`); nova promjena ide u `V2__...`.
  - `GENERATED ALWAYS AS IDENTITY`, `UNIQUE` na emailu (baza brani duplikate i kod istovremenih zahtjeva), `TIMESTAMPTZ` za vrijeme; u bazi čuvam `password_hash`, nikad lozinku.
  - DTO kao Java `record` (`HealthResponse`) i `@RestController`/`@GetMapping`; kontroler nema poslovne logike. API ne vraća JPA entitete jer bi procurila interna polja (npr. `password_hash`) i svaka promjena tabele mijenjala bi API.
  - 401 na `/api/health` ne dokazuje da kontroler radi: Spring Security je filter ispred kontrolera i vraća 401 i za putanje koje ne postoje.
  - `@WebMvcTest` podiže samo web sloj (bez baze), za razliku od `@SpringBootTest` koji podiže cijelu aplikaciju. `MockMvc` šalje lažne HTTP zahtjeve, `@WithMockUser` glumi prijavljenog korisnika. Dva testa: prijavljen korisnik dobija 200 i `{"status":"UP"}`, neprijavljen dobija 401.
  - Test s constructor injectionom treba `@Autowired` na konstruktoru, inače JUnit ne zna odakle da uzme `MockMvc`.
  - Git radni tok: mali commitovi s porukama na bosanskom, prije svakog `git status` i `git diff --staged` da provjerim da nema tajni, push na kraju sesije.
- **Greške koje sam napravio:**
  - Mislio sam da 401 znači da endpoint radi. Zapravo samo znači da je aplikacija pokrenuta i da je Security aktivan; da kontroler radi dokazuje tek test.
  - Prvi test je pao s `No ParameterResolver registered for parameter [MockMvc]` jer konstruktor nije imao `@Autowired`.
