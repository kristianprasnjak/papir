# Upute za rad na projektu Papir

Papir je aplikacija za bilješke za Android: prazan papir kao stari Windows Notepad, s pravim datotekama i otvorenim formatom. Razgovaramo na hrvatskom.

## Prije svake promjene

- Pročitaj `SPEC.md`. Ustav (odjeljak 1) je iznad svakog zahtjeva za funkcijom.
- Ne uvodi novu oznaku u format bez izričite odluke vlasnika projekta. Format se piše sporo; kod se smije pisati brzo.
- Aplikacija nikad ne mijenja tekst koji korisnik nije dirao: nema automatskog ispravljanja, prepoznavanja URL-ova, izračuna ni „pametnih" pomoći.
- Svaka promjena mora proći test od 20 sekundi: dodir ikone → tipkovnica i kursor odmah na papiru.
- Ništa se ne gubi: nikakav put kroz kod ne smije obrisati ili prepisati korisnikov tekst bez traga. Prije zapisa se provjerava promjena izvana (`diskBytes`); ono što se zamjenjuje ide u `Povijest`.
- Nikad ne dodaj dopuštenje `INTERNET` ni bilo kakvu mrežnu biblioteku.

## Tehnička pravila

- Kotlin + Jetpack Compose, `minSdk 26`.
- Čitanje/pisanje datoteka samo kroz `PapirFile` (UTF-8, čuvanje kraja retka i BOM-a, CRLF za nove datoteke).
- „Spremi kao" koristi MIME `application/octet-stream` da sustav ne bi sam dodavao nastavak.
- Ne piši u datoteku ako tekst nije promijenjen.
- Gradnja se odvija isključivo u GitHub Actions (`.github/workflows/android.yml`); lokalno nema Android SDK-a.
