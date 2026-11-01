# Ključne tehničke odluke

Za svaku odluku: šta smo izabrali, zašto i koja je alternativa. Dopunjava se uporedo s radom.

## 1. Flyway migracije + `ddl-auto=validate`

- **Odluka:** šemu baze mijenjaju samo verzionisani SQL fajlovi (`V1__...`, `V2__...`).
  Hibernate je na `validate`: pri startu samo provjerava da se entiteti slažu s tabelama.
- **Zašto:** svaka baza (lokalna, test, produkcija) prođe iste korake i završi s istom šemom,
  a historija promjena je u gitu. Primijenjena migracija se ne mijenja (Flyway čuva checksum);
  nova promjena ide u novi fajl.
- **Alternativa:** `ddl-auto=update` – Hibernate sam pogađa promjene, ali nema historije,
  ne briše uklonjene kolone i šema se tiho razilazi između okruženja.

## 2. Tajne samo kroz varijable okruženja

- **Odluka:** lozinka baze i kasnije JWT secret nisu u kodu. `application.properties` koristi
  placeholdere (`${POSTGRES_PASSWORD}`), lokalno se vrijednosti čitaju iz `.env` koji je u
  `.gitignore`. U repou je samo `.env.example` s lažnim vrijednostima.
- **Zašto:** tajna koja jednom dospije u git historiju je kompromitovana, čak i ako se obriše.
- **Alternativa:** profili (`application-local.properties`, takođe van gita) ili tajne
  menadžeri (npr. cloud secret manager) u produkciji.

## 3. DTO-ovi (Java `record`) umjesto JPA entiteta u API-ju

- **Odluka:** API vraća i prima isključivo DTO-ove.
- **Zašto:** entitet `User` sadrži `password_hash`, pa bi ga direktno vraćanje otkrilo; a svaka
  promjena tabele bi mijenjala javni API. `record` je kratak, nepromjenjiv i Jackson ga
  serijalizuje bez dodatnog koda.
- **Alternativa:** obične klase s getterima (više koda), ili mapiranje bibliotekom (MapStruct).

## 4. `spring.jpa.open-in-view=false`

- **Odluka:** JPA sesija se ne drži otvorenom do kraja HTTP zahtjeva.
- **Zašto:** lazy-loading van servisnog sloja bi tiho slao upite iz kontrolera; ovako takav kod
  odmah puca s `LazyInitializationException` i greška se vidi tokom razvoja, ne u produkciji.

## 5. `TIMESTAMPTZ` i `NUMERIC` u bazi

- **Odluka:** vrijeme je `TIMESTAMPTZ`, novac će biti `NUMERIC(12,2)` u bazi i `BigDecimal` u Javi.
- **Zašto:** `TIMESTAMPTZ` čuva trenutak nezavisno od vremenske zone servera. `double` ne može
  tačno predstaviti decimalne iznose (npr. 0.1), pa bi podjela troškova gubila ili dobijala
  cente; `NUMERIC`/`BigDecimal` su tačni.

## 6. Lombok, ali bez `@Data` na entitetima

- **Odluka:** koristimo Lombok (`@Getter`, `@NoArgsConstructor(access = PROTECTED)`) da ne pišemo
  ručno getter-e i prazan konstruktor. Na JPA entitetima ne koristimo `@Data`, `@Setter` ni
  `@EqualsAndHashCode`.
- **Zašto:** manje šablonskog koda, a entitet ostaje nepromjenjiv kroz konstruktor. `@Data` generiše
  `equals`/`hashCode`/`toString` nad svim poljima, što kod entiteta pravi probleme (npr. lazy
  relacije se učitavaju u `toString`, a `hashCode` se mijenja nakon što baza dodijeli `id`).
- **Alternativa:** ručni getteri (više koda, ali nema dodatne biblioteke) ili `record` za DTO-ove,
  gdje Lombok uopšte ne treba.

## 7. Registracija: BCrypt, normalizacija emaila, zaštita od duplikata

- **Odluka:** lozinka se hashira s BCrypt-om (`PasswordEncoder`), email se prije upisa
  `trim()`-uje i pretvara u mala slova, lozinka mora imati 8-72 znaka.
- **Zašto:** BCrypt je sporo, "soljeno" hashiranje namijenjeno lozinkama, pa i ako baza procuri
  lozinke se ne mogu lako povratiti. Gornja granica 72 je zato što BCrypt gleda samo prvih 72 bajta.
  Normalizacija sprječava dva naloga za `Ana@x.com` i `ana@x.com`.
- **Dupli email:** servis prvo provjeri `existsByEmail` (jasna poruka), ali dva istovremena zahtjeva
  mogu oba proći tu provjeru. Zato `UNIQUE` u bazi ostaje zadnja linija odbrane, a servis hvata
  `DataIntegrityViolationException` i vraća isti 409.
