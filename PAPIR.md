# Papir — aplikacija

Ovaj dokument opisuje **aplikaciju** Papir za Android: čemu služi, što nikad ne radi i kako se njome upravlja. Format datoteka opisan je zasebno, u [STANDARD.md](STANDARD.md); aplikacija ga poštuje kao i svaki drugi program. Kako je aplikacija izgrađena opisuje [ARHITEKTURA.md](ARHITEKTURA.md).

Aplikacija se smije mijenjati brzo. Ustav ne. Ukratko, sve je u [MANIFEST.md](MANIFEST.md).

---

## 1. Ustav

Ustav je iznad svakog zahtjeva za funkcijom. Ako se funkcija ne može napraviti bez kršenja ustava, ne radi se.

1. **Čovjek je gospodar svog teksta, aplikacija je sluga.**
2. **Aplikacija nikad ne tumači ono što pišeš i ne mijenja datoteku bez tvoje namjere.** Ništa se ne ispravlja, ne izračunava, ne prepoznaje i ne nudi dok tipkaš. Ako Ivica kaže `2+2=5`, papir to pamti točno tako.
3. **Prikazivati smije, prepravljati ne.** Aplikacija čita datoteku kao preglednik HTML: prikazuje i oživljava, a datoteku dira samo na izričit zahtjev. Što ne zna, prikazuje kao običan tekst.
4. **Funkcije nastaju samo iz izbornika.** Jedan dodir otvara izbornik, drugi umeće funkciju. Nema okidača u tekstu.
5. **Datoteka je istina, prikaz je tumačenje.** Sve što aplikacija prikazuje proizlazi iz datoteke, nikad iz nečeg skrivenog sa strane.
6. **Ime je samo slova.** Što korisnik upiše kao ime datoteke, uključujući bilo koji nastavak, to i dobije, slovo za slovo.
7. **Sučelje je slobodno.** Izgled aplikacije smije se mijenjati, eksperimentirati i griješiti. Format ne.
8. **Test od 20 sekundi.** Otvoriš, napišeš popis namirnica, zatvoriš. Nijedna funkcija ne smije usporiti taj put: dodir ikone, i tipkovnica i kursor su odmah na papiru.
9. **Ništa se ne gubi, ništa se ne skriva.** Svaki papir je prava datoteka u mapi koju je čovjek izabrao; aplikacija ne čuva skrivene kopije teksta ni povijest verzija. Dok je papir otvoren, svaka izmjena se može poništiti. Aplikacija nikad ne prepisuje datoteku koja je promijenjena izvana: kad dođe do sukoba, odlučuje čovjek, a druga verzija postaje vidljiva datoteka u mapi.
10. **Bez interneta.** Aplikacija ne traži pristup mreži i ne šalje nikamo ništa. Sinkronizaciju, ako je želiš, obavlja alat koji sam izabereš, jer su papiri obične datoteke u mapi.

---

## 2. Upravljanje

- **Svaka kontrola mijenja ili datoteku ili samo prikaz, i to se mora znati.** B, I, veličina, tablica, kvačica, mapa, poveznica, slika, kutija i datum mijenjaju datoteku. Pomicanje papira i traženje ne mijenjaju ništa.
- **Papir se ne prelama**, kao u Notepadu: dugačak redak ide udesno, a papir se pomiče i vodoravno.
- **Obični retci su na ekranu svi iste visine**, kao u Notepadu. Naslov je na ekranu jedan redak svoje veličine (L oko 1,3, XL oko 1,7 običnog), iako u datoteci ima i crtu ispod sebe. Tablica stoji na mreži Notepada (i crte su retci, i zidovi stupci). Slike i kutije ravnaju se po retku teksta uz koji stoje; uz sam naslov njihov položaj po visini je približan.
- **Napredno uređivanje je u Papiru.** Notepad otvara i piše. Ako u Notepadu netko pokvari okvir ili oznaku, Papir pokaže tekst, točno kako izgleda u Notepadu.
- **Nema skrivenih načina rada.** Što je na ekranu, to je stanje; nema „uključenog" načina koji mijenja što tipke rade bez da se vidi.
- **Ništa slučajno.** Nijedna gesta, dvostruki dodir ni duži pritisak ne umeće funkciju. Funkcija je uvijek dva namjerna dodira: **+**, pa funkcija.
- **Sve se može poništiti.** Svaka izmjena, uključujući funkcije i radnje s tablicama, jedan je korak za ↶.
- **Pod palcem.** Kontrole su u traci iznad tipkovnice, gdje palac već jest.
- **Datoteka ne pamti kako je nastala.** Aplikacija ne upisuje u datoteku ništa o sebi, kursoru, fontu ni načinu unosa (STANDARD.md, 1., načelo 4).

