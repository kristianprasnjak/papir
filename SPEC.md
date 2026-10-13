# Papir — specifikacija formata

**Status: nacrt.** Ništa u ovom dokumentu još nije zamrznuto. Pravilo „dodaje se, nikad ne briše" počinje vrijediti od prve objavljene verzije formata (1). Dijelovi označeni **OTVORENO** čekaju odluku.

Aplikacija se smije pisati brzo. Ova specifikacija se piše sporo: svaka njena rečenica jednom vrijedi zauvijek.

---

## 1. Ustav

Ova pravila su iznad svake funkcije. Funkcija koja krši bilo koje od njih ne ulazi u format.

1. **Čovjek je gospodar svog teksta, aplikacija je sluga.**
2. **Aplikacija nikad ne tumači ono što pišeš i ne mijenja datoteku bez tvoje namjere.** Ništa se ne ispravlja, ne izračunava, ne prepoznaje i ne nudi dok tipkaš. Ako Ivica kaže `2+2=5`, papir to pamti točno tako.
3. **Prikazivati smije, prepravljati ne.** Aplikacija čita datoteku kao preglednik HTML: prikazuje i oživljava, a datoteku dira samo na izričit zahtjev.
4. **Funkcije nastaju samo iz izbornika.** Jedan dodir otvara izbornik, drugi umeće funkciju. Nema okidača u tekstu.
5. **Tekst je tekst, funkcija je funkcija.** Niz `www.nesto.hr` je tekst. Poveznica je funkcija s vlastitom oznakom. U datoteci je uvijek nedvosmisleno što je što.
6. **Datoteka je istina, prikaz je tumačenje.** Sve što aplikacija prikazuje mora proizlaziti iz datoteke, nikad iz nečeg skrivenog sa strane.
7. **Savršeno u Notepadu.** Svaka datoteka mora biti smislena i uredna u klasičnom Windows Notepadu (bez prelamanja, font fiksne širine). Gdje preslikavanje nije 1:1, kompromis je unaprijed izabran i zapisan.
8. **Ime je samo slova.** Što korisnik upiše kao ime datoteke, uključujući bilo koji nastavak, to i dobije, slovo za slovo.
9. **Format se dodaje, nikad ne briše.** Funkcija jednom uvedena nikad ne nestaje i nikad ne mijenja značenje.
10. **Sučelje je slobodno.** Izgled aplikacije smije se mijenjati, eksperimentirati i griješiti. Format ne.
11. **Test od 20 sekundi.** Otvoriš, napišeš popis namirnica, zatvoriš. Nijedna funkcija ne smije usporiti taj put.

---

## 2. Datoteka

### 2.1 Kodiranje i kraj retka

- Kodiranje je **UTF-8**.
- Nova datoteka koju stvori aplikacija koristi kraj retka **CRLF** (Windows), bez BOM-a.
- Postojeća datoteka zadržava svoj kraj retka (CRLF, LF ili CR) i svoj BOM. Aplikacija ne mijenja ono što nije dirano: datoteka se ne prepisuje dok se tekst ne promijeni.
- Kompromis: datoteka s miješanim krajevima redaka pri prvoj izmjeni dobiva jedinstveni kraj retka, onaj koji se u njoj pojavio prvi.
- Datoteka koja nije ispravan UTF-8 otvara se samo za čitanje.

### 2.2 Goli tekst i paket

- Papir je goli `.txt` (ili bilo koji nastavak koji je korisnik izabrao) dok god nema priloga.
- S prvim prilogom (npr. slikom) postaje `.zip` koji sadrži tekstualnu datoteku i priloge. **OTVORENO:** raspored unutar zipa i ime tekstualne datoteke u njemu.
- Kad se obriše zadnji prilog, vraća se u goli tekst.

### 2.3 Ključ (podnožje)

- Datoteka s funkcijama završava retkom-ključem koji nosi **verziju formata** (ne verziju aplikacije).
- Datoteka bez ključa je običan papir, ispravan i potpun.
- Ako ključ postoji, ali nije ispravan, datoteka se otvara kao običan tekst. Bez pogađanja.
- Iza ispravnog ključa svaka funkcija se provjerava zasebno: ispravne oživljavaju, pokvarena se prikazuje kao tekst, ostatak dokumenta radi.
- **OTVORENO:** točan oblik ključa.

---

## 3. Funkcije

Za svaku funkciju specifikacija mora navesti: oblik u datoteci, **što se uvijek čuva**, **što se smije saviti** u prikazu, i ogledne datoteke (ispravne i pokvarene).

Vrste:
- **unos**: upiše običan tekst i zaboravi ga (nema veze s aplikacijom nakon unosa)
- **živa**: oznaka koju aplikacija svaki put ponovno prepoznaje

| # | Funkcija | Vrsta | Oblik u datoteci | Status |
|---|---|---|---|---|
| 0 | Ključ | sistemska | redak na kraju s verzijom formata | OTVORENO |
| 1 | Skrivanje | živa | početna oznaka s razinom; cjelina traje do sljedeće oznake iste ili niže razine; podrazine | OTVORENO: znak |
| 2 | Kvačica | živa | `[ ]` / `[x]` | prijedlog |
| 3 | Datum i vrijeme | unos | običan tekst | prijedlog |
| 4 | Slika | živa | blok omeđen znakom `♠` (U+2660), razmaci čuvaju mjesto, u sredini ime slike | OTVORENO: točan uzorak |
| 5 | 2D raspored | živa | kutije na mreži znakova; otok u papiru ili cijela bilješka | OTVORENO |
| 6 | Tablica | živa | stupci poravnati razmacima; samo drži, nikad ne računa | OTVORENO |
| 7 | Poveznica | živa | oznaka + cilj (druga bilješka ili web adresa) | OTVORENO |

Pravila za sve funkcije:
- Oznaku upisuje samo aplikacija, preko izbornika. Isti znak napisan rukom u tekstu ostaje običan znak.
- Oznake moraju biti vidljive u Notepadu. Nevidljivi znakovi (npr. širine nula) nisu dopušteni.
- Oznaka bi trebala biti nešto što bi čovjek mogao i ručno napisati.

---

## 4. Trajnost

- Uz svaku verziju formata objavljuje se popis funkcija i skup oglednih datoteka.
- Specifikacija se arhivira izvan ovog repozitorija (npr. Internet Archive, Software Heritage).
- Cilj: čitač za ovaj format mora se moći napisati iz same specifikacije, bez ove aplikacije.
