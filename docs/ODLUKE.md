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
  feninge; `NUMERIC`/`BigDecimal` su tačni.

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

## 13. Novac: računanje u feninzima, bez tihog zaokruživanja

- **Odluka:** iznosi su `BigDecimal` (nikad `double`), a podjela se računa u **feninzima kao cijelim brojevima**
  (`ExpenseSplitter`). Ostatak cijelog dijeljenja dobijaju prvi učesnici po 1 fening
  (100.00 na troje = 33.34 + 33.33 + 33.33).
- **Valuta:** konvertibilna marka (KM), 1 KM = 100 feninga. Najmanja jedinica je fening, pa su
  iznosi uvijek s najviše 2 decimale.
- **Zašto:** u cijelim brojevima nema grešaka zaokruživanja, pa je zbir dijelova **uvijek tačno** jednak
  ukupnom iznosu i nijedan fening se ne gubi ni stvara. `double` ne može tačno predstaviti npr. 0.1.
- **Ulaz se ne zaokružuje:** iznos s više od 2 decimale se odbija s greškom, umjesto da se tiho promijeni.
  Najveći iznos odgovara koloni `NUMERIC(12,2)`.
- **Poznato ograničenje:** ostatak uvijek ide prvima u listi, pa kroz mnogo troškova isti ljudi mogu
  dobiti feninga više. Za iznose koji su u feninzima to je zanemarivo; ako zatreba, ostatak se može rotirati.
- **Alternativa:** `BigDecimal.divide` s `RoundingMode`, uz ručno "ispravljanje" zadnjeg dijela. Radi,
  ali je lakše pogriješiti; računanje u feninzima je jednostavnije za dokazati testom.

## 14. Troškovi: autor, pravila u bazi i paginacija

- **Autor i platilac su različiti pojmovi.** Platilac (`paid_by`) je ko je platio, autor (`created_by`) je ko je unio
  trošak. Samo autor mijenja i briše; ostali članovi dobijaju **403** (za razliku od stranaca koji dobijaju 404),
  jer član grupe trošak ionako vidi.
- **Pravila integriteta su u bazi:** `CHECK (amount > 0)`, `UNIQUE (expense_id, user_id)` i složeni strani ključevi
  `(group_id, paid_by) -> group_members`. Servis ih provjerava prvi radi jasnih poruka, ali ih baza brani i od
  greške u kodu. Invarijanta `SUM(share_amount) = amount` ne može u običan `CHECK` (više redova), pa je čuva servis
  preko `ExpenseSplitter`-a i testovi.
- **Izmjena = zamjena dijelova.** `clearShares()` + `saveAndFlush` brišu stare dijelove prije upisa novih. Bez `flush`
  Hibernate radi INSERT prije DELETE i `UNIQUE (expense_id, user_id)` bi pukao kad ostaju isti učesnici.
- **Paginacija bez N+1 i bez paginacije u memoriji:** upit za stranicu fetch-uje samo veze "prema jednom"
  (platilac, autor), a dijelovi svih troškova sa stranice dolaze **jednim dodatnim upitom** (`in :ids`).
  Fetch liste u istom upitu uz `Pageable` natjerao bi Hibernate da učita sve i paginira u memoriji.
- **Filteri bez `null` parametara:** nedostajući datum zamjenjuju krajnje vrijednosti (1900-01-01 i 9999-12-31), a
  kategorija se šalje kao lista (`in :categories`, sve kategorije kad filter nije zadan). Tako nema `null`
  parametara čiji tip PostgreSQL ne može odrediti.
- **Vlastiti `PageResponse`:** oblik JSON-a stranice određujemo mi, ne Spring Data. `size` je ograničen na 100.
- **Dio od 0.00 se ne upisuje** (npr. 0.01 KM na troje), jer je u bazi `share_amount > 0`.

## 15. Balansi: zbirni upiti i tačan algoritam poravnanja

- **Balans se ne čuva, računa se.** Nema kolone `balance` koja bi se mogla razići s troškovima. Svaki put se računa
  iz troškova, dijelova i uplata, pa je uvijek tačan i kad se trošak izmijeni ili obriše.