---

## 3. Papiri i mapa

- **Mapa za papire** bira se jednom, pri prvom pokretanju. Svi papiri su obične datoteke u njoj.
- **Svako pokretanje otvara prazan papir**, s kursorom i tipkovnicom (manifest, 5). Datoteka otvorena iz druge aplikacije otvara tu datoteku. Povratak u Papir dok je još otvoren ostavlja papir na kojem si bio. Prijašnji papiri su u popisu papira, jedan dodir daleko.
- **Novi papir** postaje datoteka `Note.pap` čim dobije prvo slovo. Ako ime postoji, Android sam dodaje broj: `Note (1).pap`, `Note (2).pap`…
- **Nastavak `.pap` je preporuka, ne obveza.** Ime se može promijeniti u bilo kojem trenutku (**Preimenuj…**, **Spremi kao…**) u bilo što, uključujući `.txt`, `.html` ili `.random`; papir ostaje papir zbog ključa, a ne zbog nastavka.
- **Otvori…** otvara bilo koju tekstnu datoteku. Datoteka bez ključa je goli tekst i ostaje goli tekst dok korisnik izričito ne doda funkciju.
- **Sprema se samo**, i samo kad se tekst promijenio.
- **Sukob:** prije svakog zapisa provjerava se je li datoteka promijenjena izvana. Ako jest, ništa se ne prepisuje dok čovjek ne odabere koju verziju zadržati; druga postaje vidljiva datoteka u mapi (`Note (sukob).pap`).

---

## 4. Kopiranje

- Kopiranje iz Papira šalje **dvije verzije** istog odabira: **goli tekst** (slova, bez ijedne oznake) i **HTML** s pravim podebljanim, kurzivom i veličinama. Odabir bez oblikovanja šalje samo goli tekst, jer je to isto.
- Program u koji se lijepi sam bira što zna: e-mail i uređivači teksta dobiju pravo oblikovanje, tražilica i SMS dobiju goli tekst.
- Lijepljenje u Papir zasad uzima samo goli tekst. Lijepljenje s oblikovanjem dolazi kasnije.
- Tko želi oznake formata, kopira ih iz Notepada ili kroz **Spremi kao…**; kopiranje iz aplikacije nikad ne šalje znakove formata kao tekst.

---

## 5. Traži i zamijeni

- Kao Ctrl+F i Ctrl+H u Notepadu, iz izbornika **⋯**: traži, sljedeće i prethodno, velika i mala slova, zamijeni jedno ili sve.
- Traži se tekst kakav se vidi. Tablice i sadržaj zatvorenih mapa zasad se preskaču: što se ne vidi, ne mijenja se.
- Zamjena je izmjena kao svaka druga i poništava se s ↶.

---

## 6. Kvačice, mape i poveznice

- **Kvačica** i **mapa** su na ekranu element sučelja: kvadratić (prazan ili s kvačicom) i strelica, s dodirnom površinom od prsta. U datoteci su znakovi □ ■ ► ▼. Dodir mijenja taj jedan znak u datoteci, pa je stanje zapisano i vidi se i u Notepadu. Zatvorena mapa skriva svoj sadržaj samo na ekranu.
- Mapa se ne može zatvoriti dok slika ili kutija stoji uz njezin rub (dijelom uz sadržaj, dijelom izvan): tekst ispod bi je na ekranu presjekao. Slika koja je cijela uz sadržaj skrije se s njim.
- Uvlaka se ne dodaje sama: razmake na početku retka upisuješ ti, kao na pisaćem stroju.
- **Poveznica** se umeće iz **+**: tekst (odabrani tekst je prijedlog) i cilj, ime papira u mapi ili web adresa. Dok je kursor u poveznici, iznad trake stoji **Otvori**; poveznica se nikad ne otvara slučajnim dodirom. Web adresu otvara preglednik, jer Papir nema internet.

