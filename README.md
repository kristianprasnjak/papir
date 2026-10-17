# Papir

Prazan papir za mobitel. Otvoriš, pišeš, zatvoriš.

Ispod površine: tvoj tekst je obična datoteka koju sam imenuješ i koja se zauvijek otvara u bilo kojem programu za tekst, uključujući stari Windows Notepad. Format je otvoren i opisan u [STANDARD.md](STANDARD.md); aplikacija u [PAPIR.md](PAPIR.md).

## Verzija 0.8: tablica kao pisaći stroj

- stupac je uvijek **točno širok koliko njegov najdulji red**, uključujući razmake
- riječ ili razmak koji ne stane širi stupac; ništa se ne prelama samo od sebe
- uži stupac: obriši tekst ili razmake, ili Enterom prebaci dio u novi red
- nema povlačenja zidova ni skrivenih širina: u txt-u su samo slova i razmaci
- nova tablica počinje s najužim stupcima (u aplikaciji se crtaju malo šire, da se ćelija može dodirnuti)

## Verzija 0.7: standard

- **standard i aplikacija su odvojeni**: [STANDARD.md](STANDARD.md) opisuje format (što svaki program koji čita ili piše papir mora poštovati), [PAPIR.md](PAPIR.md) aplikaciju
- **ključ s brojem** `─── papir N ───`: papir iz budućeg formata se otvara; poznato se prikazuje, nepoznato kao tekst, a ključ se ne gubi ni ne smanjuje
- **novi papir je `Note.pap`** (`Note (1).pap`…); nastavak je preporuka, ime i dalje može biti bilo što
- datoteke nepoznate vrste, poput `.pap`, otvaraju se iz drugih aplikacija
- **kopiranje s oblikovanjem**: u e-mail ili uređivač teksta stiže pravo podebljano, kurziv i veličine; tamo gdje oblikovanja nema stiže goli tekst. Znakovi formata se nikad ne kopiraju
- **radnje s tablicom**: dok je kursor u ćeliji, **+** nudi novi redak ispod, novi stupac desno, brisanje retka, stupca ili cijele tablice; sve se može poništiti s ↶

## Verzija 0.6: tablice

- **Tablica** u popisu funkcija (**+**) ubacuje praznu tablicu sa zaglavljem
- u aplikaciji tablica ima prave linije i ćelije; dodirneš ćeliju i pišeš, Enter u ćeliji je novi red
- kad tekst ne stane, stupac se proširi u cijeloj tablici (od 0.8 stupac je uvijek točno širok koliko najdulji red)
- tablica nacrtana u Notepadu prepoznaje se ako je točno nacrtana i papir ima ključ ([STANDARD.md, 4](STANDARD.md))
- u ćeliji se zasad samo piše (oblikovanje i datum dolaze kasnije; radnje s retcima i stupcima od 0.7)

U Notepadu:

```
┌───────────────┬────────┬─────┐
│ Vježba        │ Serije │ Kg  │
╞═══════════════╪════════╪═════╡
│ Čučanj        │ 4x8    │ 80  │
├───────────────┼────────┼─────┤
│ Mrtvo dizanje │ 3x5    │ 100 │
│               │ 3x3    │ 110 │
└───────────────┴────────┴─────┘

─── papir 1 ───
```

## Verzija 0.5: format 1

Oblikovanje koje se i u Notepadu „vidi" (vidi [STANDARD.md, 3](STANDARD.md)). Znakovi se upisuju Alt kodovima, pa ih na mobitelu nitko ne može upisati slučajno; u aplikaciji se nikad ne prikazuju.

- u popisu funkcija (**+**): **B** podebljano, *I* kurziv, veličine **XS S M L XL** (M je zadana)
- B i I djeluju na odabrani tekst, ili na ono što upravo počinješ pisati; veličina je svojstvo retka i djeluje na cijele retke
- papir bez oblikovanja i dalje je goli tekst, bez ijednog posebnog znaka

Isti papir u Notepadu:

```
Palačinke
═════════

200 g brašna, ▐glatkog▌
ostaviti ‹pola sata› da odstoji
░ može i s malo mineralne vode

─── papir 1 ───
```

- pisač mijenja samo retke koje si mijenjao; ostali ostaju bajt za bajt isti
- ogledne datoteke: [`primjeri/`](primjeri/); kako aplikacija radi: [ARHITEKTURA.md](ARHITEKTURA.md)
- oznake iz 0.4 (`{*}`, `{papir 1}`) su napuštene; takva datoteka čita se kao običan tekst

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
