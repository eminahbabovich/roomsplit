# Dnevnik učenja

Hronološki zapis šta sam naučio tokom izrade portfolija (RoomSplit, pa SlotBook).
Svaki unos ima datum, vrijeme i temu sesije. Dopisuje se na kraju svake sesije.

Format unosa:

## YYYY-MM-DD (dan, HH:MM) - Faza N: tema
- **Naučio sam:** konkretni koncepti i odluke, uz kratko "zašto"
- **Greške koje sam napravio:** šta je pošlo po zlu i kako sam to riješio

---

## 2026-11-01 (nedjelja, 12:22) - Faza 1 (Temelji): Postgres u Dockeru, Flyway, prvi endpoint

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
  - Cijeli `mvnw test` uključuje i `@SpringBootTest` (`RoomsplitApplicationTests`) koji podiže cijelu aplikaciju s pravom bazom i Flywayom; prolazak dokazuje da datasource, migracije i `ddl-auto=validate` rade zajedno, što `@WebMvcTest` ne može dokazati. Mana: test ovisi o lokalnoj bazi i env varijablama (kasnije Testcontainers).
  - Git radni tok: jedan commit je jedna logična cjelina, poruke na bosanskom, prije svakog `git status` i `git diff --staged` da provjerim da nema tajni, a push radim ja na kraju sesije.
- **Greške koje sam napravio:**
  - Mislio sam da 401 znači da endpoint radi. Zapravo samo znači da je aplikacija pokrenuta i da je Security aktivan; da kontroler radi dokazuje tek test.
  - Prvi test je pao s `No ParameterResolver registered for parameter [MockMvc]` jer konstruktor nije imao `@Autowired`.

---

## 2026-11-01 (nedjelja, 12:56) - Faza 2 (Autentikacija): User entitet i Lombok

- **Naučio sam:**
  - `@Entity` i `@Table(name = "users")` vežu Java klasu za tabelu. Naziv tabele pišem eksplicitno jer bi Hibernate inače tražio `user`, a to je u Postgresu rezervisana riječ.
  - `@Id` + `@GeneratedValue(strategy = IDENTITY)` znači da id generiše baza. `@Column(name = "password_hash")` mi treba jer se camelCase u Javi razlikuje od snake_case u bazi.
  - JPA traži prazan konstruktor jer entitete pravi refleksijom. Držim ga `protected` da ga ostali ne zovu.
  - Entitet nema setter-e, vrijednosti dobija kroz konstruktor. `createdAt` postavljam u konstruktoru (`Instant.now()`), jer Hibernate pri `INSERT`-u šalje sva polja pa bi `null` pao na `NOT NULL`.
  - `JpaRepository<User, Long>` daje `save`, `findById` itd. bez koda, a `findByEmail` i `existsByEmail` Spring sam pretvori u SQL iz imena metode (ime mora pratiti polje entiteta).
  - Lombok ide kao `optional` zavisnost plus `annotationProcessorPaths` u `maven-compiler-plugin`, i isključen je iz finalnog jara. Na entitetima koristim samo `@Getter` i `@NoArgsConstructor`, ne `@Data`, jer `equals`/`hashCode`/`toString` nad svim poljima prave probleme kod JPA entiteta.
  - Da je Lombok zaista radio provjerio sam sa `javap`, jer kompajler ne bi javio grešku dok god niko ne poziva getter-e.
- **Greške koje sam napravio:** u ovom koraku nijedna veća. Jedino sam za Lombok morao paziti na konfiguraciju u `pom.xml`, jer bez `annotationProcessorPaths` kod može da se kompajlira, a getteri ne postoje.

---

## 2026-11-01 (nedjelja, 13:01) - Faza 2 (Autentikacija): registracija korisnika

