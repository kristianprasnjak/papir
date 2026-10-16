# Papir — aplikacija

Ovaj dokument opisuje **aplikaciju** Papir za Android: čemu služi, što nikad ne radi i kako se njome upravlja. Format datoteka opisan je zasebno, u [STANDARD.md](STANDARD.md); aplikacija ga poštuje kao i svaki drugi program. Kako je aplikacija izgrađena opisuje [ARHITEKTURA.md](ARHITEKTURA.md).

Aplikacija se smije mijenjati brzo. Ustav ne.

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

- **Svaka kontrola mijenja ili datoteku ili samo prikaz, i to se mora znati.** B, I, veličina, tablica i datum mijenjaju datoteku. Pomicanje, povećavanje i sve što samo gleda ne mijenja ništa.
- **Nema skrivenih načina rada.** Što je na ekranu, to je stanje; nema „uključenog" načina koji mijenja što tipke rade bez da se vidi.
- **Ništa slučajno.** Nijedna gesta, dvostruki dodir ni duži pritisak ne umeće funkciju. Funkcija je uvijek dva namjerna dodira: **+**, pa funkcija.
- **Sve se može poništiti.** Svaka izmjena, uključujući funkcije i radnje s tablicama, jedan je korak za ↶.
- **Pod palcem.** Kontrole su u traci iznad tipkovnice, gdje palac već jest.
- **Datoteka ne pamti kako je nastala.** Aplikacija ne upisuje u datoteku ništa o sebi, kursoru, fontu ni načinu unosa (STANDARD.md, 1., načelo 4).

---

## 3. Papiri i mapa

- **Mapa za papire** bira se jednom, pri prvom pokretanju. Svi papiri su obične datoteke u njoj.
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
