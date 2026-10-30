# Papir — standard formata

**Format 1. Status: nacrt** do verzije aplikacije 1.0, kad se format 1 zamrzava. Od tada vrijedi: dodaje se, nikad ne briše.

Ovaj dokument opisuje **format**: što je u datoteci i što svaki program koji ga čita ili piše mora poštovati. Kako se to radi u aplikaciji Papir opisuje [PAPIR.md](PAPIR.md). Format se mijenja rijetko; aplikacija često.

Ključne riječi **MORA**, **NE SMIJE**, **TREBA** i **SMIJE** znače obvezu, zabranu, preporuku i dopuštenje.

---

## 1. Načela

1. **Papir je običan tekst.** Svaki papir je UTF-8 tekst koji se otvara u bilo kojem programu za tekst. Bez ključa je goli tekst i ništa se u njemu ne tumači.
2. **Savršeno u Notepadu.** Sve oznake su vidljivi znakovi koji oponašaju svoj učinak, tako da je papir smislen i uredan i u klasičnom Windows Notepadu (bez prelamanja, font jednake širine). Gdje preslikavanje nije 1:1, kompromis je izabran i zapisan.
3. **Uzorak, a ne autor.** Funkcija se prepoznaje isključivo po točnom uzorku u tekstu, bez obzira na to tko ga je upisao. Jedan znak krivo i to je tekst.
4. **Datoteka ne pamti kako je nastala, nego samo što je.** Nema zapisa o programu, autoru, uređaju, kursoru, fontu ni načinu unosa.
5. **Tekst je tekst, funkcija je funkcija.** U datoteci je uvijek nedvosmisleno što je što.
6. **Format se dodaje, nikad ne briše.** Funkcija jednom uvedena nikad ne nestaje i nikad ne mijenja značenje.

---

## 2. Datoteka

### 2.1 Kodiranje i kraj retka

- Kodiranje je **UTF-8**.
- Pisač za novu datoteku TREBA koristiti kraj retka CRLF (Windows), bez BOM-a.
- Postojeća datoteka MORA zadržati svoj kraj retka (CRLF, LF ili CR) i svoj BOM. Datoteka s miješanim krajevima redaka pri prvoj izmjeni dobiva jedinstveni kraj retka, onaj koji se u njoj pojavio prvi.
- Datoteka koja nije ispravan UTF-8 nije papir; program je NE SMIJE prepisati.

### 2.2 Ime i nastavak

- Preporučeni nastavak je **`.pap`**. Nije obavezan: papir je papir zbog ključa (2.4), a ne zbog nastavka. `recept.pap`, `recept.txt` i `index.html` s ključem jednako su papiri.
- Vrsta sadržaja je `text/plain; charset=utf-8`.
- Papir sa slikama je paket (2.3) s nastavkom **`.pzp`**.

### 2.3 Goli tekst i paket

- Papir je jedna tekstna datoteka dok god nema slika.
- **Paket** je papir sa slikama: obična zip arhiva s nastavkom `.pzp`. Zip je zip: otvara ga svaki program za arhive.
- U korijenu paketa je **točno jedan tekstni papir** (nastavak `.pap`, ili `.txt` ako `.pap` nema) i uz njega slike. Druge stavke u korijenu su prilozi; program ih MORA sačuvati kakve jesu. Podmape se ne koriste.
- Slika se u papiru navodi imenom stavke u paketu (6.2). Tekstni papir u paketu je papir kao svaki drugi.

### 2.4 Ključ

- Datoteka s ijednom funkcijom završava retkom-ključem `─── papir N ───`: tri znaka `─` (U+2500), razmak, `papir`, razmak, broj formata N, razmak, tri `─`. N je cijeli broj od 1 naviše, bez vodećih nula.
- Ispred ključa stoji jedan prazan redak; on pripada ključu, a ne tekstu. Ako ga nema, ključ i dalje vrijedi.
- Iza ključa smije stajati jedan završni prijelom retka.
- Sve drugo (`─── papir 0 ───`, `─── papir 01 ───`, `--- papir 1 ---`, ključ koji nije zadnji redak) nije ključ: datoteka je goli tekst.
- Datoteka bez ključa je goli tekst, ispravan i potpun.
- Iza ispravnog ključa svaka oznaka se provjerava zasebno: ispravne vrijede, pokvarena je tekst, ostatak papira radi.
- Broj N je broj formata, a ne verzija programa. U Notepadu ključ izgleda kao podnožje stranice.