- **Naučio sam:**
  - Tok registracije: `AuthController` (prima zahtjev) → `AuthService` (posao) → `UserRepository` (upis). Kontroler nema logike, vraća `UserResponse` bez `password_hash`.
  - `RegisterRequest` je `record` s Bean Validation anotacijama (`@NotBlank`, `@Email`, `@Size`), a `@Valid` na parametru kontrolera ih uključuje. Bez `@Valid` anotacije bi se ignorisale. Lozinka ima gornju granicu 72 jer BCrypt gleda samo prvih 72 bajta.
  - Lozinku hashiram s `BCryptPasswordEncoder` (bean `PasswordEncoder`). Hash je "soljen" i spor namjerno, pa se lozinka ne može lako povratiti ni ako baza procuri.
  - Email normalizujem (`trim` + mala slova), da `Ana@x.com` i `ana@x.com` ne budu dva naloga.
  - Duplikat emaila: `existsByEmail` daje lijepu poruku, ali dva istovremena zahtjeva mogu oba proći tu provjeru. Zato je `UNIQUE` u bazi zadnja linija odbrane, a servis hvata `DataIntegrityViolationException` i vraća isti 409.
  - `@Transactional` na servisu znači da je cijela metoda jedna transakcija; ako nešto pukne, ništa se ne upisuje.
  - `@RestControllerAdvice` (`GlobalExceptionHandler`) na jednom mjestu pretvara izuzetke u `ApiError`: 400 s `fieldErrors` po polju, 409 za zauzet email.
  - `SecurityConfig`: stateless sesije, CSRF isključen (nema cookie-ja), `/api/auth/**` je javan, ostalo traži prijavu. Bez `authenticationEntryPoint` neprijavljen korisnik dobija 403 umjesto 401.
  - Tri vrste testova za istu funkcionalnost: unit (Mockito, bez Springa) za servis, `@WebMvcTest` + `@MockitoBean` za HTTP sloj, i `@SpringBootTest` s pravom bazom koji se vraća u prvobitno stanje (`@Transactional`).
  - Lombok `@RequiredArgsConstructor` pravi konstruktor za `final` polja, pa dobijam constructor injection bez ručnog koda.
- **Greške koje sam napravio:** nije bilo grešaka u ovom koraku. Integracioni test s bazom još nisam pokrenuo (traži učitan `.env`).

---

## 2026-11-01 (nedjelja, 13:10) - Faza 2 (Autentikacija): prijava i JWT

- **Naučio sam:**
  - JWT je potpisan zapis (header.payload.potpis). Server ga potpiše tajnim ključem pri prijavi, a na svakom zahtjevu samo provjeri potpis i istek, pa ne treba sesija (`STATELESS`). Payload nije šifrovan, samo potpisan, zato u njega ne stavljam ništa osjetljivo.
  - U tokenu držim samo `sub` (id korisnika) i vremena izdavanja/isteka. Id jer se email može promijeniti.
  - Tokene izdajem s `JwtEncoder`, a provjeru radi Spring Security preko `oauth2ResourceServer().jwt()` i `JwtDecoder`, bez vlastitog filtera. Isti HS256 ključ za oba. U Spring Boot 4 starter se zove `spring-boot-starter-security-oauth2-resource-server`.
  - Ključ (`JWT_SECRET`) mora imati najmanje 32 znaka; ako je kraći, `JwtConfig` baci izuzetak pri startu, pa aplikacija ne radi sa slabim ključem. Nikad ga ne stavljam u kod, samo u `.env` i lažnu vrijednost u `.env.example`.
  - Pri prijavi vraćam istu poruku "Pogrešan email ili lozinka" za pogrešan email i pogrešnu lozinku, da napadač ne može saznati koji emailovi postoje.
  - `@AuthenticationPrincipal Jwt` u kontroleru daje token prijavljenog korisnika; id uzimam iz tokena, ne iz zahtjeva, pa niko ne može tražiti tuđe podatke (`/api/users/me`).
  - Bez `authenticationEntryPoint` Security vraća 403 za neprijavljene; s `HttpStatusEntryPoint(UNAUTHORIZED)` vraća 401, što je ispravno.
  - CORS: browser blokira zahtjeve s drugog origina (React na 5173, API na 8080) dok server ne dozvoli taj origin. Dozvoljene origine čitam iz `CORS_ALLOWED_ORIGINS`.
  - Testovi: istekao token testiram tako da ručno napravim token izdat prije 2 sata (provjera isteka ima toleranciju sata od 60 sekundi); token potpisan tuđim ključem se odbija; `jwt()` post-procesor u `MockMvc` glumi prijavljenog korisnika.
  - `application-test.properties` s izmišljenim ključem i `@ActiveProfiles("test")` omogućavaju da testovi ne ovise o mom `.env` za JWT.
- **Greške koje sam napravio:** test isteka tokena je prvo pao (`expiresAt must be after issuedAt`), jer encoder ne dozvoljava token koji ističe prije izdavanja. Riješio sam ga ručnim kreiranjem tokena iz prošlosti. Također sam u `.env` prvo upisao tekst komande za generisanje ključa umjesto njenog rezultata. Aplikacija bi tako startovala (niz je dovoljno dug), ali bi ključ bio javno poznat iz `.env.example`, pa bi se mogli falsifikovati tokeni. Pazim da u `.env` ide ispisana vrijednost, a ne komanda. Cijeli skup od 26 testova, uključujući one s pravom bazom, prolazi.

