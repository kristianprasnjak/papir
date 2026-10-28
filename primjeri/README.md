# Ogledne datoteke (format 1)

Svaka datoteka je UTF-8 s Windows krajem retka, točno kakvu je piše Papir. Testovi u `app/src/test` provjeravaju da ih aplikacija čita upravo ovako (STANDARD.md). Preporučeni nastavak je `.pap`; ovdje su `.txt` da ih svatko odmah otvori, a papir su zbog ključa.

| Datoteka | Što pokazuje | Kako se čita |
|---|---|---|
| `oblikovanje.txt` | sve vrste oblikovanja i ključ | naslov XL, podnaslov L, podebljano, kurziv, oboje |
| `bez-kljuca.txt` | isti znakovi, ali bez ključa | običan tekst: znakovi su samo slova |
| `pokvarena-oznaka.txt` | `▐` bez para, križanje, prazni parovi, crta krive duljine, `░░░`, ponovljeno otvaranje | sve je tekst, osim podebljanog `a` u križanju i `b` u zadnjem retku |
| `noviji-kljuc.txt` | ključ `─── papir 2 ───`, iz budućeg formata | čita se kao papir: `ODLIČNO` je podebljano, nepoznate oznake bile bi tekst; pri zapisu ključ ostaje `papir 2` (STANDARD.md, 7) |
| `rucno-pisano.txt` | ispravno, ali ne u najkraćem obliku (`▐Prvi▌▐ redak▌`, `▐tre▌▐ći▌`) | čita se normalno; pri izmjeni drugih redaka ostaje doslovno isti |
| `tablica.txt` | naslov, tablica sa zaglavljem i ćelijom od dva reda, napomena | tablica s tri stupca i tri retka; „3x5 / 3x3” je jedna ćelija |