---

## 3. Oblikovanje

Oblikovanje je dvije vrste: **unutar retka** (podebljano, kurziv) i **cijeli redak** (veličina). U datoteci izgleda tako da se i u Notepadu „vidi" što znači.

| Što | U datoteci | Znak, Alt kod | Zašto baš tako |
|---|---|---|---|
| podebljano | `▐ODLIČNO▌` | `▐` U+2590 Alt+222, `▌` U+258C Alt+221 | debeli blokovi grle riječ kao marker za isticanje |
| kurziv | `‹2+2=5›` | `‹` U+2039 Alt+0139, `›` U+203A Alt+0155 | nagnuti kutovi, nagnut tekst |
| XL (naslov) | redak, ispod njega crta od `═` | `═` U+2550 Alt+205 | dvostruka crta, najjači naglasak |
| L (podnaslov) | redak, ispod njega crta od `─` | `─` U+2500 Alt+196 | jednostruka crta |
| M | redak bez ičega | | zadano |

**Podebljano i kurziv** (unutar jednog retka):

- `▐` otvara, `▌` zatvara podebljano; `‹` otvara, `›` zatvara kurziv. Par vrijedi samo unutar jednog retka i ne smije biti prazan (`▐▌` je tekst).
- Ako se otvaranje ponovi prije zatvaranja, ranije otvaranje je tekst: u `▐a▐b▌` podebljano je samo `b`.
- Podebljano i kurziv smiju biti jedno u drugome (`▐‹kruh›▌`, `‹a ▐b▌ c›`), ali se ne smiju križati. Ako se križaju (`▐a‹b▌c›`), vrijedi podebljano, a znakovi kurziva su tekst.
- Svi ostali takvi znakovi (bez para, bez sadržaja) su tekst i prikazuju se kakvi jesu.
- Pisač piše najmanje oznaka: jedan par za svaki uzastopni dio; ako bi se kurziv i podebljano križali, pisač zatvori i ponovno otvori unutarnji par.

**Veličina je svojstvo cijelog retka:**

- **XL/L:** redak iza kojeg slijedi redak samo od `═` (XL) ili samo od `─` (L), **točno jednako dug** (broj znakova, uključujući znakove podebljanog i kurziva). Redak s naslovom ne smije biti prazan ni sam biti crta. Crta pripada naslovu; program je TREBA prikazati kao veličinu, a ne kao crtu.
- Ako crta nije točno duga kao redak iznad, ni redak ni crta nisu naslov: oboje je tekst. Tako se i naslov koji netko sam ručno podcrta u Notepadu prepoznaje, ali dugačak razdjelnik nikad.
- Veličine S i XS postojale su u nacrtu (`░ `, `░░ `) i izbačene su prije zamrzavanja formata; `░` je običan znak.
- Pisač veličinu retka uzima iz prvog znaka u retku; prazan redak je uvijek M.

**Čuvanje nediranog:** pisač mijenja samo retke koji su se promijenili. Redak čiji se tekst i oblikovanje nisu promijenili zapisuje se doslovno kako je bio u datoteci, i kad nije u najkraćem obliku (npr. rukom pisan `▐a▌▐b▌`).

**Uvijek se čuva:** svaki znak teksta, i koji je znak kako oblikovan.

**Smije se saviti:** točan izgled veličina (program bira mjere M, L i XL), font (uvijek jednake širine), razmak redaka.

Primjer u Notepadu:

```
Palačinke
═════════

Sastojci
────────
200 g brašna, ▐glatkog▌
Sve izmiješati i ostaviti ‹pola sata› da odstoji.

─── papir 1 ───
```

Ogledne datoteke: [`primjeri/`](primjeri/).

*Napomena: oznake iz verzije 0.4 (`{*}`, `{papir 1}`…) nisu dio formata. Napuštene su prije nego što je format zamrznut; datoteka s njima čita se kao običan tekst.*

## 4. Tablica i okvir

Tablica je nacrtana zidovima od tankih linija, a unutra je tekst. Okvir je tablica s jednom ćelijom. U Notepadu izgleda kao prava tablica.