---

## 2026-11-01 (nedjelja, 13:26) - Faza 3 (Grupe): model, servis i API za grupe

- **Naučio sam:**
  - `@ManyToOne` je veza "mnogo prema jednom" (mnogo grupa, jedan kreator). `optional = false` odgovara `NOT NULL`, a `@JoinColumn` kaže koja je kolona strani ključ; bez nje Hibernate pogađa ime kolone i `validate` pada.
  - `FetchType.LAZY` učitava povezani objekat tek kad ga pozovem. `EAGER` bi uz svaku grupu odmah povlačio korisnika i pravio višak upita.
  - Lista od N redova s lazy relacijom pravi 1 + N upita (N+1 problem). Rješavam ga s `join fetch` u JPQL upitu.
  - Složeni ključ: `@EmbeddedId` + `@Embeddable` klasa koja mora imati `equals`/`hashCode` i biti `Serializable`. `@MapsId` kaže da je kolona istovremeno dio ključa i strani ključ, da je Hibernate ne mapira dvaput.
  - `@Enumerated(EnumType.STRING)` čuva "OWNER" umjesto rednog broja, pa dodavanje nove uloge usred enuma ne kvari podatke.
  - Indeks na `user_id` treba jer primarni ključ `(group_id, user_id)` ubrzava samo upite po `group_id`, a "moje grupe" traži po `user_id`.
  - `@Transactional` stavlja kreiranje grupe i upis vlasnika u jednu transakciju: ili uspije oboje ili ništa. Za čitanje koristim `readOnly = true`.
  - Autorizacija po grupi: prvo provjera članstva, pa tek onda učitavanje grupe. Ko nije član dobija 404 kao da grupa ne postoji, da se ne otkriju ids grupa. Id korisnika uzimam samo iz tokena.
  - Kod za pridruživanje generišem sa `SecureRandom` iz abecede bez znakova koji se brkaju, a `UNIQUE` u bazi hvata koliziju. Dva istovremena pridruživanja istog korisnika hvata primarni ključ, pa vraćam isti 409.
  - Mockito: `thenAnswer` treba kad mock mora vratiti ono što je dobio kao argument (npr. `save`).
- **Greške koje sam napravio:** u ovom koraku nijedna. `GroupFlowIntegrationTest` (s pravom bazom) još nisam pokrenuo.

---

## 2026-11-01 (nedjelja, 13:30) - Faza 4 (Troškovi i balansi): tačna podjela novca

- **Naučio sam:**
  - Novac nikad ne držim u `double` (0.1 + 0.2 nije 0.3). Koristim `BigDecimal`, a za podjelu računam u feninzima kao cijelim brojevima, pa nema zaokruživanja i zbir dijelova je uvijek tačno jednak ukupnom iznosu.
  - Jednaka podjela: `feninge / broj ljudi` daje osnovni dio, a ostatak (`%`) se razdijeli po 1 fening prvima. 100.00 na troje je 33.34 + 33.33 + 33.33.
  - Iznos s više od 2 decimale odbijam, a ne zaokružujem tiho, jer bi korisnik platio drugačije nego što je unio. `stripTrailingZeros()` mi treba da 10.0000 ostane ispravan unos.
  - `BigDecimal.valueOf(feninzi, 2)` pravi iznos s tačno 2 decimale iz feninga, a `movePointRight(2).longValueExact()` ide nazad i baca izuzetak ako broj ne stane.
  - Zbog `equals` kod `BigDecimal` (2.5 i 2.50 nisu jednaki) u testovima koristim `isEqualByComparingTo` kad poredim vrijednost, a `isEqualTo` samo kad mi je bitna i skala.
  - Test koji prolazi kroz hiljade kombinacija iznosa i broja ljudi (zbir tačan, razlika među dijelovima najviše 1 fening) dokazuje invarijantu bolje od nekoliko ručnih primjera.
- **Greške koje sam napravio:** u ovom koraku nijedna.

---

## 2026-11-01 (nedjelja, 13:42) - Faza 4 (Troškovi i balansi): modeli i API za troškove

