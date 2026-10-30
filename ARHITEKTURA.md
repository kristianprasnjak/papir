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
| **Mreža** | slike i kutije na točnom mjestu; tekst teče desno od njih; izvlačenje prije čitanja, umetanje nakon pisanja | `Objekti.kt` |
| **Mape i kvačice** | živi znakovi na početku retka, sadržaj mape po uvlaci, skriveni retci | `Mape.kt` |
| **Paket** | `.pzp`: zip s jednim papirom i slikama; isti sadržaj daje iste bajtove | `Paket.kt` |
| **Traženje** | traži i zamijeni nad tekstom koji se vidi | `Trazi.kt` |

Čitač i pisač su čista logika bez Androida: dobiju tekst, vrate tekst. Zato se mogu iscrpno testirati.

**Dva zakona pisača:**
1. Što pisač napiše, čitač pročita točno isto.
2. Pisač dira samo ono što se promijenilo. Svaki pročitani redak pamti svoj izvorni tekst; ako se nije promijenio, u datoteku ide doslovno taj izvornik.

**Mreža na ekranu:** obični redak ima visinu `RED`; naslov je jedan odlomak visok `RED` puta njegova veličina (crta postoji samo u datoteci), tablica crta zidove i crte na stupcima i retcima koje zauzimaju u datoteci. Slike se nakon svake izmjene teksta pomiču s retcima (`Objekti.pomakni`), a slobodno mjesto se provjerava i po širini koju tekst ima na ekranu.

**Mreža:** čitač prvo nađe okvire slika i kutija, izvadi ih iz redaka i spoji ostatak (tekst koji teče oko njih), pa tek onda čita oblikovanje i tablice. Pisač radi obrnuto: napiše tekst, pa okvire postavi na mrežu. Redak čiji se tekst i okviri nisu promijenili ide u datoteku doslovno.

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
| 0.8 | Tablica kao pisaći stroj: stupac je točno širok koliko najdulji red, s razmacima; bez povlačenja zidova i bez oznake prijeloma |
| 0.9 | Manifest; bez S i XS; traži i zamijeni; kvačica; mape; poveznica; slike u paketu `.pzp`; kutije teksta; papir bez prelamanja |
| 0.9.1 | Jedan redak datoteke = jedan redak ekrana (i naslovi i tablice); kvačice i mape kao element sučelja; slike se pomiču s retcima; provjera slobodnog mjesta po ekranu; XL uz sliku se odbija |
| 0.9.3 | Naslov je na ekranu jedan redak svoje veličine |
| 0.9.4 | Izbornik teksta samo uz odabir ili dugi pritisak; vlastite nedavne verzije nisu sukob |
| 0.10 | Prazan papir na dodir ikone; popis papira; novi izgled: tinta, ugrađen font, vlastite ikone, tiše obavijesti |
| kasnije | „Popravi tablicu", datum u ćeliji, oblikovanje u ćeliji, lijepljenje s oblikovanjem, traženje u tablicama |

Dogovoreno za tablice (STANDARD.md, 4): zidovi od tankih linija `┌ ┬ ┐ │ ├ ┼ ┤ └ ┴ ┘ ─`, dvostruka crta `╞ ═ ╪ ╡` ispod zaglavlja, crta između svaka dva retka tablice. Ćelija je pisaći stroj: Enter je jedini prijelom; riječi i razmaci šire stupac, a stupac je uvijek točno širok koliko najdulji red. Povlačenje zidova i oznaka prijeloma `↓` su odbačeni (vlasnik, 0.8): sve je u slovima i razmacima.
