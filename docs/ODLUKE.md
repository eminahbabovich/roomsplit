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

## 6. Testiranje web sloja s `@WebMvcTest`

- **Odluka:** kontrolere testiramo s `@WebMvcTest` + `MockMvc`, a Security ponašanje s `@WithMockUser`.
- **Zašto:** podiže se samo web sloj (bez baze), pa su testovi brzi. Testiramo i uspješan slučaj
  i odbijanje neprijavljenog korisnika (401).
- **Alternativa:** `@SpringBootTest` podiže cijelu aplikaciju – bliže produkciji, ali sporije;
  koristit ćemo ga za integracione testove s bazom.