- **Naučio sam:**
  - `@OneToMany(mappedBy = "expense")` je "pogled" na vezu čiji je strani ključ u drugoj tabeli; vlasnik veze je polje `expense` u `ExpenseShare`. `cascade = ALL` prenosi snimanje i brisanje na dijelove, a `orphanRemoval` briše dio koji izbacim iz liste.
  - Dvosmjernu vezu održavam kroz metodu `addShare` (konstruktor dijela je package-private), a listu vraćam kao `unmodifiableList`, da je niko ne mijenja mimo te metode.
  - Pravila držim i u bazi: `CHECK (amount > 0)`, `UNIQUE` i složeni strani ključ `(group_id, paid_by) -> group_members` garantuju da platilac pripada baš toj grupi, čak i ako kod pogriješi. Zbir dijelova ne može u `CHECK`, pa ga čuva servis i testovi.
  - Pri izmjeni troška moram `flush` poslije `clearShares()`, jer Hibernate radi INSERT prije DELETE pa bi `UNIQUE (expense_id, user_id)` puknuo kad ostaju isti učesnici.
  - Paginacija: `join fetch` kolekcije uz `Pageable` natjera Hibernate da paginira u memoriji. Zato stranicu učitavam s fetch-om samo "prema jednom" veza, a dijelove svih troškova sa stranice jednim dodatnim upitom (`in :ids`).
  - `null` parametri u JPQL upitu zadaju problem PostgreSQL-u (ne može odrediti tip), pa umjesto njih koristim krajnje datume i listu kategorija.
  - Spring Data `Page` nema stabilan JSON, pa vraćam vlastiti `PageResponse`.
  - 403 za člana grupe koji mijenja tuđi trošak, 404 za stranca: član trošak ionako vidi, pa 404 ne bi imao smisla.
  - `HttpMessageNotReadableException` i `MethodArgumentTypeMismatchException` hvatam u `GlobalExceptionHandler`, da loš JSON, nepoznat enum ili loš datum daju 400 s porukom.
- **Greške koje sam napravio:** integracioni test izmjene troška je pao s `duplicate key ... expense_shares_expense_id_user_id_key`. Razlog nije bio u servisu nego u testu: `@Transactional` test stavlja sve zahtjeve u jedan persistence context, pa Hibernate nije obrisao stare dijelove prije upisa novih. U stvarnosti je svaki zahtjev zasebna transakcija. Test sada između zahtjeva radi `flush` i `clear`, što to oponaša (svih 8 testova prolazi). Pouka: integracioni test mora vjerno oponašati granice transakcija.

---

## 2026-11-01 (nedjelja, 13:54) - Faza 4 (Troškovi i balansi): balansi, poravnanje i uplate

- **Naučio sam:**
  - Balans ne čuvam u bazi nego ga računam: `platio - njegov dio + isplatio - primio`. Tako se ne može razići s troškovima kad se neki izmijeni ili obriše. Zbir balansa u grupi je uvijek 0.
  - Umjesto učitavanja svih troškova koristim `SUM ... GROUP BY` upite koji vraćaju jedan red po članu. U JPQL-u za to služi constructor expression (`select new ...UserTotal(...)`).
  - Greedy poravnanje (najveći dužnik plaća najvećem povjeriocu) nije uvijek najbolje. Postoje slučajevi gdje daje 4 uplate a dovoljno su 3. Tačno rješenje: nađem najviše podskupova čiji je zbir balansa 0, pa je broj uplata `n - broj podskupova`. To radim DP-om nad bitmaskama, što je 2^n, pa vrijedi do 20 članova, a iznad toga pada na greedy.
  - Algoritam provjeravam poređenjem s nezavisnim algoritmom na 400 nasumičnih primjera, i posebnim testom gdje greedy griješi.
  - `GroupAccess` je jedno mjesto za provjeru članstva, pa ne kopiram istu provjeru u svaki servis. Nečlan dobija 404, isto kao da grupa ne postoji.
  - Uplatu uvijek bilježi prijavljeni korisnik (platilac nije u zahtjevu). Da može upisati uplatu u tuđe ime, mogao bi nekome "izbrisati" dug.
  - `@WebMvcTest` može testirati dva kontrolera odjednom: `@WebMvcTest({A.class, B.class})`.
  - U integracionom testu opet koristim `flush` + `clear` između zahtjeva, da svaki zahtjev bude kao zasebna transakcija.
- **Greške koje sam napravio:** u ovom koraku nijedna. Integracioni test `BalanceFlowIntegrationTest` (s pravom bazom) još nisam pokrenuo, pa balanse i uplate ne smijem označiti kao gotove dok ne prođe.

---

