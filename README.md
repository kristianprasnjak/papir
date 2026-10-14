# Papir

Prazan papir za mobitel. Otvoriš, pišeš, zatvoriš.

Ispod površine: tvoj tekst je obična datoteka koju sam imenuješ i koja se zauvijek otvara u bilo kojem programu za tekst, uključujući stari Windows Notepad. Format je otvoren i opisan u [SPEC.md](SPEC.md).

## Verzija 0.3: povjerenje

- **prave datoteke od prvog slova**: jednom odabereš mapu za papire, a svaki novi papir odmah postaje datoteka u njoj (ime: datum i vrijeme nastanka, npr. `2026-10-14 21.04.txt`)
- **Preimenuj…**: novo ime je točno ono što upišeš, uključujući nastavak; ako ga sustav ipak promijeni, aplikacija to kaže
- **poništi ↶ i ponovi ↷** u traci iznad tipkovnice; tipkanje se grupira, novi redak i funkcije su zasebni koraci
- **nikad tiho prepisivanje**: prije svakog zapisa provjerava se je li datoteka promijenjena izvana (sinkronizacija, drugi program); ako jest, ništa se ne prepisuje dok ne odabereš koju verziju zadržati
- pri povratku u aplikaciju, ako je datoteka promijenjena izvana, a ovdje nema ničeg nespremljenog, prikazuje se njeno novo stanje
- **Prethodne verzije…**: zadnjih 30 verzija svakog papira (izvorna prije izmjena, verzija koja je izgubila u sukobu, napušteni nacrt…); dodir je vraća
- nacrt se više nikad ne briše bez traga
- aplikacija nema dopuštenje za internet

## Verzija 0.2

- gumb **+** u traci iznad tipkovnice otvara popis funkcija; dodir na funkciju je umeće
- prva funkcija: **Datum i vrijeme**, upisuje npr. `09:05 14.10.2026.` na mjesto kursora (ili umjesto odabranog teksta), kao F5 u Notepadu
- nacrt se otvara s kursorom na kraju teksta, datoteka s kursorom na početku

## Verzija 0.1

Samo papir, bez funkcija:

- otvori aplikaciju i odmah pišeš
- sve se sprema samo, nema gumba za spremanje
- **Spremi kao…** s bilo kojim imenom i nastavkom, slovo za slovo
- **Otvori…** bilo koju tekstualnu datoteku
- nove datoteke su UTF-8 s Windows krajem retka; postojeće zadržavaju svoj

## Instalacija na mobitel

Svaki push na `main` gradi APK u GitHub Actions i objavljuje ga pod [Releases](../../releases). Na Androidu otvori najnovije izdanje, preuzmi `papir.apk` i instaliraj (treba dopustiti instalaciju iz preglednika).

Napomena: izdanja 0.x potpisana su javnim razvojnim ključem (`app/debug.keystore`), samo radi lakšeg ažuriranja. Pravi ključ dolazi prije objave na Google Playu.
