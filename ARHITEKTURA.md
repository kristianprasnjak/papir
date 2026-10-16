# Kako Papir radi

## Jedno pravilo iznad svih

**Jedina istina je txt.** Aplikacija nema bazu, kopije ni skriveno pamćenje sadržaja. Pamti samo postavke prikaza (font, mapa za papire). Sve što je na ekranu u svakom je trenutku pročitano iz txt-a.

## Tri dijela

| Dio | Što radi | Gdje |
|---|---|---|
| **Čitač** | txt → papir: retci (XL/L naslovi, M, S, XS), unutar njih podebljano i kurziv; sve što ne štima točno ostaje tekst | `Oblik.citaj` |
| **Pisač** | papir → txt: najmanje oznaka; retke koje nisi dirao zapisuje doslovno kakvi su bili | `Oblik.zapisi` |
| **Prikaz** | papir → ekran: crta značenje, nikad znakove oznaka; slobodan izbor fonta (jednake širine), mjera i boja | `MainActivity` |
| **Tablice** | prepoznavanje, čitanje i crtanje tablica (STANDARD.md, 4); radnje s retcima i stupcima; u čistom tekstu tablica stoji doslovno | `Tablica.kt` |
| **Kopiranje** | odabir → goli tekst + HTML s pravim oblikovanjem (PAPIR.md, 4) | `Html.kt`, `PapirClipboard` |

Čitač i pisač su čista logika bez Androida: dobiju tekst, vrate tekst. Zato se mogu iscrpno testirati.

**Dva zakona pisača:**
1. Što pisač napiše, čitač pročita točno isto.
2. Pisač dira samo ono što se promijenilo. Svaki pročitani redak pamti svoj izvorni tekst; ako se nije promijenio, u datoteku ide doslovno taj izvornik.

**Ključ s brojem:** čitač čita `─── papir N ───` za svaki N i pamti broj (`Procitano.format`). Pisač zapisuje veći od tog broja i svog (1), a papiru novijeg formata nikad ne skida ključ, jer u njemu mogu biti oznake koje ne poznaje (STANDARD.md, 7).

## Što se događa kod svakog slova

1. Korisnik nešto napravi: tipka, briše, dodirne B ili veličinu.
2. Aplikacija to primijeni na čisti tekst i oblikovanje (`PapirViewModel`).
3. Pisač iz toga napiše txt.
4. Čitač iz tog txt-a pročita papir, i **to** se prikazuje (`setModel`).

**Odsječci:** papir se prikazuje kao niz odsječaka, tekst i tablice (`Odsjecci`). Izmjena u tekstnom odsječku prevodi se na cijeli čisti tekst; izmjena u ćeliji ili radnja s tablicom (novi redak, stupac, brisanje) iznova nacrta tablicu (`Tablice.zapisi`) i zamijeni njezine retke. Papir bez tablica je jedan tekstni odsječak.

Korak 4 je osigurač: ekran se nikad ne crta iz onoga što aplikacija „misli", nego iz onoga što je stvarno u txt-u. Ako je korisnik upisao nešto što je točno oznaka, ona odmah postaje oznaka, jer bi to bila i pri sljedećem otvaranju.

Spremanje (s provjerom promjene izvana), sukobi i poništavanje rade nad txt-om, pa automatski obuhvaćaju i oblikovanje.

## Kako se drže pravila pod kontrolom

- Svako pravilo formata je prvo zapisano u [STANDARD.md](STANDARD.md), a svako pravilo aplikacije u [PAPIR.md](PAPIR.md), tek onda u kodu.
- Ogledne datoteke u [`primjeri/`](primjeri/): za svako pravilo txt koji se mora pročitati točno određeno. `OblikTest` ih provjerava.
- **Testovi nasumičnim izmjenama** (`OblikNasumicnoTest`): tisuće nasumičnih izmjena, a nakon svake provjera da se txt čita onako kako se prikazuje, da nijedno slovo nije nestalo i da su nedirani retci bajt za bajt isti.

## Redoslijed

| Verzija | Što |
|---|---|
| 0.5 | Čitač i pisač po retcima, znakovi formata 1, testovi nasumičnim izmjenama |
| 0.6 | Prepoznavanje i prikaz tablica; tipkanje u ćeliju, Enter u ćeliji, širenje stupca; nova tablica iz izbornika |
| 0.7 | Standard odvojen od aplikacije; ključ s brojem; `Note.pap`; kopiranje u dvije verzije; dodavanje i brisanje redaka i stupaca, brisanje tablice |
| 0.8 | Povlačenje zida, prijelomi s `↓`, „Popravi tablicu", datum u ćeliji |

Dogovoreno za tablice (format je u STANDARD.md, 4; ostatak dolazi): zidovi od tankih linija `┌ ┬ ┐ │ ├ ┼ ┤ └ ┴ ┘ ─`, dvostruka crta `╞ ═ ╪ ╡` ispod zaglavlja, crta između svaka dva retka tablice; Enter u ćeliji je pravi novi red; tipkanje nikad ne prelama nego širi stupac; prelama samo sužavanje stupca, a takav prijelom ima `↓` (Alt+25) uz desni zid ćelije; stupac sa strelicom zadržava širinu koju je postavio korisnik.
