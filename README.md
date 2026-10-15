# Papir

Prazan papir za mobitel. Otvoriš, pišeš, zatvoriš.

Ispod površine: tvoj tekst je obična datoteka koju sam imenuješ i koja se zauvijek otvara u bilo kojem programu za tekst, uključujući stari Windows Notepad. Format je otvoren i opisan u [SPEC.md](SPEC.md).

## Verzija 0.3.1: povjerenje

- **mapa za papire** bira se jednom, pri prvom pokretanju; nacrt iz ranijih verzija postaje datoteka u njoj
- **prave datoteke od prvog slova**: novi papir je `Note.txt` čim dobije prvo slovo; ako ime postoji, Android sam dodaje broj (`Note (1).txt`…)
- **Preimenuj…**: novo ime je točno ono što upišeš, uključujući nastavak; ako ga sustav ipak promijeni, aplikacija to kaže
- **poništi ↶ i ponovi ↷** u traci iznad tipkovnice, dok je papir otvoren; tipkanje se grupira, novi redak i funkcije su zasebni koraci
- **nikad tiho prepisivanje**: prije svakog zapisa provjerava se je li datoteka promijenjena izvana; ako jest, ništa se ne prepisuje dok ne odabereš koju verziju zadržati, a druga postaje vidljiva datoteka u mapi (`Note (sukob).txt`)
- pri povratku u aplikaciju, ako je datoteka promijenjena izvana, a ovdje nema ničeg nespremljenog, prikazuje se njeno novo stanje
- **bez skrivenih kopija**: aplikacija ne čuva povijest ni kopije tvog teksta; sve što postoji je u tvojoj mapi
- aplikacija nema dopuštenje za internet

(0.3.0 je zamijenjena ovom verzijom: imala je skrivenu povijest verzija.)

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
