# Papir

Prazan papir za mobitel. Otvoriš, pišeš, zatvoriš.

Ispod površine: tvoj tekst je obična datoteka koju sam imenuješ i koja se zauvijek otvara u bilo kojem programu za tekst, uključujući stari Windows Notepad. Format je otvoren i opisan u [SPEC.md](SPEC.md).

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
