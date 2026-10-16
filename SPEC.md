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
12. **Ništa se ne gubi, ništa se ne skriva.** Svaki papir je prava datoteka u mapi koju je čovjek izabrao; aplikacija ne čuva skrivene kopije teksta. Dok je papir otvoren, svaka izmjena se može poništiti. Aplikacija nikad ne prepisuje datoteku koja je promijenjena izvana: kad dođe do sukoba, odlučuje čovjek, a druga verzija postaje vidljiva datoteka u mapi.
13. **Bez interneta.** Aplikacija ne traži pristup mreži i ne šalje nikamo ništa. Sinkronizaciju, ako je želiš, obavlja alat koji sam izabereš, jer su papiri obične datoteke u mapi.

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

**Oblik ključa (format 1):** zadnji redak datoteke je točno `─── papir 1 ───` (tri znaka `─` U+2500, Alt+196, razmak, `papir 1`, razmak, tri `─`). U Notepadu izgleda kao podnožje stranice.

- Ispred ključa stoji jedan prazan redak; on pripada ključu, a ne tekstu. Ako ga nema, ključ i dalje vrijedi.
- Iza ključa smije stajati jedan završni prijelom retka (npr. Enter u Notepadu).
- Sve drugo (`─── papir 2 ───`, `--- papir 1 ---`, ključ koji nije zadnji redak) nije ključ za format 1: datoteka je običan tekst.
- Aplikacija dodaje ključ kad datoteka dobije prvu živu funkciju, a uklanja ga kad nestane i zadnja. Datoteka bez funkcija je uvijek goli tekst.

---

## 3. Funkcije

Za svaku funkciju specifikacija mora navesti: oblik u datoteci, **što se uvijek čuva**, **što se smije saviti** u prikazu, i ogledne datoteke (ispravne i pokvarene).

Vrste:
- **unos**: upiše običan tekst i zaboravi ga (nema veze s aplikacijom nakon unosa)
- **živa**: oznaka koju aplikacija svaki put ponovno prepoznaje

| # | Funkcija | Vrsta | Oblik u datoteci | Status |
|---|---|---|---|---|
| 0 | Ključ | sistemska | zadnji redak `─── papir 1 ───` (vidi 2.3) | format 1 |
| 1 | Skrivanje | živa | početna oznaka s razinom; cjelina traje do sljedeće oznake iste ili niže razine; podrazine | OTVORENO: znak |
| 2 | Kvačica | živa | `[ ]` / `[x]` | prijedlog |
| 3 | Datum i vrijeme | unos | običan tekst, fiksne širine: `09:05 14.10.2026.` | u aplikaciji od 0.2 (ne dira format) |
| 4 | Slika | živa | blok omeđen znakom `♠` (U+2660), razmaci čuvaju mjesto, u sredini ime slike | OTVORENO: točan uzorak |
| 5 | 2D raspored | živa | kutije na mreži znakova; otok u papiru ili cijela bilješka | OTVORENO |
| 6 | Tablica | živa | stupci poravnati razmacima; samo drži, nikad ne računa | OTVORENO |
| 7 | Poveznica | živa | oznaka + cilj (druga bilješka ili web adresa) | OTVORENO |
| 8 | Oblikovanje | živa | `▐…▌` `‹…›`, crte `═`/`─` ispod naslova, `░`/`░░` (vidi 3.1) | format 1, od 0.5 |