```
┌───────────────┬────────┬─────┐
│ Vježba        │ Serije │ Kg  │
╞═══════════════╪════════╪═════╡
│ Čučanj        │ 4x8    │ 80  │
├───────────────┼────────┼─────┤
│ Mrtvo dizanje │ 3x5    │ 100 │
│               │ 3x3    │ 110 │
└───────────────┴────────┴─────┘
```

| Znak | Alt kod | | Znak | Alt kod |
|---|---|---|---|---|
| `┌` | Alt+218 | | `├` | Alt+195 |
| `┬` | Alt+194 | | `┼` | Alt+197 |
| `┐` | Alt+191 | | `┤` | Alt+180 |
| `└` | Alt+192 | | `╞` | Alt+198 |
| `┴` | Alt+193 | | `═` | Alt+205 |
| `┘` | Alt+217 | | `╪` | Alt+216 |
| `─` | Alt+196 | | `╡` | Alt+181 |
| `│` | Alt+179 | | | |

**Oblik** (sve u broju znakova kako ih broji Notepad, tj. kodnim točkama):

- **Vrh:** `┌`, za svaki stupac niz `─`, stupci odvojeni s `┬`, na kraju `┐`. Duljine nizova određuju stupce; svaki je dug barem 3.
- **Redak sadržaja:** `│` na točno istim mjestima kao `┌`, `┬` i `┐` na vrhu. Između zidova: razmak, tekst, dopuna razmacima, razmak.
- **Crta između redaka tablice:** `├` `─` `┼` `┤` na istim mjestima. Stoji između svaka dva retka tablice.
- **Zaglavlje:** ako je prva crta `╞` `═` `╪` `╡`, prvi redak tablice je zaglavlje. Smije biti samo prva crta.
- **Dno:** `└` `─` `┴` `┘`.
- Redak tablice su svi retci sadržaja između dvije crte; tako ćelija može imati više redova teksta.

**Prepoznavanje je strogo:** tablica postoji samo u datoteci s ključem, i samo ako je svaki njezin redak točno ovakav. Inače su svi ti retci običan tekst, vidljiv kakav jest.

**Ćelija je pisaći stroj:**
- Svaki red teksta u ćeliji je pravi red (Enter). Ništa se ne prelama samo od sebe.
- Stupac je **točno širok koliko njegov najdulji red**, uključujući razmake, i najmanje jedan znak. Riječi i razmaci šire stupac u cijeloj tablici; uži postaje samo kad se tekst ili razmaci obrišu ili se dio reda Enterom prebaci u novi red.
- Nema skrivene širine ni oznake prijeloma: sve što određuje izgled tablice su slova i razmaci.
- Razmaci na kraju reda koji ne šire stupac ne razlikuju se od dopune do zida, pa se ne čuvaju; isto vrijedi za prazne redove na kraju ćelije. Sva slova i razmaci ispred njih se čuvaju.
- **Višak širine:** ako je stupac u datoteci širi od svog najduljeg reda (bez razmaka na kraju), višak su razmaci na kraju **prvog najduljeg reda** u stupcu: prvi po retcima tablice odozgo, a unutar ćelije po redovima odozgo. Tako se ručno nacrtana tablica čita bez gubitka, a zapisana se vraća bajt za bajt ista.
- Oblikovanje unutar ćelija još nije određeno: dotad su `▐ ▌ ‹ ›` u ćeliji obični znakovi.

**Tablica je živa funkcija:** papir s tablicom ima ključ. Tablica nacrtana u papiru bez ključa ostaje običan tekst dok korisnik papiru izričito ne doda funkciju (7.).

**Uvijek se čuva:** svaki znak teksta u ćelijama, njihov raspored po recima i stupcima, širina stupaca (kao razmaci, vidi gore).

**Smije se saviti:** kako program crta zidove, razmake i zaglavlje.

## 5. Funkcije

