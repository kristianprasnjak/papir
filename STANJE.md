# Stanje projekta

Zadnje ažurirano: 1. 11. 2026., verzija **0.10.2** (izdanje `v0.10.2-build43`).

Ovo je predaja za novi razgovor. Pravila su u [MANIFEST.md](MANIFEST.md), [STANDARD.md](STANDARD.md) (format), [PAPIR.md](PAPIR.md) (aplikacija) i [ARHITEKTURA.md](ARHITEKTURA.md) (kod). Ovdje je samo ono čega u njima nema: što je provjereno na mobitelu, što je otvoreno i kako se radi.

## Provjereno na mobitelu (vlasnik)

- instalacija radi; preuzimanje iz aplikacije Claude ne radi (ugrađeni preglednik), poveznicu treba zalijepiti u Chrome
- novi izgled 0.10 (tinta, font JetBrains Mono NL, ikone) prikazuje se ispravno
- tablice, slike u paketu `.pzp`, kutije, naslovi se prikazuju

## Čeka potvrdu na mobitelu

- 0.10.2: svako pokretanje otvara prazan papir (0.10 i 0.10.1 su i dalje otvarali zadnji `Note.pap`)
- 0.9.2: kvačice i mape kao element sučelja, rade na dodir
- 0.9.4: izbornik Zalijepi / Odaberi sve samo uz odabir ili dugi pritisak (pravi uzrok nije nađen, ovo je zaobilaznica)
- 0.9.4: lažni sukob nakon zapisa (vjerojatno mapa u oblaku); Papir sad pamti vlastite nedavne verzije
- popis papira (0.10): još ga nitko nije vidio na mobitelu

## Otvorena pitanja za vlasnika

- „▼kgll" bez razmaka na jednoj slici: je li vlasnik sam obrisao razmak ili Papir negdje gubi razmak

## Odgovoreno (vlasnik, 1. 11. 2026.)

- izdanja ostaju „prerelease" još nekoliko mjeseci razvoja
- mapa za papire je gdje god je korisnik postavi (mobitel ili oblak); Papir ne smije pretpostaviti ni jedno ni drugo
- tablice trebaju biti ljepše (sljedeći posao, sučelje)

## Poznata ograničenja

- slika ne može biti uža od svog imena (ime mora stati u okvir); dugačka imena slika zaslona daju široke slike, rješenje je Preimenuj
- traženje preskače tablice i sadržaj zatvorenih mapa
- lijepljenje u Papir uzima samo goli tekst
- uz naslov (L, XL) položaj slike po visini je približan

## Kako se radi

- **Gradnja samo u GitHub Actions.** Radi se na grani, PR na `main`; CI se pokreće na PR-u (ne na pushu grane). Nakon spajanja `main` gradi izdanje `vX.Y.Z-buildN` s datotekom `papirX.Y.Z.apk`. Promjene samo u `.md` datotekama ne pokreću CI.
- **Verzija:** `versionCode` i `versionName` u `app/build.gradle.kts`; uz svaku verziju odjeljak u README.md i redak u ARHITEKTURA.md.
- **Testovi lokalno** (bez Android SDK-a): čista logika (`Oblik`, `Tablica`, `PapirFile`, `Povratak`, `Funkcije`, `Html`, `Objekti`, `Mape`, `Trazi`, `Paket`) prevodi se samostalnim kotlinc 2.1.0 uz male zamjene za JUnit; sučelje (`MainActivity`, `PapirViewModel`, `Ikone`) provjerava samo CI.
- **Greške iz CI-ja** čitaju se iz anotacija provjere (korak „Greške" pretvara `e:` retke kompajlera i pale testove u `::error::`).
- Format se mijenja samo izričitom odlukom vlasnika (CLAUDE.md). Sučelje se smije mijenjati slobodno.
