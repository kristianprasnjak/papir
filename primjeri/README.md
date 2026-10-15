# Ogledne datoteke

Svaka datoteka je UTF-8 s Windows krajem retka, točno kakvu je piše Papir. Testovi u `app/src/test` provjeravaju da ih aplikacija čita upravo ovako.

| Datoteka | Što pokazuje | Kako se čita |
|---|---|---|
| `oblikovanje.txt` | sve oznake oblikovanja i ključ | naslov XL, podebljano, kurziv, kombinacija, sitno |
| `bez-kljuca.txt` | iste oznake, ali bez ključa | običan tekst: oznake su samo slova |
| `pokvarena-oznaka.txt` | `{*` bez zatvaranja, `{/*}` krivim redom, `{-}-5°C` | prve dvije su tekst; zadnja je ispravna: „-5°C" sitno |
| `nepoznat-kljuc.txt` | ključ `{papir 2}` | nije ključ formata 1: cijela datoteka je običan tekst |
