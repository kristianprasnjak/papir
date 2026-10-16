# Ogledne datoteke (format 1)

Svaka datoteka je UTF-8 s Windows krajem retka, točno kakvu je piše Papir. Testovi u `app/src/test` provjeravaju da ih aplikacija čita upravo ovako (SPEC.md, 3.1).

| Datoteka | Što pokazuje | Kako se čita |
|---|---|---|
| `oblikovanje.txt` | sve vrste oblikovanja i ključ | naslov XL, podnaslov L, podebljano, kurziv, oboje, sitno, sitnije |
| `bez-kljuca.txt` | isti znakovi, ali bez ključa | običan tekst: znakovi su samo slova |
| `pokvarena-oznaka.txt` | `▐` bez para, križanje, prazni parovi, crta krive duljine, `░░░`, ponovljeno otvaranje | sve je tekst, osim podebljanog `a` u križanju i `b` u zadnjem retku |
| `nepoznat-kljuc.txt` | ključ `─── papir 2 ───` | nije ključ formata 1: cijela datoteka je običan tekst |
| `rucno-pisano.txt` | ispravno, ali ne u najkraćem obliku (`▐Prvi▌▐ redak▌`) | čita se normalno; pri izmjeni drugih redaka ostaje doslovno isti |
| `tablica.txt` | naslov, tablica sa zaglavljem i ćelijom od dva reda, sitna napomena | tablica s tri stupca i tri retka; „3x5 / 3x3” je jedna ćelija |