## 2026-11-01 (nedjelja, 14:02) - Faza 4 završena: provjera s bazom

- **Naučio sam:**
  - Svih 147 testova prolazi s pravom bazom, uključujući integracioni test balansa i uplata. Tek tada faza 4 ide na gotovo.
  - `.env` je u korijenu projekta, a ne u `backend/`. Varijable učitavam iz korijena, pa tek onda idem u `backend` i pokrećem `.\mvnw.cmd test`.
- **Greške koje sam napravio:** komande za učitavanje `.env` pokrenuo sam iz foldera `backend`, pa su `cd backend` i `Get-Content .env` pali s "Cannot find path". Test je krenuo bez varijabli okruženja. Pouka: prije učitavanja provjerim u kojem sam folderu.

---

## 2026-11-01 (nedjelja, 14:25) - Faza 5: React frontend

- **Naučio sam:**
  - Frontend ima jedan sloj za pozive prema API-ju (`api/client.js`). Komponente ne zovu `fetch` same, pa token i greške rješavam na jednom mjestu. Greška se pretvara u `ApiError` s `fieldErrors`, i forma pokaže poruku ispod tačno onog polja koje je backend odbio.
  - `AuthProvider` čuva ko je prijavljen, a `RequireAuth` šalje neprijavljenog na login i vraća ga poslije prijave tamo gdje je bio. Odgovor 401 odjavljuje korisnika.
  - Vite proxy prosljeđuje `/api` na Spring (port 8080), pa u razvoju nema CORS problema.
  - Hook `useAsync` pamti rezultat uz ključ zahtjeva. Dok se novi podaci učitavaju, stari ostaju na ekranu, pa lista ne trepće pri promjeni stranice. Ovako je i lint-čist (nema `setState` direktno u effectu).
  - Nativni `<dialog>` daje focus trap i Esc besplatno. Sadržaj modala se montira tek kad je otvoren, pa se forma svaki put resetuje.
  - Ista podjela novca u feninzima kao na backendu, da pregled u formi tačno odgovara onom što server izračuna.
  - ESLint pravilo `react-refresh/only-export-components`: fajl s komponentom smije izvoziti samo komponente, pa podatke (kategorije) držim u `lib/`.
  - CSS: `@starting-style` za animaciju otvaranja modala, `clip-path` za "štampanje" računa, `:has(:focus-visible)` za fokus na skrivenim radio inputima, `color-mix` za prozirne boje.
- **Greške koje sam napravio:**
  - Novac sam formatirao preko `Intl.NumberFormat('bs-BA')`. U jednom browseru je pao na engleski format i pokazao "33.34" umjesto "33,34". Sad formatiram ručno, jer izgled iznosa ne smije zavisiti od podataka u browseru. Isto i za datume.
  - U tekst sam slučajno ubacio ćirilično slovo "е" u riječ "duguješ". Uhvatio sam ga pretragom po Unicode opsegu. Pouka: provjeriti tekst pretragom, jer se golim okom ne vidi.
  - Fiksirao sam `opsz` osu fonta na veliku vrijednost za sve naslove, pa su riječi u malim naslovima izgledale zbijeno. Sad font sam bira optičku veličinu.
  - Pečat na računu u hero sekciji prekrivao je iznos, a drugi račun je bio gotovo skriven iza prvog. Našao sam to tek na screenshotu, ne u kodu.
  - Cijeli UI sam provjerio samo s lažnim API-jem i izmišljenim podacima. S pravim backendom ga tek treba proći.

---

## 2026-11-01 (nedjelja, 14:54) - Faza 5 završena: provjera s pravim backendom

- **Naučio sam:**
  - `localStorage` je zajednički za sve tabove istog sajta, pa prijava drugog korisnika u drugom tabu prepiše token prvog. Poslije osvježavanja svi tabovi postanu zadnji prijavljeni korisnik. `sessionStorage` je odvojen po tabu i opet preživi osvježavanje, pa sam prešao na njega. Cijena je da novi tab traži novu prijavu.
  - Pravi backend je prošao sve scenarije koje sam prije vidio samo na lažnom API-ju: jednaka podjela, ostatak od feninga, tačni iznosi, balansi, najmanji broj uplata, uplata do pečata KVIT.
- **Greške koje sam napravio:** token sam u prvoj verziji čuvao u `localStorage`, pa testiranje s tri korisnika u istom browseru nije radilo kako treba. Tu grešku sam našao tek kad sam probao s više korisnika odjednom, a ne dok sam pisao kod.