- **Alternativa:** hashiranje s Argon2 (noviji, ali traži dodatnu biblioteku), ili samo oslanjanje
  na bazu bez `existsByEmail` (nema lijepe poruke, hvatanje greške je tada jedini put).

## 8. Validacija i jedinstven oblik greške

- **Odluka:** ulaz validira Bean Validation na DTO-ovima (`@Valid`), a `GlobalExceptionHandler`
  sve greške pretvara u `ApiError`.
- **Zašto:** klijent (React) uvijek dobija isti oblik greške i može pokazati poruku uz tačno polje.
  Kontroleri ostaju čisti, bez `try/catch`.
- **Alternativa:** `ProblemDetail` (RFC 9457, ugrađen u Spring), standardizovan format; razmotrit ćemo
  ga ako ApiError postane preuzak.

## 9. JWT preko Spring Securityjevog resource servera, ne vlastiti filter

- **Odluka:** tokene potpisujemo HS256 (zajednički tajni ključ) i provjeravamo ih ugrađenim
  `oauth2ResourceServer().jwt()`. Ključ je samo u `JWT_SECRET` i aplikacija ne starta ako je kraći od 32 znaka.
- **Zašto:** provjera potpisa i isteka je sigurnosno osjetljiva i bolje je koristiti provjeren kod
  nego pisati svoj filter. Jedan backend potpisuje i provjerava, pa simetričan ključ je dovoljan.
- **Šta je u tokenu:** samo id korisnika (`sub`) i vremena. Ne email (može se promijeniti) ni hash.
- **Alternativa:** RS256 (par ključeva) ima smisla kad tokene provjerava više servisa; ili
  biblioteka `jjwt` s vlastitim filterom (više koda, više mjesta za grešku).
- **Poznato ograničenje:** nema refresh tokena ni odjave na serveru; token važi 60 minuta.

## 10. Ista poruka za pogrešan email i pogrešnu lozinku

- **Odluka:** prijava vraća 401 "Pogrešan email ili lozinka" u oba slučaja.
- **Zašto:** drugačije poruke bi otkrile napadaču koji emailovi imaju nalog.
- **Poznato ograničenje:** razlika u vremenu odgovora (BCrypt se računa samo ako korisnik postoji) i
  nema ograničenja broja pokušaja; moguće poboljšanje kasnije.

## 11. Testiranje web sloja s `@WebMvcTest`

- **Odluka:** kontrolere testiramo s `@WebMvcTest` + `MockMvc`, a Security ponašanje s `@WithMockUser`.
- **Zašto:** podiže se samo web sloj (bez baze), pa su testovi brzi. Testiramo i uspješan slučaj
  i odbijanje neprijavljenog korisnika (401).
- **Alternativa:** `@SpringBootTest` podiže cijelu aplikaciju – bliže produkciji, ali sporije;
  koristit ćemo ga za integracione testove s bazom.

## 12. Grupe: složeni ključ, 404 umjesto 403 i kod za pridruživanje

- **Složeni primarni ključ** `(group_id, user_id)` u `group_members`: isti korisnik ne može ući u
  istu grupu dvaput, a to garantuje baza, ne samo kod. Mapiran je s `@EmbeddedId` i `@MapsId`.
  Alternativa je surogat `id` kolona uz posebno `UNIQUE(group_id, user_id)`.
- **Ko nije član, dobija 404, ne 403.** Ista poruka vrijedi kad grupa ne postoji i kad korisnik nije
  član, pa se ne može saznati koji ids grupa postoje. Provjera članstva ide **prije** učitavanja grupe.
- **Kod za pridruživanje:** 8 znakova iz abecede bez znakova koji se brkaju (0/O, 1/I/L), generisan s
  `SecureRandom` (kod je jedina "lozinka" za ulazak u grupu). Prostor je 31^8 (oko 8,5 · 10^11).
  `UNIQUE` u bazi je zadnja linija odbrane od kolizije. Poznato ograničenje: nema ograničenja broja
  pogrešnih pokušaja, pa bi se kod teoretski mogao pogađati; kasnije se može dodati rate limit.
- **Lazy relacije i `join fetch`:** relacije su `LAZY`, a liste se učitavaju upitima s `join fetch`
  da se izbjegne N+1 problem (1 + N upita za listu od N redova).
- **`@Transactional`:** kreiranje grupe upisuje grupu i prvog člana (vlasnika) u jednoj transakciji,
  da ne ostane grupa bez vlasnika. Čitanje je `readOnly = true`.
- **Autorizacija:** id korisnika dolazi isključivo iz tokena, nikad iz tijela ili putanje zahtjeva.
- **Članovi vide samo ime za prikaz**, ne email drugih članova.