Pravila za sve funkcije:
- Aplikacija ne zna tko je što upisao. Funkcija se prepoznaje isključivo po točnom uzorku u tekstu: tko god ga upiše, kroz izbornik ili rukom, dobije funkciju.
- Uzorak mora biti dovoljno neobičan da se nikad ne pojavi slučajno (npr. `$%&(//ODLIČNO//)&%$`). Jedan običan znak, poput `♠`, sam za sebe nije uzorak.
- Jedan znak manje i uzorak se raspada: aplikacija prikazuje znakove kao običan tekst.
- Oznake moraju biti vidljive u Notepadu. Nevidljivi znakovi (npr. širine nula) nisu dopušteni.
- Oznaka se sastoji od znakova koji se upisuju Alt kodom (npr. Alt+222), a ne nalaze se na tipkovnicama mobitela. Čovjek ih u Notepadu može upisati namjerno, ali ih ne može upisati slučajno.
- Iz Alt kodova se ne koriste znakovi koji se pojavljuju u običnom ili zalijepljenom tekstu: `•` `·` `–` `—` `„` `“` `»` `«` `°` `±` `²` `½` `©` `€` i slični.
- Oznaka oponaša svoj učinak („onomatopeja"), tako da je i u Notepadu jasno što znači.
- Posljedica: u datoteci s ključem tekst koji je točno oznaka jest oznaka, i kad je upisan rukom. Format nema „bijeg" kojim bi se oznaka napisala kao obični tekst.

### 3.1 Oblikovanje (format 1)

Oblikovanje je dvije vrste: **unutar retka** (podebljano, kurziv) i **cijeli redak** (veličina). U datoteci izgleda tako da se i u Notepadu „vidi" što znači.

| Što | U datoteci | Znak, Alt kod | Zašto baš tako |
|---|---|---|---|
| podebljano | `▐ODLIČNO▌` | `▐` U+2590 Alt+222, `▌` U+258C Alt+221 | debeli blokovi grle riječ kao marker za isticanje |
| kurziv | `‹2+2=5›` | `‹` U+2039 Alt+0139, `›` U+203A Alt+0155 | nagnuti kutovi, nagnut tekst |
| XL (naslov) | redak, ispod njega crta od `═` | `═` U+2550 Alt+205 | dvostruka crta, najjači naglasak |
| L (podnaslov) | redak, ispod njega crta od `─` | `─` U+2500 Alt+196 | jednostruka crta |
| M | redak bez ičega | | zadano |
| S | `░ ` na početku retka | `░` U+2591 Alt+176 | najsvjetlija sjena: tiho |
| XS | `░░ ` na početku retka | | dvaput tiho |

**Podebljano i kurziv** (unutar jednog retka):

- `▐` otvara, `▌` zatvara podebljano; `‹` otvara, `›` zatvara kurziv. Par vrijedi samo unutar jednog retka i ne smije biti prazan (`▐▌` je tekst).
- Ako se otvaranje ponovi prije zatvaranja, ranije otvaranje je tekst: u `▐a▐b▌` podebljano je samo `b`.
- Podebljano i kurziv smiju biti jedno u drugome (`▐‹kruh›▌`, `‹a ▐b▌ c›`), ali se ne smiju križati. Ako se križaju (`▐a‹b▌c›`), vrijedi podebljano, a znakovi kurziva su tekst.
- Svi ostali takvi znakovi (bez para, bez sadržaja) su tekst i prikazuju se kakvi jesu.
- Pisač piše najmanje oznaka: jedan par za svaki uzastopni dio; ako bi se kurziv i podebljano križali, pisač zatvori i ponovno otvori unutarnji par.

**Veličina je svojstvo cijelog retka:**

- **XL/L:** redak iza kojeg slijedi redak samo od `═` (XL) ili samo od `─` (L), **točno jednako dug** (broj znakova, uključujući znakove podebljanog i kurziva). Redak s naslovom ne smije biti prazan ni sam biti crta. Crta pripada naslovu i u aplikaciji se ne vidi.
- Ako crta nije točno duga kao redak iznad, ni redak ni crta nisu naslov: oboje je tekst. Tako se i naslov koji netko sam ručno podcrta u Notepadu prepoznaje, ali dugačak razdjelnik nikad.
- **S:** redak počinje s `░` i razmakom. **XS:** redak počinje s `░░` i razmakom. Razmak pripada oznaci. `░░░ ` nije oznaka.
- Ako je redak i naslov i počinje s `░ `, vrijedi naslov.
- Pisač veličinu retka uzima iz prvog znaka u retku; prazan redak je uvijek M.

**Čuvanje nediranog:** pisač mijenja samo retke koji su se promijenili. Redak čiji se tekst i oblikovanje nisu promijenili zapisuje se doslovno kako je bio u datoteci, i kad nije u najkraćem obliku (npr. rukom pisan `▐a▌▐b▌`).

**Uvijek se čuva:** svaki znak teksta, i koji je znak kako oblikovan.

**Smije se saviti:** točan izgled veličina (aplikacija bira mjere XS–XL), font (uvijek jednake širine), razmak redaka.

Primjer u Notepadu:

```
Palačinke
═════════

Sastojci
────────
200 g brašna, ▐glatkog▌
Sve izmiješati i ostaviti ‹pola sata› da odstoji.
░ može i s malo mineralne vode

─── papir 1 ───
```

Ogledne datoteke: [`primjeri/`](primjeri/).

*Napomena: oznake iz verzije 0.4 (`{*}`, `{papir 1}`…) nisu dio formata. Napuštene su prije nego što je format zamrznut; datoteka s njima čita se kao običan tekst.*

---

## 4. Trajnost

- Uz svaku verziju formata objavljuje se popis funkcija i skup oglednih datoteka.
- Specifikacija se arhivira izvan ovog repozitorija (npr. Internet Archive, Software Heritage).
- Cilj: čitač za ovaj format mora se moći napisati iz same specifikacije, bez ove aplikacije.