---

## 7. Slike i kutije teksta

- **Slika** iz **+** ulazi u papir, a papir postaje paket `.pzp` (zip s tekstom i slikama). Ime papira se mijenja samo u nastavku, i aplikacija to kaže.
- Slika i kutija dolaze na prvo slobodno mjesto ispod kursora.
- **Na ekranu se slika i tekst nikad ne presijecaju.** Mjeri se ono što se vidi, uključujući veća slova naslova, a ne samo stupci u datoteci.
- **Pomicanje:** dodir na sliku pa **Pomakni**, strelicama do novog mjesta, i **Potvrdi**. Ništa se ne mijenja dok se ne potvrdi. Slika ne može stati na tekst ni na drugu sliku.
- **Veličina** ide istim putem, strelicama, uz isto pravilo. **Preimenuj** mijenja ime i u okviru i u paketu.
- **Tekst koji se tipka nikad ne pomiče sliku:** sljedeće slovo koje naleti na sliku nastavlja odmah desno od nje, kao na papiru po kojem se ne piše preko slike. Riječ se tako može prelomiti na zidu slike. Kad se obrišu slova lijevo od slike, slova desno od nje primaknu se, jer je redak jedan tekst.
- **Tekst se ne može povećati** (L, XL) ako bi na ekranu zauzeo mjesto slike ili kutije. Papir to odbije i kaže zašto.
- **Redak dodan ili obrisan:** sve ispod se pomiče zajedno, i slike i kutije. Isto vrijedi kad redak iznad postane ili prestane biti naslov. Slika koja počinje u retku koji se mijenja ostaje u njemu.
- **Kutija teksta** piše se kao ćelija: Enter je novi red, a kutija je široka koliko njezin najdulji red.

---

## 8. Popis funkcija

| Funkcija | U datoteci | Od |
|---|---|---|
| Datum i vrijeme | običan tekst | 0.2 |
| Podebljano, kurziv, veličine M L XL | oznake | 0.5 |
| Tablica i okvir; redak, stupac, brisanje | zidovi od crta | 0.6, 0.7 |
| Kopiranje s oblikovanjem | ništa (međuspremnik) | 0.7 |
| Traži i zamijeni | ništa | 0.9 |
| Kvačica | □ ■ | 0.9 |
| Mapa (skrivanje) | ► ▼ i uvlaka | 0.9 |
| Poveznica | →tekst│cilj← | 0.9 |
| Slika | okvir ╔═╗ u paketu .pzp | 0.9 |
| Kutija teksta | okvir ╒═╕ | 0.9 |

Odbijeno: štoperica (ne pripada bilježnici), veličine S i XS (izbačene u 0.9), povlačenje zidova tablice i oznaka prijeloma (0.8). Datum i vrijeme ostaje dok vlasnik ne odluči drugačije.

---

## 9. Popis papira

- Ikona u gornjem lijevom kutu otvara **popis papira** iz mape: najnoviji gore, a za svaki ime, prvi redak teksta i kad je mijenjan. Bez sličica i bez kartica.
- Vide se i ostale datoteke u mapi; one koje nisu tekst prikazane su sivo, ali se smiju otvoriti.
- Traži se po imenu i po tekstu.
- Popis samo otvara. Brisanje, premještanje i mape unutar mape radi upravitelj datoteka.

---

## 10. Izgled

- **Jedan papir, jedna tinta.** Bijela podloga (u tamnoj temi tamna) i jedna boja tinte, plavo-crna, za kursor, odabir, poveznice i aktivne gumbe. Crveno samo za greške i sukob.
- **Font papira** je JetBrains Mono NL, ugrađen u aplikaciju: jednake širine, bez ligatura, sa svim znakovima formata (`fonts/README.md`). Sučelje koristi sistemski font.
- **Ikone** su tanke, iste debljine, nacrtane za Papir; ne koriste se znakovi iz fonta kao ikone.
- **Funkcije** su jedna vodoravna traka ikona s kratkim natpisima iznad tipkovnice.
- **Obavijesti** su sive, kratke i same nestanu; greške ostaju dok ih ne dodirneš.
- **Sve crte** (tablice, slike, kutije, razdjelnice) iste su tanke debljine.