| # | Funkcija | Vrsta | Oblik u datoteci | Gdje |
|---|---|---|---|---|
| 0 | Ključ | sistemska | zadnji redak `─── papir N ───` | 2.4 |
| 1 | Oblikovanje | živa | `▐…▌` `‹…›`, crte `═`/`─` ispod naslova | 3 |
| 2 | Tablica i okvir | živa | zidovi `┌ ┬ ┐ │ ├ ┼ ┤ └ ┴ ┘ ─`, zaglavlje `╞ ═ ╪ ╡`; samo drži, nikad ne računa | 4 |
| 3 | Kvačica | živa | `□ ` / `■ ` na početku retka | 5.1 |
| 4 | Mapa (skrivanje) | živa | `► ` / `▼ ` na početku retka; sadržaj je uvučen | 5.2 |
| 5 | Poveznica | živa | `→tekst│cilj←` | 5.3 |
| 6 | Slika | živa | okvir `╔═╗ ║ ╚═╝` s imenom slike u sredini, na točnom mjestu mreže | 6.2 |
| 7 | Kutija teksta | živa | okvir `╒═╕ │ ╘═╛` s tekstom, na točnom mjestu mreže | 6.3 |
| — | Datum i vrijeme | unos | običan tekst; format ga ne poznaje | — |

Vrste: **unos** upiše običan tekst i zaboravi ga; **živa** je oznaka koja se svaki put ponovno prepoznaje.

### 5.1 Kvačica

- Redak je kvačica ako počinje (iza uvlake od razmaka) znakom `□` (U+25A1, neoznačeno) ili `■` (U+25A0, Alt+254, označeno), iza kojeg je razmak ili kraj retka.
- Označavanje mijenja taj jedan znak u datoteci. Stanje je uvijek zapisano i vidi se i u Notepadu.

```
□ kruh
■ mlijeko
□ jaja
```

### 5.2 Mapa (skrivanje)

- Zaglavlje mape je redak koji (iza uvlake) počinje znakom `►` (U+25BA, Alt+16, zatvorena) ili `▼` (U+25BC, Alt+31, otvorena), iza kojeg je razmak ili kraj retka. Ostatak retka je ime mape.
- **Sadržaj mape** su svi retci ispod zaglavlja uvučeni više od zaglavlja, zajedno s praznim retcima između njih. Prvi neprazan redak uvučen jednako ili manje završava mapu. Prazni retci na kraju ne pripadaju mapi.
- Podmapa je zaglavlje unutar sadržaja. Uvlaka je od razmaka; predlaže se dva razmaka po razini, kao stablo u Exploreru.
- Otvaranje i zatvaranje mijenja taj jedan znak u datoteci: mapa ostaje upisana prema zadnjem stanju. Program sadržaj zatvorene mape ne prikazuje, ali ga nikad ne briše; u Notepadu je uvijek vidljiv.

```
▼ Kupovina
  ► Pekara
    kruh
  ▼ Tržnica
    rajčica
    paprika
► Posao
  sastanak u 9
```

### 5.3 Poveznica

- `→` (U+2192, Alt+26), prikazani tekst, `│` (U+2502, Alt+179), cilj, `←` (U+2190, Alt+27). Sve u jednom retku.
- Tekst i cilj ne smiju biti prazni i ne smiju sadržavati `→` ni `←`; cilj ne smije sadržavati ni `│`. Inače su znakovi običan tekst.
- Cilj je ime drugog papira u istoj mapi (npr. `Torta.pap`) ili web adresa (`https://…`). Program prikazuje samo tekst; cilj otvara tek na izričit zahtjev.
- Znakovi `▐ ▌ ‹ ›` unutar teksta poveznice su obični znakovi; podebljano i kurziv smiju obuhvatiti cijelu poveznicu.

```
Vidi →recept za tortu│Torta.pap← i →pekaru│https://pekara.hr←.
```

### 5.4 Pravila za sve oznake