- **Zbirni upiti umjesto učitavanja redova:** `SUM + GROUP BY` u bazi vraća jedan red po članu. Grupa s hiljadu
  troškova ne znači hiljadu objekata u memoriji.
- **Minimalan broj uplata je tačan, ne približan.** Greedy često daje isti rezultat, ali ne uvijek: postoje slučajevi
  gdje daje 4 uplate a optimum je 3 (pokriveno testom). Zato `SettlementPlanner` koristi DP nad podskupovima.
  Test poredi rezultat s nezavisnim algoritmom na 400 nasumičnih primjera.
- **Poznato ograničenje:** DP je eksponencijalan (2^n), pa je tačan samo do 20 članova s nenultim balansom. Za veće
  grupe pada na greedy. Za stan ili društvo prijatelja to je više nego dovoljno.
- **Uplata uvijek od prijavljenog korisnika.** Da može upisati uplatu u tuđe ime, mogao bi nekome "izbrisati" dug.
- **Poznato ograničenje:** uplata može biti veća od duga (tada primalac postaje dužnik) i zasad nema liste ni
  brisanja uplata.
- **Alternativa:** čuvati balanse u zasebnoj tabeli i ažurirati ih pri svakoj promjeni. Brže čitanje, ali lako
  dobije pogrešnu vrijednost ako se jedna promjena zaboravi.

## 16. Frontend: običan CSS, vlastiti dizajn i tanak API sloj

- **Bez UI biblioteke.** Sav izgled je običan CSS s varijablama (`index.css`: boje, fontovi, sjene, dugmad, forme). Izgled je moderan i prozračan: tanke ivice, meke slojevite sjene, naslovi u Figtree sans fontu s Fraunces serifom samo kao akcentom. Prva verzija je imala debele ivice i tvrde sjene; sve ivice i sjene su sad tokeni (`--bc`, `--shadow-card`...), pa se izgled mijenja na jednom mjestu.
  Zašto: kod se može objasniti liniju po liniju, a izgled nije "još jedan Tailwind/Material projekat".
  Cijena: više posla i ručna briga o pristupačnosti.
- **Fontovi preko `@fontsource`** (Fraunces, Figtree, DM Mono): rade lokalno, bez poziva prema Google Fonts.
- **Novac se na klijentu formatira ručno** (`1.234,50 KM`), a ne preko `Intl`. Prva verzija je koristila `Intl`
  s bosanskim locale-om, a u jednom browseru je pao na engleski format (33.34 umjesto 33,34). Izgled iznosa ne
  smije zavisiti od podataka u browseru. Isto važi za nazive mjeseci.
- **Podjela troška se uživo prikazuje u formi** istim algoritmom kao backend (`splitEqual`, u feninzima). To je samo
  pregled; konačan račun uvijek radi server.
- **JWT u `sessionStorage`, ne u `localStorage`.** Prva verzija je koristila `localStorage`, a on je zajednički za sve
  tabove istog sajta: prijava drugog korisnika u drugom tabu prepiše token i poslije osvježavanja svi tabovi postanu
  zadnji korisnik. `sessionStorage` je odvojen po tabu, a i dalje preživi osvježavanje. Cijena: novi tab traži novu
  prijavu, i zatvaranje taba odjavljuje. Obje opcije su čitljive skripti (XSS); alternativa je httpOnly cookie, ali traži
  CSRF zaštitu i promjene na backendu. Za ovaj projekat biram jednostavnost, uz kratak istek tokena (60 min).
- **`useAsync` umjesto biblioteke za podatke** (npr. TanStack Query): jedan mali hook pokriva loading, grešku i
  ponovno učitavanje. Nema keširanja između stranica, što ovdje ne smeta.
- **Nativni `<dialog>` za modale:** browser sam daje focus trap i Esc.
- **Uplatu uvijek bilježi platilac.** UI nudi dugme "Platio/la sam" samo na uplatama koje je tebi predložio algoritam,
  jer backend ne dozvoljava upis uplate u tuđe ime.