- Aplikacija ne zna tko je što upisao. Funkcija se prepoznaje isključivo po točnom uzorku u tekstu: tko god ga upiše, kroz izbornik ili rukom, dobije funkciju.
- Uzorak mora biti dovoljno neobičan da se nikad ne pojavi slučajno (npr. `$%&(//ODLIČNO//)&%$`). Jedan običan znak, poput `♠`, sam za sebe nije uzorak.
- Jedan znak manje i uzorak se raspada: program prikazuje znakove kao običan tekst.
- Oznake moraju biti vidljive u Notepadu. Nevidljivi znakovi (npr. širine nula) nisu dopušteni.
- Oznaka se sastoji od znakova koji se upisuju Alt kodom (npr. Alt+222), a ne nalaze se na tipkovnicama mobitela. Čovjek ih u Notepadu može upisati namjerno, ali ih ne može upisati slučajno.
- Iz Alt kodova se ne koriste znakovi koji se pojavljuju u običnom ili zalijepljenom tekstu: `•` `·` `–` `—` `„` `“` `»` `«` `°` `±` `²` `½` `©` `€` i slični.
- Oznaka oponaša svoj učinak („onomatopeja"), tako da je i u Notepadu jasno što znači.
- Posljedica: u datoteci s ključem tekst koji je točno oznaka jest oznaka, i kad je upisan rukom. Format nema „bijeg" kojim bi se oznaka napisala kao obični tekst.

---

## 6. Slike i kutije teksta

### 6.1 Mreža

- Papir u Notepadu je mreža: retci odozgo, stupci slijeva (kodne točke, kako broji Notepad, bez prelamanja). Slika i kutija stoje na **točnom mjestu mreže**: pamte se redak gornjeg ruba, stupci lijevog i desnog ruba i visina u retcima, a sve je to sam okvir nacrtan u tekstu.
- **Tekst teče oko njih:** tekst retka puni stupce slijeva nadesno; kad naleti na okvir, nastavlja desno od njega. Ako je tekst kraći, do okvira se dopunjuje razmacima.
- Čitač zato tekst retka čita kao spoj svih stupaca izvan okvira. U retku s okvirom razmaci na kraju su dopuna i ne čuvaju se.
- Okvir ne smije sjediti preko drugog okvira. Ako bi se dva okvira preklapala, prvi (odozgo, slijeva) vrijedi, a znakovi drugoga su tekst.
- Okvir koji seže ispod kraja teksta dodaje prazne retke, da stane.
- Okviri se prepoznaju samo u papiru s ključem, i samo ako su točno nacrtani.

```
Recept za tortu    ╔═════════════╗
za osam osoba      ║             ║
                   ║  torta.jpg  ║
200 g brašna, 4 jaj║             ║a, šećer
                   ╚═════════════╝
```

Ovdje je četvrti redak teksta „200 g brašna, 4 jaja, šećer": nastavlja desno od slike.

### 6.2 Slika

- Okvir od `╔` (Alt+201), `═` (Alt+205), `╗` (Alt+187), `║` (Alt+186), `╚` (Alt+200), `╝` (Alt+188). Unutra je najmanje jedan redak.
- Unutar okvira je **točno jedan neprazan redak: ime slike** u paketu (2.3). Pisač ga stavlja u samu sredinu: u srednji redak (gornji od dva srednja), vodoravno na sredinu (lijevo jedan razmak manje ako ne ide točno). Čitač prihvaća ime bilo gdje unutar okvira.
- Okvir je širok barem koliko ime i po razmak sa svake strane; ako ime ne stane, okvir se širi udesno.
- Točan omjer slike se ne čuva: vrijede početna točka (gornji lijevi kut) i mjere okvira. Program sliku crta u okviru najbolje što može.

### 6.3 Kutija teksta

- Okvir od `╒` (Alt+213), `═`, `╕` (Alt+184), `│` (Alt+179), `╘` (Alt+212), `╛` (Alt+190).
- Unutra je tekst kao u ćeliji tablice (4): razmak uz svaki zid, Enter je novi red, ništa se ne prelama samo, a kutija je točno široka koliko njezin najdulji red (najmanje jedan znak). Višak širine pripada prvom najduljem redu.

```
Popis              ╒═════════════╕
kruh               │ ne zaboravi │
mlijeko            │ kartice!    │
jaja               ╘═════════════╛
```

---

## 7. Obveze čitača i pisača

**Čitač formata 1:**
- MORA datoteku bez ključa prikazati kao goli tekst, bez ikakvog tumačenja.
- MORA prepoznati samo točne uzorke; sve ostalo prikazuje kao tekst, znak po znak.
- MORA pročitati papir s ključem bilo kojeg broja N (8.).

**Pisač formata 1:**
- NE SMIJE mijenjati nijedno slovo teksta osim onih koje je korisnik izričito mijenjao.
- MORA retke koje korisnik nije mijenjao zapisati doslovno kakvi su bili, bajt za bajt.
- NE SMIJE dodati ključ ni oznaku bez korisnikove izričite radnje (npr. izbora oblikovanja ili tablice).
- MORA ukloniti ključ kad u papiru nema nijedne funkcije.
- NE SMIJE pisati u datoteku ako se ništa nije promijenilo.
- MORA zadržati broj formata iz ključa ako je veći od njegova (8.).
- MORA u paketu sačuvati sve priloge koje nije izričito obrisao korisnik (2.3).
- NE SMIJE upisati ništa o načinu nastanka datoteke (1., načelo 4).

---

## 8. Verzije i proširenje

- **Stariji program prikazuje ono što zna, a ostalo kao tekst.** Čitač formata 1 koji otvori papir s ključem `─── papir 2 ───` prikazuje oznake formata 1, a nepoznate oznake kao obične znakove.
- **Pravilo proširenja:** svaka nova oznaka u budućem formatu MORA starijem čitaču izgledati kao običan tekst, i nijedan stariji čitač je NE SMIJE pročitati kao nešto drugo. Zato stariji program noviji papir ne kvari: nepoznate oznake su za njega slova, a slova čuva.
- Pisač NE SMIJE smanjiti broj u ključu.
- Najstariji čitač je Notepad: ne zna ništa, pa sve prikazuje kao tekst. Svaka oznaka zato mora imati smisla i kao goli tekst.

---

## 9. Trajnost

- Uz svaku verziju formata objavljuju se ovaj dokument i ogledne datoteke ([`primjeri/`](primjeri/)).
- Standard se arhivira izvan ovog repozitorija (npr. Internet Archive, Software Heritage).
- Cilj: čitač se mora moći napisati iz samog ovog dokumenta, bez aplikacije Papir.

---

## Dodatak A: Ručni unos oznaka

Oznake se ne nalaze na tipkovnicama mobitela. Na računalu se upisuju ovako:

| Znak | Unicode | Windows (Alt + brojčanik) | macOS (Unicode Hex Input) | Linux (Ctrl+Shift+U) |
|---|---|---|---|---|
| `▐` podebljano, otvaranje | U+2590 | Alt+222 | Option+2590 | 2590 |
| `▌` podebljano, zatvaranje | U+258C | Alt+221 | Option+258C | 258c |
| `‹` kurziv, otvaranje | U+2039 | Alt+0139 | Option+2039 | 2039 |
| `›` kurziv, zatvaranje | U+203A | Alt+0155 | Option+203A | 203a |
| `═` crta XL | U+2550 | Alt+205 | Option+2550 | 2550 |
| `─` crta L, ključ, zidovi | U+2500 | Alt+196 | Option+2500 | 2500 |
| `┌ ┬ ┐` vrh tablice | U+250C U+252C U+2510 | Alt+218, 194, 191 | | |
| `│` zid | U+2502 | Alt+179 | Option+2502 | 2502 |
| `├ ┼ ┤` crta između redaka | U+251C U+253C U+2524 | Alt+195, 197, 180 | | |
| `╞ ╪ ╡` crta zaglavlja | U+255E U+256A U+2561 | Alt+198, 216, 181 | | |
| `└ ┴ ┘` dno tablice | U+2514 U+2534 U+2518 | Alt+192, 193, 217 | | |
| `□` kvačica, neoznačena | U+25A1 | nema Alt koda u Notepadu; kopirati | Option+25A1 | 25a1 |
| `■` kvačica, označena | U+25A0 | Alt+254 | Option+25A0 | 25a0 |
| `►` mapa, zatvorena | U+25BA | Alt+16 | Option+25BA | 25ba |
| `▼` mapa, otvorena | U+25BC | Alt+31 | Option+25BC | 25bc |
| `→ ←` poveznica | U+2192 U+2190 | Alt+26, Alt+27 | Option+2192, 2190 | 2192, 2190 |
| `│` cilj poveznice | U+2502 | Alt+179 | Option+2502 | 2502 |
| `╔ ╗ ╚ ╝ ║` slika | U+2554 U+2557 U+255A U+255D U+2551 | Alt+201, 187, 200, 188, 186 | | |
| `╒ ╕ ╘ ╛` kutija | U+2552 U+2555 U+2558 U+255B | Alt+213, 184, 212, 190 | | |

Na macOS-u treba u postavkama tipkovnice uključiti izvor unosa „Unicode Hex Input"; na Linuxu (GTK) Ctrl+Shift+U, zatim heksadecimalni kod i Enter.

Alt kodovi 1–31 (`►` `▼` `→` `←`) na Windowsu daju stare OEM sličice; treba ih jednom provjeriti u Notepadu na kojem se radi.
